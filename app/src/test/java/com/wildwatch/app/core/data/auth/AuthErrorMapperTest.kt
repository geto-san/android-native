package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.FirebaseAuthException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMapperTest {

    // FirebaseAuthException's real constructor reaches into android.text.TextUtils, which
    // isn't mocked in a plain JVM unit test (no Robolectric here) - mockk sidesteps that by
    // never actually running the constructor.
    private fun firebaseAuthException(errorCode: String): FirebaseAuthException =
        mockk<FirebaseAuthException> { every { this@mockk.errorCode } returns errorCode }

    @Test
    fun `isNetworkError is true for a network-failure FirebaseAuthException`() {
        val error = firebaseAuthException("ERROR_NETWORK_REQUEST_FAILED")

        assertTrue(AuthErrorMapper.isNetworkError(error))
    }

    @Test
    fun `isNetworkError is false for a different FirebaseAuthException error code`() {
        val error = firebaseAuthException("ERROR_INVALID_EMAIL")

        assertFalse(AuthErrorMapper.isNetworkError(error))
    }

    @Test
    fun `isNetworkError is false for a non-Firebase exception`() {
        assertFalse(AuthErrorMapper.isNetworkError(IllegalStateException("boom")))
    }

    @Test
    fun `isNetworkError is false for null`() {
        assertFalse(AuthErrorMapper.isNetworkError(null))
    }
}
