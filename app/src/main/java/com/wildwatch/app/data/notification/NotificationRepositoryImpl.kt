package com.wildwatch.app.data.notification

import com.wildwatch.app.data.local.db.NotificationDao
import com.wildwatch.app.data.local.db.NotificationEntity
import com.wildwatch.app.data.local.db.NotificationType
import com.wildwatch.app.di.IoDispatcher
import com.wildwatch.app.domain.model.Notification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// wireframe community.notifications - like AlertRepositoryImpl, no backend
// service produces these yet (sighting-confirmed / claim-approved pushes
// would come from Cloud Functions reacting to Firestore writes in a future
// phase), so this seeds representative starter content into Room on first
// read. Every subsequent read/write is a genuine Room query, and marking an
// item read persists across process restarts like the rest of the app.
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {

    override fun observeAll(): Flow<List<Notification>> =
        notificationDao.observeAll()
            .onStart { seedIfEmpty() }
            .map { entities -> entities.map(Notification::fromEntity) }

    override fun observeUnreadCount(): Flow<Int> =
        notificationDao.observeUnreadCount().onStart { seedIfEmpty() }

    override suspend fun markAsRead(id: String) = withContext(ioDispatcher) {
        notificationDao.markRead(id)
    }

    private suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (notificationDao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        notificationDao.insertAll(
            listOf(
                NotificationEntity(
                    id = "seed-notif-1",
                    title = "Your sighting was confirmed",
                    type = NotificationType.SIGHTING_CONFIRMED,
                    createdAt = now - TimeUnit.MINUTES.toMillis(12),
                    isRead = false,
                ),
                NotificationEntity(
                    id = "seed-notif-2",
                    title = "New alert: Elephants near Kichwamba",
                    type = NotificationType.ALERT,
                    createdAt = now - TimeUnit.HOURS.toMillis(1),
                    isRead = false,
                ),
                NotificationEntity(
                    id = "seed-notif-3",
                    title = "Claim CLM-1027 approved — UGX 750,000",
                    type = NotificationType.CLAIM_UPDATE,
                    createdAt = now - TimeUnit.HOURS.toMillis(9),
                    isRead = true,
                ),
                NotificationEntity(
                    id = "seed-notif-4",
                    title = "Ranger patrol scheduled this afternoon",
                    type = NotificationType.PATROL,
                    createdAt = now - TimeUnit.DAYS.toMillis(1),
                    isRead = true,
                ),
            ),
        )
    }
}
