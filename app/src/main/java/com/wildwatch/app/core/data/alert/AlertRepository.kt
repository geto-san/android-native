package com.wildwatch.app.core.data.alert

import com.wildwatch.app.core.model.Alert
import kotlinx.coroutines.flow.Flow

// Read-only: Community Alerts are ranger/UWA-authored broadcast content this
// app has no create/edit UI for (see AlertEntity's doc comment). Real Room
// database reads via Flow, seeded once on first access, rather than the
// hardcoded in-memory list this screen used to render.
interface AlertRepository {
    fun observeAll(): Flow<List<Alert>>
}
