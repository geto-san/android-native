package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.ParkIdMatcher
import com.wildwatch.app.core.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetIncidentsUseCase @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val observeUserUseCase: ObserveUserUseCase,
) {
    // Rangers only ever see their own stationed park's incidents - everywhere this use case
    // is consumed (map, dashboard, home, profile), a ranger station in Bwindi has no reason
    // to see a Queen Elizabeth incident, and previously did (this filter didn't exist at
    // all). Scoped by User.parkId (the server-assigned park custom claim), not GPS-nearest
    // park - same "server-assigned, not GPS" call already made for offline map prefetch (see
    // OfflineMapCoordinator). A ranger with no park assigned yet falls back to seeing
    // everything, same fallback CommunityAlertFilter already uses. Every other role is
    // unaffected - unfiltered, matching behavior before this change.
    operator fun invoke(): Flow<List<Incident>> =
        combine(incidentRepository.observeAll(), observeUserUseCase()) { incidents, user ->
            val parkId = user?.parkId
            if (user?.role == UserRole.RANGER && !parkId.isNullOrBlank()) {
                incidents.filter { ParkIdMatcher.matches(it.park, parkId) }
            } else {
                incidents
            }
        }
}
