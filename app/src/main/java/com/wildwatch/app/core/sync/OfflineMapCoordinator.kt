package com.wildwatch.app.core.sync

import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.di.ApplicationScope
import com.wildwatch.app.core.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

// Ranger-only, and only once the server-assigned park claim (User.parkId) is present - see
// CODING-AGENT-PROMPTS.md Prompt 7's "assigned park, not GPS-nearest" decision. Pulled out as
// a standalone function so the trigger condition is unit-testable without a live
// AuthRepository/WorkManager.
internal fun shouldPrefetchOfflineMap(role: UserRole, parkId: String?): Boolean =
    role == UserRole.RANGER && !parkId.isNullOrBlank()

/**
 * Watches the signed-in user for a confirmed ranger park assignment and kicks off a
 * background offline-map download for it. Started once from WildWatchApplication.onCreate,
 * mirroring how SyncScheduler's periodic jobs are kicked off there.
 */
@Singleton
class OfflineMapCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        authRepository.currentUser
            .filterNotNull()
            .map { it.role to it.parkId }
            .distinctUntilChanged()
            .onEach { (role, parkId) ->
                parkId?.takeIf { shouldPrefetchOfflineMap(role, it) }
                    ?.let(syncScheduler::scheduleOfflineMapDownload)
            }
            .launchIn(applicationScope)
    }
}
