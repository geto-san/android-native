package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.model.User
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<User?> = authRepository.currentUser
}
