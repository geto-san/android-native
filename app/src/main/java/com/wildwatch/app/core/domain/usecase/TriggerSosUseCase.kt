package com.wildwatch.app.core.domain.usecase

import timber.log.Timber
import javax.inject.Inject

class TriggerSosUseCase @Inject constructor() {
    operator fun invoke() {
        Timber.d("SOS Triggered!")
        // Here you would call a repository to send the emergency alert
    }
}
