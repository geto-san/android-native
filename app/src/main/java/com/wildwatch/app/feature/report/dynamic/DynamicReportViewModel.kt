package com.wildwatch.app.feature.report.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.sync.SyncScheduler
import com.wildwatch.app.feature.report.dynamic.model.FormEngine
import com.wildwatch.app.feature.report.dynamic.model.FormViewMode
import com.wildwatch.app.feature.report.dynamic.model.Question
import com.wildwatch.app.feature.report.dynamic.model.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DynamicReportUiState(
    val type: IncidentType = IncidentType.SIGHTING,
    val draftId: String? = null,
    val viewMode: FormViewMode = FormViewMode.FLOW,
    val visibleQuestions: List<Question> = emptyList(),
    val answers: Map<String, Any?> = emptyMap(),
    val currentPageIndex: Int = 0,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedIncidentId: String? = null,
    val canSubmit: Boolean = false,
    val isLocationLoading: Boolean = false,
    val locationName: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val schemaVersion: String? = null,
)

@HiltViewModel
class DynamicReportViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val userDataRepository: com.wildwatch.app.core.data.user.UserDataRepository,
    private val syncScheduler: SyncScheduler,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DynamicReportUiState())
    val uiState: StateFlow<DynamicReportUiState> = _uiState.asStateFlow()

    private var engine: FormEngine? = null

    init {
        userDataRepository.formViewMode.onEach { mode ->
            val viewMode = if (mode == "PAGING") FormViewMode.PAGING else FormViewMode.FLOW
            _uiState.update { it.copy(viewMode = viewMode) }
        }.launchIn(viewModelScope)
    }

    fun initialize(type: IncidentType, questions: List<Question>, draftId: String? = null, schemaVersion: String? = null) {
        val newEngine = FormEngine(questions)
        engine = newEngine
        _uiState.update {
            it.copy(
                type = type,
                draftId = draftId,
                schemaVersion = schemaVersion,
                visibleQuestions = newEngine.visibleQuestions.value,
                answers = newEngine.answers.value,
            )
        }

        if (draftId != null) {
            viewModelScope.launch {
                incidentRepository.getById(draftId)?.let { incident ->
                    incident.animalSeen?.let { seen ->
                        newEngine.updateAnswer("Have_you_seen_the_wild_animal_", if (seen) "yes" else "no")
                    }
                    newEngine.updateAnswer("_2_2_Which_wild_animal_have_yo", listOf(incident.species))
                    newEngine.updateAnswer("severity", incident.severity)
                    newEngine.updateAnswer("description", incident.summary)
                    newEngine.updateAnswer("photos", incident.localImageUris)
                    incident.district?.let { newEngine.updateAnswer("District", it) }
                    incident.subCounty?.let { newEngine.updateAnswer("Sub_county", it) }
                    incident.parish?.let { newEngine.updateAnswer("_1_4_Parish", it) }
                    incident.community.takeIf { it.isNotBlank() }?.let { newEngine.updateAnswer("_1_5_Village", it) }
                }
            }
        }

        newEngine.visibleQuestions.onEach { visible ->
            _uiState.update { it.copy(visibleQuestions = visible) }
            validate()
        }.launchIn(viewModelScope)

        newEngine.answers.onEach { answers ->
            _uiState.update { it.copy(answers = answers) }
            validate()
        }.launchIn(viewModelScope)

        loadCurrentLocation()
    }

    fun updateAnswer(questionId: String, answer: Any?) {
        engine?.updateAnswer(questionId, answer)
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val nextMode = if (_uiState.value.viewMode == FormViewMode.FLOW) "PAGING" else "FLOW"
            userDataRepository.setFormViewMode(nextMode)
        }
    }

    fun setCurrentPage(index: Int) {
        _uiState.update { it.copy(currentPageIndex = index) }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocationLoading = true) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            lat = location.latitude,
                            lng = location.longitude,
                            locationName = name,
                            isLocationLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLocationLoading = false, saveError = "Failed location: ${error.message}") }
                }
        }
    }

    private fun validate() {
        val questions = engine?.questions ?: return
        val answers = _uiState.value.answers

        val allRequiredFilled = questions
            .filter { it.isRequired }
            .filter { it.visibilityCondition?.invoke(answers) ?: true }
            .all { q ->
                val ans = answers[q.id]
                when (q.type) {
                    QuestionType.PHOTOS -> (ans as? List<*>)?.isNotEmpty() ?: false
                    else -> ans?.toString()?.isNotBlank() ?: false
                }
            }

        _uiState.update { it.copy(canSubmit = allRequiredFilled) }
    }

    fun save(asDraft: Boolean = false) {
        val state = _uiState.value
        val answers = state.answers

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val animalSeenAnswer = answers["Have_you_seen_the_wild_animal_"]?.toString()
                val animalSeen = when (animalSeenAnswer) {
                    "yes" -> true
                    "no" -> false
                    else -> null
                }

                val village = firstNonBlank(
                    answers["_1_5_Village"]?.toString(),
                    answers["_1_5_Village_001"]?.toString(),
                    state.locationName,
                ) ?: "Unknown"

                val details = NewIncidentDetails(
                    type = state.type,
                    park = Park.BWINDI_IMPENETRABLE,
                    district = answers["District"]?.toString(),
                    subCounty = answers["Sub_county"]?.toString() ?: answers["Sub_county_001"]?.toString(),
                    parish = answers["_1_4_Parish"]?.toString() ?: answers["Parish"]?.toString(),
                    community = village,
                    species = (answers["_2_2_Which_wild_animal_have_yo"] as? List<*>)?.firstOrNull()?.toString() ?: "Unknown",
                    severity = (answers["severity"] as? IncidentSeverity) ?: IncidentSeverity.MEDIUM,
                    category = answers["What_effect_conflict_has_the_"]?.toString(),
                    summary = answers["description"]?.toString() ?: "",
                    lat = state.lat,
                    lng = state.lng,
                    locationName = state.locationName,
                    localImageUris = (answers["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    animalSeen = animalSeen,
                    answersJson = encodeAnswers(answers),
                    schemaVersion = state.schemaVersion,
                )

                val incidentId = if (state.draftId != null) {
                    incidentRepository.update(state.draftId, details, asDraft)
                    state.draftId
                } else {
                    incidentRepository.create(details, asDraft).id
                }

                if (!asDraft) {
                    syncScheduler.triggerImmediateSync()
                    notificationRepository.notifyPendingSync(incidentId)
                }

                _uiState.update { it.copy(isSaving = false, savedIncidentId = incidentId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(savedIncidentId = null) }
    }

    private fun encodeAnswers(answers: Map<String, Any?>): String {
        val serializable = answers.mapValues { (_, value) ->
            when (value) {
                null -> null
                is List<*> -> value.map { it?.toString() }
                else -> value.toString()
            }
        }
        return Json.encodeToString(serializable)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
