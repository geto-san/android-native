package com.wildwatch.app.core.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    @Test
    fun `violatesRangerSignInPolicy is false for a Google-signed-in gmail address`() {
        assertFalse(violatesRangerSignInPolicy(listOf("google.com"), "ranger.joe@gmail.com"))
    }

    @Test
    fun `violatesRangerSignInPolicy is true when Google is not among the linked providers`() {
        assertTrue(violatesRangerSignInPolicy(listOf("password"), "ranger.joe@gmail.com"))
    }

    @Test
    fun `violatesRangerSignInPolicy is true for a non-gmail address even via Google`() {
        assertTrue(violatesRangerSignInPolicy(listOf("google.com"), "ranger.joe@wildwatch.app"))
        assertTrue(violatesRangerSignInPolicy(listOf("google.com"), "ranger.joe@workspace-domain.com"))
    }

    @Test
    fun `violatesRangerSignInPolicy is true for a null email`() {
        assertTrue(violatesRangerSignInPolicy(listOf("google.com"), null))
    }

    @Test
    fun `violatesRangerSignInPolicy is case-insensitive on the gmail domain`() {
        assertFalse(violatesRangerSignInPolicy(listOf("google.com"), "Ranger.Joe@GMAIL.COM"))
    }

    @Test
    fun `violatesRangerSignInPolicy is true when neither condition is met`() {
        assertTrue(violatesRangerSignInPolicy(listOf("password"), "ranger.joe@example.com"))
    }
}
