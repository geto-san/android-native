package com.wildwatch.app.core.data.notification

import com.wildwatch.app.core.database.NotificationDao
import com.wildwatch.app.core.database.NotificationType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: NotificationDao
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        every { dao.observeAll() } returns flowOf(emptyList())
        repository = NotificationRepositoryImpl(dao, testDispatcher)
    }

    @Test
    fun `recordIncoming persists the notification with its targetId`() = runTest(testDispatcher) {
        repository.recordIncoming(
            type = NotificationType.NEW_FEED_ARTICLE,
            title = "New article",
            message = "Protecting Bwindi",
            targetId = "art-1",
        )

        coVerify {
            dao.insertAll(
                match { entities ->
                    entities.size == 1 &&
                        entities.first().type == NotificationType.NEW_FEED_ARTICLE &&
                        entities.first().title == "New article" &&
                        entities.first().targetId == "art-1" &&
                        !entities.first().isRead
                },
            )
        }
    }

    @Test
    fun `recordIncoming persists a null targetId for types with no navigable target`() = runTest(testDispatcher) {
        repository.recordIncoming(
            type = NotificationType.SECURITY_ALERT,
            title = "Emergency alert",
            message = "A new emergency incident requires attention.",
            targetId = null,
        )

        coVerify {
            dao.insertAll(
                match { entities -> entities.first().targetId == null },
            )
        }
    }
}
