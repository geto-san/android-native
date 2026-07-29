package com.wildwatch.app.feature.report

import androidx.lifecycle.SavedStateHandle
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.model.Incident
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportIncidentViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReportIncidentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        locationRepository = mockk()
        savedStateHandle = SavedStateHandle()
        viewModel = ReportIncidentViewModel(incidentRepository, locationRepository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has canSubmit false`() {
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `canSubmit becomes true when species and description are provided`() {
        viewModel.species = "Gorilla"
        assertFalse(viewModel.uiState.value.canSubmit) // Description still empty

        viewModel.description = "Healthy family spotted."
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `state is restored from SavedStateHandle`() {
        val handle = SavedStateHandle(mapOf("species" to "Elephant", "description" to "Near the river"))
        val vm = ReportIncidentViewModel(incidentRepository, locationRepository, handle)
        
        assertEquals("Elephant", vm.species)
        assertEquals("Near the river", vm.description)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `save updates UI state with incident id on success`() {
        coEvery { incidentRepository.create(any()) } returns sampleIncident(id = "inc-123")

        viewModel.species = "Elephant"
        viewModel.description = "Test description"
        viewModel.save()

        assertEquals("inc-123", viewModel.uiState.value.savedIncidentId)
        coVerify { incidentRepository.create(match { it.species == "Elephant" }) }
    }

    @Test
    fun `addPhoto and removePhoto update the photo list`() {
        viewModel.addPhoto("file:///photo1.jpg")
        assertEquals(listOf("file:///photo1.jpg"), viewModel.uiState.value.photoUris)

        viewModel.removePhoto("file:///photo1.jpg")
        assertEquals(emptyList<String>(), viewModel.uiState.value.photoUris)
    }

    private fun sampleIncident(id: String = "inc-1") = Incident(
        id = id,
        type = IncidentType.CONFLICT,
        status = IncidentStatus.OPEN,
        park = Park.MGAHINGA_GORILLA,
        community = "Buhoma",
        species = "Elephant",
        severity = IncidentSeverity.HIGH,
        summary = null,
        lat = -1.5,
        lng = 29.5,
        locationName = "Buhoma sector",
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        reportedAt = "2026-07-22T00:00:00Z",
        syncStatus = SyncStatus.PENDING,
        lastModified = 1000L,
    )
}
