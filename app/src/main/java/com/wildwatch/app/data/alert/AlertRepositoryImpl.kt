package com.wildwatch.app.data.alert

import com.wildwatch.app.data.local.db.AlertCategory
import com.wildwatch.app.data.local.db.AlertDao
import com.wildwatch.app.data.local.db.AlertEntity
import com.wildwatch.app.data.local.db.AlertSeverity
import com.wildwatch.app.di.IoDispatcher
import com.wildwatch.app.domain.model.Alert
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// No admin tool exists yet to author real alerts, so this repository seeds
// the local table with representative starter content the first time it's
// read (a real, one-time database write - not a hardcoded list re-rendered
// every recomposition). Once seeded, every read is a genuine Room query.
@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AlertRepository {

    override fun observeAll(): Flow<List<Alert>> =
        alertDao.observeAll()
            .onStart { seedIfEmpty() }
            .map { entities -> entities.map(Alert::fromEntity) }

    private suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (alertDao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        alertDao.insertAll(
            listOf(
                AlertEntity(
                    id = "seed-alert-1",
                    title = "Elephant herd movement",
                    description = "Herd of ~12 elephants heading toward Kichwamba village. Keep distance.",
                    location = "Kichwamba",
                    category = AlertCategory.WILDLIFE,
                    severity = AlertSeverity.URGENT,
                    createdAt = now - TimeUnit.MINUTES.toMillis(10),
                ),
                AlertEntity(
                    id = "seed-alert-2",
                    title = "Buffalo sighting",
                    description = "Single buffalo seen near the river crossing.",
                    location = "Buliisa",
                    category = AlertCategory.WILDLIFE,
                    severity = AlertSeverity.CAUTION,
                    createdAt = now - TimeUnit.HOURS.toMillis(2),
                ),
                AlertEntity(
                    id = "seed-alert-3",
                    title = "Ranger patrol scheduled",
                    description = "Patrol team in your area today from 14:00 to 18:00.",
                    location = "Pakwach",
                    category = AlertCategory.PATROLS,
                    severity = AlertSeverity.INFO,
                    createdAt = now - TimeUnit.HOURS.toMillis(6),
                ),
                AlertEntity(
                    id = "seed-alert-4",
                    title = "Snare trap warning",
                    description = "Multiple snares discovered. Report any you find.",
                    location = "Wairingo",
                    category = AlertCategory.TRAPPING,
                    severity = AlertSeverity.URGENT,
                    createdAt = now - TimeUnit.DAYS.toMillis(1),
                ),
            ),
        )
    }
}
