package com.nexora.app.ui.screens.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.DeviceRepository
import com.nexora.app.domain.model.DeviceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DeviceUiState(
    val isLoading: Boolean = false,
    val devices: List<DeviceModel> = emptyList(),
    val errorMessage: String? = null,
    val actionLoading: Boolean = false
)

class DeviceViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    init {
        fetchDevices()
    }

    fun fetchDevices() {
        viewModelScope.launch {
            deviceRepository.getDevices().collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            devices = result.data,
                            errorMessage = null
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = formatErrorMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    fun executeAction(deviceId: String, capability: String, value: Any) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = true)
            val result = deviceRepository.executeAction(deviceId, capability, value)
            if (result is NetworkResult.Error) {
                _uiState.value = _uiState.value.copy(
                    actionLoading = false,
                    errorMessage = formatErrorMessage(result.error)
                )
            } else {
                _uiState.value = _uiState.value.copy(actionLoading = false)
            }
        }
    }

    private fun formatErrorMessage(error: NetworkError): String {
        return when (error) {
            is NetworkError.HttpError -> error.serverMessage ?: "HTTP Error ${error.statusCode}"
            is NetworkError.ConnectivityError -> error.userFriendlyMessage
            is NetworkError.SerializationError -> error.userFriendlyMessage
            is NetworkError.UnknownError -> error.userFriendlyMessage
        }
    }
}

class DeviceViewModelFactory(
    private val deviceRepository: DeviceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(deviceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
