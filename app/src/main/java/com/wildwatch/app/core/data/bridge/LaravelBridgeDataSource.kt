package com.wildwatch.app.core.data.bridge

import com.wildwatch.app.core.model.Incident

interface LaravelBridgeDataSource {
    /**
     * Forwards an incident (or wildlife sighting / SOS alert - all the same `Incident` row with
     * a different `type`, see IncidentType) to the Laravel API right after it's already been
     * written to Firestore. [eventType] is "create" or "update", matching the shape the old
     * Cloud-Functions-relayed webhook sent.
     */
    suspend fun postIncidentEvent(incident: Incident, eventType: String): Result<Unit>
}
