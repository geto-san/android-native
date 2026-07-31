package com.wildwatch.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.user.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    val formViewMode: StateFlow<String> = userDataRepository.formViewMode
        .map { it ?: "FLOW" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FLOW")

    fun toggleFormViewMode() {
        viewModelScope.launch {
            val next = if (formViewMode.value == "FLOW") "PAGING" else "FLOW"
            userDataRepository.setFormViewMode(next)
        }
    }
}
