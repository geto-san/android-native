package com.silverback.sentry.ui.auth

import app.cash.turbine.test
import com.silverback.sentry.data.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with blank email sets an error without calling the repository`() {
        val viewModel = AuthViewModel(authRepository)

        viewModel.signIn(email = "", password = "password")

        assertEquals("Enter your email and password", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `signIn success clears loading and error`() = runTest(testDispatcher) {
        coEvery { authRepository.signIn("a@b.com", "pw") } returns Result.success(Unit)
        val viewModel = AuthViewModel(authRepository)

        viewModel.signIn("a@b.com", "pw")
        advanceUntilIdle()

        assertEquals(AuthUiState(isLoading = false, errorMessage = null), viewModel.uiState.value)
        coVerify { authRepository.signIn("a@b.com", "pw") }
    }

    @Test
    fun `signIn failure surfaces the exception message and clears loading`() = runTest(testDispatcher) {
        coEvery { authRepository.signIn(any(), any()) } returns Result.failure(Exception("bad credentials"))
        val viewModel = AuthViewModel(authRepository)

        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.signIn("a@b.com", "pw")
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertEquals("bad credentials", result.errorMessage)
        }
    }

    @Test
    fun `signUp with a blank field sets an error without calling the repository`() {
        val viewModel = AuthViewModel(authRepository)

        viewModel.signUp(displayName = "", email = "a@b.com", password = "pw")

        assertEquals("Fill in all fields", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp success clears loading and error`() = runTest(testDispatcher) {
        coEvery { authRepository.signUp("a@b.com", "pw", "Jane") } returns Result.success(Unit)
        val viewModel = AuthViewModel(authRepository)

        viewModel.signUp(displayName = "Jane", email = "a@b.com", password = "pw")
        advanceUntilIdle()

        assertEquals(AuthUiState(isLoading = false, errorMessage = null), viewModel.uiState.value)
        coVerify { authRepository.signUp("a@b.com", "pw", "Jane") }
    }

    @Test
    fun `signOut delegates to the repository`() {
        every { authRepository.signOut() } returns Unit
        val viewModel = AuthViewModel(authRepository)

        viewModel.signOut()

        verify { authRepository.signOut() }
    }

    @Test
    fun `clearError resets the error message`() = runTest(testDispatcher) {
        coEvery { authRepository.signIn(any(), any()) } returns Result.failure(Exception("oops"))
        val viewModel = AuthViewModel(authRepository)

        viewModel.signIn("a@b.com", "pw")
        assertEquals("oops", viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
