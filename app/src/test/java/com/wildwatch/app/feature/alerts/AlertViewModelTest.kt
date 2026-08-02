package com.wildwatch.app.feature.alerts

import app.cash.turbine.test
import com.wildwatch.app.core.database.AlertCategory
import com.wildwatch.app.core.database.AlertSeverity
import com.wildwatch.app.core.data.alert.AlertRepository
import com.wildwatch.app.core.model.Alert
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val alertRepository: AlertRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `alerts state reflects repository data`() = runTest {
        val alertList = listOf(
            Alert(
                id = "1",
                title = "Lion nearby",
                description = "Sector A",
                location = "Sector A",
                category = AlertCategory.WILDLIFE,
                severity = AlertSeverity.URGENT,
                createdAt = 1000L
            )
        )
        every { alertRepository.observeAll() } returns flowOf(alertList)

        val viewModel = AlertViewModel(alertRepository)
        
        viewModel.alerts.test {
            assertEquals(alertList, awaitItem())
        }
    }
}
