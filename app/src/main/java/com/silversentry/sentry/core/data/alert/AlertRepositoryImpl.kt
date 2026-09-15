package com.silversentry.sentry.core.data.alert

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.silversentry.sentry.core.database.AlertCategory
import com.silversentry.sentry.core.database.AlertDao
import com.silversentry.sentry.core.database.AlertEntity
import com.silversentry.sentry.core.database.AlertSeverity
import com.silversentry.sentry.core.di.ApplicationScope
import com.silversentry.sentry.core.model.Alert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val ALERTS_COLLECTION = "alerts"

// Single-source pattern: the Firestore `alerts` collection (seeded/authorized from the
// backend, mobile read-only per firestore.rules) is the only broadcast source and is merged
// into Room via a background listener. The old local seed that fabricated four hardcoded
// alerts ("Elephant herd movement", "Buffalo sighting", ...) so the screen was never truly
// empty was removed - fake alerts made it impossible to tell a healthy-but-quiet park from a
// broken listener, and they leaked made-up locations/park lore. Runtime errors from the remote
// leg are caught and logged so the screen renders whatever Room has cached.
@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val firestore: FirebaseFirestore,
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
            .map { entities -> entities.map(Alert::fromEntity) }
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
