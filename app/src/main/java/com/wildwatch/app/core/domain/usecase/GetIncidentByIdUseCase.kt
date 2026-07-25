package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.model.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetIncidentByIdUseCase @Inject constructor(
    private val incidentRepository: IncidentRepository
) {
    operator fun invoke(id: String): Flow<Incident?> = 
        incidentRepository.observeAll().map { incidents -> incidents.find { it.id == id } }
}
