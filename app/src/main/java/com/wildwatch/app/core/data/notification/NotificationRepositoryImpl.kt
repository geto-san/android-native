package com.wildwatch.app.core.data.notification

import com.wildwatch.app.core.database.NotificationDao
import com.wildwatch.app.core.database.NotificationEntity
import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.Notification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {

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
