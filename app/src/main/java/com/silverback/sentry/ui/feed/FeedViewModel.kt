package com.silverback.sentry.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.domain.model.Observation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observationRepository: ObservationRepository,
) : ViewModel() {

    // Room-backed (guardrail G2) - this is the same Flow Phase 4 built, now also
    // fed by Phase 7's live Firestore merge. The UI never touches Firestore.
    val observations: StateFlow<List<Observation>> = observationRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAttended(id: String) {
        viewModelScope.launch {
            observationRepository.markAttended(id)
        }
    }
}
