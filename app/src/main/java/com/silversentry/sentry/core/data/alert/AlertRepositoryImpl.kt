package com.silversentry.sentry.core.data.alert

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.silversentry.sentry.core.database.AlertCategory
import com.silversentry.sentry.core.database.AlertDao
import com.silversentry.sentry.core.database.AlertEntity
import com.silversentry.sentry.core.database.AlertSeverity
import com.silversentry.sentry.core.di.ApplicationScope
import com.silversentry.sentry.core.di.IoDispatcher
import com.silversentry.sentry.core.model.Alert
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val ALERTS_COLLECTION = "alerts"

// Graceful two-source pattern: the Firestore `alerts` collection (seeded/authorized
// from the backend, mobile read-only) is the authoritative broadcast source and is
// merged into Room via a background listener, while a one-time local seed keeps the
// screen populated offline or before any real alerts have been published. Runtime
// errors from the remote leg are caught and logged so the screen always renders.
@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope,
) : AlertRepository {

    init {
        applicationScope.launch {
            runCatching {
                firestore.collection(ALERTS_COLLECTION)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.e(error, "Alerts snapshot listener error")
                            return@addSnapshotListener
                        }
                        snapshot?.documentChanges?.forEach { change ->
                            val alert = change.document.toAlertEntity()
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED ->
                                    applicationScope.launch {
                                        alertDao.deleteById(alert.id)
                                    }
                                else ->
                                    applicationScope.launch {
                                        alertDao.upsert(alert)
                                    }
                            }
                        }
                    }
            }.onFailure { Timber.e(it, "Alerts remote listener failed to attach") }
        }
    }

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

private fun com.google.firebase.firestore.QueryDocumentSnapshot.toAlertEntity(): AlertEntity {
    val data = data
    fun category(): AlertCategory = when (data["category"] as? String) {
        "SAFETY" -> AlertCategory.SAFETY
        "PATROLS" -> AlertCategory.PATROLS
        "TRAPPING" -> AlertCategory.TRAPPING
        else -> AlertCategory.WILDLIFE
    }
    fun severity(): AlertSeverity = when (data["severity"] as? String) {
        "CAUTION" -> AlertSeverity.CAUTION
        "INFO" -> AlertSeverity.INFO
        else -> AlertSeverity.URGENT
    }
    fun createdAt(): Long = when (val raw = data["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Number -> raw.toLong()
        else -> System.currentTimeMillis()
    }
    return AlertEntity(
        id = id,
        title = data["title"] as? String ?: "Alert",
        description = data["description"] as? String ?: "",
        location = data["location"] as? String ?: "",
        category = category(),
        severity = severity(),
        createdAt = createdAt(),
    )
}
