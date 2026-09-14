package com.silversentry.sentry.core.domain.usecase

import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.model.User
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<User?> = authRepository.currentUser
}
