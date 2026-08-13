package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.FirebaseAuthException

object AuthErrorMapper {
    fun messageForSendEmailLink(error: Throwable): String = when (error) {
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
            "ERROR_UNAUTHORIZED_DOMAIN" -> "This app isn't configured to send sign-in links yet."
            else -> error.message ?: "Couldn't send the sign-in link. Please try again."
        }
        else -> error.message ?: "Couldn't send the sign-in link. Please try again."
    }

    fun messageForCompleteEmailLink(error: Throwable): String = when (error) {
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "That doesn't look like the email this link was sent to."
            "ERROR_EXPIRED_ACTION_CODE" -> "This sign-in link has expired. Request a new one."
            "ERROR_INVALID_ACTION_CODE" -> "This sign-in link is invalid or has already been used."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
            else -> error.message ?: "Couldn't complete sign-in. Please request a new link."
        }
        else -> error.message ?: "Couldn't complete sign-in. Please request a new link."
    }
}
