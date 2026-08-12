package com.wildwatch.app.feature.report.dynamic

import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.sync.SyncScheduler
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.feature.report.dynamic.model.Question
import com.wildwatch.app.feature.report.dynamic.model.QuestionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var viewModel: DynamicReportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)

        every { userDataRepository.formViewMode } returns MutableStateFlow("FLOW")

        viewModel = DynamicReportViewModel(
            incidentRepository,
            locationRepository,
            userDataRepository,
            syncScheduler,
            notificationRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GPS field is pre-filled when location is loaded`() = runTest {
        val questions = listOf(
            Question(id = "GPS_Coordinates", label = "GPS", type = QuestionType.TEXT)
        )
        
        val mockLocation = GeoLocation(-1.234, 29.567, 10f)
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(mockLocation)
        coEvery { locationRepository.reverseGeocode(any(), any()) } returns "Test Village"

        viewModel.initialize(IncidentType.SIGHTING, questions)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("-1.234, 29.567", uiState.answers["GPS_Coordinates"])
    }

    @Test
    fun `robust extraction handles multiple village ID variants`() = runTest {
        val questions = emptyList<Question>()
        viewModel.initialize(IncidentType.SIGHTING, questions)
        
        // Simulating answers for village in different forms
        viewModel.updateAnswer("_1_5_Village_003", "Murore")
        
        viewModel.save(asDraft = false)
        advanceUntilIdle()

        coVerify {
            incidentRepository.create(match { 
                it.community == "Murore"
            }, false)
        }
    }

    @Test
    fun `robust extraction handles multiple species ID variants`() = runTest {
        val questions = emptyList<Question>()
        viewModel.initialize(IncidentType.SIGHTING, questions)
        
        // Simulating answers for species from Kiruhura path
        viewModel.updateAnswer("_2_0_Animal_involved_001", listOf("1__hippopotamus__enjubu"))
        
        viewModel.save(asDraft = false)
        advanceUntilIdle()

        coVerify {
            incidentRepository.create(match { 
                it.species == "1__hippopotamus__enjubu"
            }, false)
        }
    }
}
