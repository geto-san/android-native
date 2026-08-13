package com.wildwatch.app.feature.auth

import app.cash.turbine.test
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var observeUserUseCase: ObserveUserUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        userDataRepository = mockk(relaxUnitFun = true)
        observeUserUseCase = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { observeUserUseCase() } returns MutableStateFlow(null)
        every { userDataRepository.pendingEmailLinkAddress } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AuthViewModel(authRepository, userDataRepository, observeUserUseCase)

    @Test
    fun `sendEmailSignInLink with a blank email sets an error without calling the repository`() {
        val vm = viewModel()

        vm.sendEmailSignInLink("")

        assertEquals("Enter your email address", vm.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.sendSignInLinkToEmail(any()) }
    }

    @Test
    fun `sendEmailSignInLink success saves the address and shows the check-your-email view`() = runTest(testDispatcher) {
        coEvery { authRepository.sendSignInLinkToEmail("a@b.com") } returns Result.success(Unit)
        val vm = viewModel()

        vm.sendEmailSignInLink("a@b.com")
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.emailLinkSent)
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        coVerify { userDataRepository.setPendingEmailLinkAddress("a@b.com") }
    }

    @Test
    fun `sendEmailSignInLink failure surfaces an error and does not save the address`() = runTest(testDispatcher) {
        coEvery { authRepository.sendSignInLinkToEmail(any()) } returns Result.failure(Exception("network down"))
        val vm = viewModel()

        vm.sendEmailSignInLink("a@b.com")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.emailLinkSent)
        assertEquals("network down", vm.uiState.value.errorMessage)
        coVerify(exactly = 0) { userDataRepository.setPendingEmailLinkAddress(any()) }
    }

    @Test
    fun `completeEmailLinkSignIn with a saved address signs in directly`() = runTest(testDispatcher) {
        every { authRepository.isSignInWithEmailLink(any()) } returns true
        every { userDataRepository.pendingEmailLinkAddress } returns flowOf("a@b.com")
        coEvery { authRepository.signInWithEmailLink("a@b.com", "https://wildwatch-82abc.web.app/?oobCode=x") } returns Result.success(Unit)
        val vm = viewModel()

        vm.completeEmailLinkSignIn("https://wildwatch-82abc.web.app/?oobCode=x")
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.pendingConfirmationLink)
        coVerify { userDataRepository.setPendingEmailLinkAddress(null) }
    }

    @Test
    fun `completeEmailLinkSignIn with no saved address asks the user to confirm`() = runTest(testDispatcher) {
        every { authRepository.isSignInWithEmailLink(any()) } returns true
        every { userDataRepository.pendingEmailLinkAddress } returns flowOf(null)
        val vm = viewModel()

        vm.completeEmailLinkSignIn("https://wildwatch-82abc.web.app/?oobCode=x")
        advanceUntilIdle()

        assertEquals("https://wildwatch-82abc.web.app/?oobCode=x", vm.uiState.value.pendingConfirmationLink)
        coVerify(exactly = 0) { authRepository.signInWithEmailLink(any(), any()) }
    }

    @Test
    fun `completeEmailLinkSignIn ignores a link that isn't a real sign-in link`() = runTest(testDispatcher) {
        every { authRepository.isSignInWithEmailLink(any()) } returns false
        val vm = viewModel()

        vm.completeEmailLinkSignIn("https://wildwatch-82abc.web.app/some-other-page")
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingConfirmationLink)
        coVerify(exactly = 0) { authRepository.signInWithEmailLink(any(), any()) }
    }

    @Test
    fun `confirmEmailForPendingLink completes sign-in with the typed-in address`() = runTest(testDispatcher) {
        every { authRepository.isSignInWithEmailLink(any()) } returns true
        every { userDataRepository.pendingEmailLinkAddress } returns flowOf(null)
        coEvery { authRepository.signInWithEmailLink("typed@b.com", any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.completeEmailLinkSignIn("https://wildwatch-82abc.web.app/?oobCode=x")
        advanceUntilIdle()

        vm.confirmEmailForPendingLink("typed@b.com")
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingConfirmationLink)
        coVerify { authRepository.signInWithEmailLink("typed@b.com", "https://wildwatch-82abc.web.app/?oobCode=x") }
    }

    @Test
    fun `only the tapped button's loadingAction is set while a sign-in is in flight`() = runTest(testDispatcher) {
        // A mock that resolves immediately would race StateFlow's own conflation (only the
        // latest value survives) against UnconfinedTestDispatcher's eager execution, making
        // the transient ANONYMOUS state unobservable by accident of timing rather than by
        // anything wrong with the ViewModel. Pausing on a real suspension point sidesteps that.
        val deferred = CompletableDeferred<Result<Unit>>()
        coEvery { authRepository.signInAnonymously() } coAnswers { deferred.await() }
        val vm = viewModel()

        vm.signInAnonymously()
        assertEquals(AuthLoadingAction.ANONYMOUS, vm.uiState.value.loadingAction)

        deferred.complete(Result.success(Unit))
        advanceUntilIdle()
        assertNull(vm.uiState.value.loadingAction)
    }

    @Test
    fun `signOut delegates to the repository`() {
        every { authRepository.signOut() } returns Unit
        val vm = viewModel()

        vm.signOut()

        verify { authRepository.signOut() }
    }

    @Test
    fun `resetEmailFlow clears both the sent and confirmation states`() = runTest(testDispatcher) {
        coEvery { authRepository.sendSignInLinkToEmail(any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.sendEmailSignInLink("a@b.com")
        advanceUntilIdle()
        assertEquals(true, vm.uiState.value.emailLinkSent)

        vm.resetEmailFlow()

        assertFalse(vm.uiState.value.emailLinkSent)
        assertNull(vm.uiState.value.pendingConfirmationLink)
    }
}
