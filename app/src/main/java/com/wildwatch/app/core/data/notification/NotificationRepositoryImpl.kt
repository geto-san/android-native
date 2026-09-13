package com.wildwatch.app.core.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wildwatch.app.core.database.NotificationDao
import com.wildwatch.app.core.database.NotificationEntity
import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.di.ApplicationScope
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.Notification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val NOTIFICATIONS_COLLECTION = "notifications"
private const val FIELD_TARGET_UID = "target_uid"

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : NotificationRepository {

    // Per-user Firestore listener: the `notifications` collection is written Admin-SDK-only
    // (seed script / Cloud Functions / portal), and security rules scope reads to the
    // document's own target_uid == caller. The listener re-binds whenever the signed-in
    // identity changes (including a guest signing into a real account). Room stays the UI's
    // only read source, so this is strictly additive; a denied or failing remote read is
    // logged and the local inbox just keeps reflecting what it has.
    private var activeRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        applicationScope.launch {
            firebaseAuth.addAuthStateListener { auth ->
                val uid = auth.currentUser?.uid
                activeRegistration?.remove()
                activeRegistration = if (uid != null) attachListener(uid) else null
            }
        }
    }

    private fun attachListener(uid: String): com.google.firebase.firestore.ListenerRegistration =
        firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo(FIELD_TARGET_UID, uid)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Notifications snapshot listener error")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    val notification = change.document.toNotificationEntity()
                    applicationScope.launch {
                        notificationDao.upsert(notification)
                    }
                }
            }

    override fun observeAll(): Flow<List<Notification>> =
        notificationDao.observeAll()
            .onStart { seedIfEmpty() }
            .map { entities -> entities.map(Notification::fromEntity) }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override suspend fun markRead(id: String) = withContext(ioDispatcher) {
        notificationDao.markRead(id)
    }

    override suspend fun notifyPendingSync(incidentId: String) = withContext(ioDispatcher) {
        notificationDao.insertAll(
            listOf(
                NotificationEntity(
                    id = "pending-sync-$incidentId",
                    type = NotificationType.PENDING_SYNC,
                    title = "Report pending upload",
                    message = "Your report is queued and will sync when connectivity allows.",
                    createdAt = System.currentTimeMillis(),
                ),
            ),
        )
    }

    override suspend fun recordIncoming(
        type: NotificationType,
        title: String,
        message: String,
        targetId: String?,
    ) = withContext(ioDispatcher) {
        notificationDao.insertAll(
            listOf(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    title = title,
                    message = message,
                    createdAt = System.currentTimeMillis(),
                    targetId = targetId,
                ),
            ),
        )
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        notificationDao.deleteAll()
    }

    private suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (notificationDao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        notificationDao.insertAll(
            listOf(
                NotificationEntity(
                    id = "seed-notif-1",
                    type = NotificationType.SYSTEM,
                    title = "Welcome to WildWatch",
                    message = "Start protecting wildlife by reporting your first sighting.",
                    createdAt = now
                )
            )
        )
    }
}

private fun com.google.firebase.firestore.QueryDocumentSnapshot.toNotificationEntity(): NotificationEntity {
    val data = data
    fun type(): NotificationType = when (data["type"] as? String) {
        "SIGHTING_APPROVED", "sighting_approved" -> NotificationType.SIGHTING_APPROVED
        "SECURITY_ALERT", "security_alert" -> NotificationType.SECURITY_ALERT
        "LIKE", "like" -> NotificationType.LIKE
        "COMMENT", "comment" -> NotificationType.COMMENT
        "NEW_FEED_ARTICLE", "new_feed_article" -> NotificationType.NEW_FEED_ARTICLE
        else -> NotificationType.SYSTEM
    }
    fun createdAt(): Long = when (val raw = data["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Number -> raw.toLong()
        else -> System.currentTimeMillis()
    }
    return NotificationEntity(
        id = id,
        type = type(),
        title = data["title"] as? String ?: "Notification",
        message = data["message"] as? String ?: "",
        isRead = (data["isRead"] as? Boolean) ?: false,
        createdAt = createdAt(),
        targetId = data["target_id"] as? String,
    )
}
