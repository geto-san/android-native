package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.model.Incident
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIncidentsUseCase @Inject constructor(
    private val incidentRepository: IncidentRepository
) {
    operator fun invoke(): Flow<List<Incident>> = incidentRepository.observeAll()
}
