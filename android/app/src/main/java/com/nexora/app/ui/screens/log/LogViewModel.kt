package com.nexora.app.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.model.ActivityLogDto
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogUiState(
    val isLoading: Boolean = false,
    val activityLogs: List<ActivityLogDto> = emptyList(),
    val decisionLogs: List<DecisionLogDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class LogViewModel(
    private val homeId: Int,
    private val repository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val activityResult = repository.getActivityLogs(homeId)
            val decisionResult = repository.getDecisionLogs(homeId)

            if (activityResult is NetworkResult.Success && decisionResult is NetworkResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activityLogs = activityResult.data.sortedByDescending { it.timestamp },
                    decisionLogs = decisionResult.data.sortedByDescending { it.timestamp }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load logs"
                )
            }
        }
    }

    fun approveDecision(logId: Int, approve: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.approveDecision(homeId, logId, approve)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (approve) "Decision approved and executed" else "Decision rejected"
                    )
                    refreshLogs()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.userFriendlyMessage
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

class LogViewModelFactory(
    private val homeId: Int,
    private val repository: LogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogViewModel(homeId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
