package com.nexora.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.model.PresenceEventDto
import com.nexora.app.data.model.SecurityEventDto
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.PresenceSecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PresenceSecurityUiState(
    val isLoading: Boolean = false,
    val presenceState: String = "unknown", // home, away, unknown
    val securityMode: String = "disarmed", // disarmed, armed_home, armed_away
    val isStale: Boolean = false,
    val error: String? = null,
    val lastPresenceEvent: PresenceEventDto? = null,
    val lastSecurityEvent: SecurityEventDto? = null,
    val arrivalDetected: Boolean = false
)

class PresenceSecurityViewModel(
    private val homeId: Int,
    private val repository: PresenceSecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresenceSecurityUiState())
    val uiState: StateFlow<PresenceSecurityUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val presenceResult = repository.getPresenceEvents(homeId)
            val securityResult = repository.getSecurityEvents(homeId)

            if (presenceResult is NetworkResult.Success && securityResult is NetworkResult.Success) {
                val latestPresence = presenceResult.data.firstOrNull()
                val latestSecurity = securityResult.data.firstOrNull()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    presenceState = latestPresence?.state ?: "unknown",
                    securityMode = latestSecurity?.mode ?: "disarmed",
                    lastPresenceEvent = latestPresence,
                    lastSecurityEvent = latestSecurity,
                    isStale = false,
                    arrivalDetected = latestPresence?.source == "arrival_sensor" || 
                                     (latestPresence?.state == "home" && _uiState.value.presenceState == "away")
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load presence or security status",
                    isStale = true
                )
            }
        }
    }

    fun updatePresence(state: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.updatePresence(homeId, state)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        presenceState = result.data.state,
                        lastPresenceEvent = result.data
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update presence"
                    )
                }
                else -> {}
            }
        }
    }

    fun changeSecurityMode(mode: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.changeSecurityMode(homeId, mode)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        securityMode = result.data.mode,
                        lastSecurityEvent = result.data
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to change security mode"
                    )
                }
                else -> {}
            }
        }
    }
}

class PresenceSecurityViewModelFactory(
    private val homeId: Int,
    private val repository: PresenceSecurityRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PresenceSecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PresenceSecurityViewModel(homeId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
