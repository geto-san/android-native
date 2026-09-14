package com.silversentry.sentry.core.data.bridge

import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.database.IncidentSeverity
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.database.Park
import com.silversentry.sentry.core.database.SyncStatus
import com.silversentry.sentry.core.model.Incident
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response

// Regression coverage for GRANT-READINESS-REPORT.md §1 ("SOS mirrors Incident Report"): SOS
// and SIGHTING incidents must be posted to their own Laravel bridge endpoints
// (mobile/sos-alerts, mobile/sightings), not always mobile/incidents - the bug that made the
// purpose-built SosAlert/WildlifeSighting backend infrastructure unreachable from the mobile
// app's primary surface.
class LaravelBridgeDataSourceImplTest {

    private lateinit var api: LaravelBridgeApi
    private lateinit var authRepository: AuthRepository
    private lateinit var dataSource: LaravelBridgeDataSourceImpl

    private fun okResponse(): Response<okhttp3.ResponseBody> =
        Response.success("".toResponseBody(null))

    @Before
    fun setUp() {
        api = mockk()
        authRepository = mockk()
        dataSource = LaravelBridgeDataSourceImpl(api, authRepository)
        coEvery { authRepository.getIdToken() } returns "test-token"
    }

    private fun incidentOfType(type: IncidentType): Incident = Incident(
        id = "incident-1",
        type = type,
        status = IncidentStatus.OPEN,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = IncidentSeverity.HIGH,
        lat = -1.0,
        lng = 29.7,
        reportedAt = "2026-09-14T00:00:00Z",
        syncStatus = SyncStatus.PENDING,
        lastModified = 0L,
    )

    @Test
    fun `SOS incident is posted to the sos-alerts endpoint`() = runTest {
        coEvery { api.postSosAlertEvent(any(), any()) } returns okResponse()

        val result = dataSource.postIncidentEvent(incidentOfType(IncidentType.SOS), "create")

        assert(result.isSuccess)
        coVerify(exactly = 1) { api.postSosAlertEvent(any(), any()) }
        coVerify(exactly = 0) { api.postIncidentEvent(any(), any()) }
    }

    @Test
    fun `SIGHTING incident is posted to the sightings endpoint`() = runTest {
        coEvery { api.postSightingEvent(any(), any()) } returns okResponse()

        val result = dataSource.postIncidentEvent(incidentOfType(IncidentType.SIGHTING), "create")

        assert(result.isSuccess)
        coVerify(exactly = 1) { api.postSightingEvent(any(), any()) }
        coVerify(exactly = 0) { api.postIncidentEvent(any(), any()) }
    }

    @Test
    fun `ordinary incident still posts to the generic incidents endpoint`() = runTest {
        coEvery { api.postIncidentEvent(any(), any()) } returns okResponse()

        val result = dataSource.postIncidentEvent(incidentOfType(IncidentType.CONFLICT), "create")

        assert(result.isSuccess)
        coVerify(exactly = 1) { api.postIncidentEvent(any(), any()) }
        coVerify(exactly = 0) { api.postSosAlertEvent(any(), any()) }
        coVerify(exactly = 0) { api.postSightingEvent(any(), any()) }
    }
}
