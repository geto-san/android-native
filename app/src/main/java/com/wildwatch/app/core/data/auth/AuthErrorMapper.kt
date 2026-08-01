package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

object AuthErrorMapper {
    fun messageForSignIn(error: Throwable, email: String): String = when (error) {
        is FirebaseAuthInvalidUserException -> "No account found for that email address."
        is FirebaseAuthInvalidCredentialsException -> {
            if (error.errorCode == "ERROR_WRONG_PASSWORD") {
                "Incorrect password. Please try again."
            } else {
                "Incorrect password. Please try again."
            }
        }
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_USER_NOT_FOUND" -> "No account found for that email address."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
            else -> "Sign in failed. Check your email and password."
        }
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Sign in failed. Check your email and password."
    }

    fun messageForSignUp(error: Throwable): String = when (error) {
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 8 characters."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
            else -> error.message ?: "Could not create account. Please try again."
        }
        else -> error.message ?: "Could not create account. Please try again."
    }
}
