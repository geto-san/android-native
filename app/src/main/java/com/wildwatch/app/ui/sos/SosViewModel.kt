package com.wildwatch.app.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SosUiState(
    val isLoadingLocation: Boolean = true,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val locationError: String? = null,
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracyMeters,
                            locationName = name,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingLocation = false, locationError = error.message ?: "Location unavailable")
                    }
                }
        }
    }
}
