package com.nexora.app.ui.screens.energy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.model.*
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.EnergyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EnergyUiState(
    val isLoading: Boolean = false,
    val bills: List<BillDto> = emptyList(),
    val usage: List<EnergyUsageDto> = emptyList(),
    val analysis: AiAnalysisResponse? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false
)

class EnergyViewModel(
    private val homeId: Int,
    private val repository: EnergyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnergyUiState())
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val billsResult = repository.getBills(homeId)
            val usageResult = repository.getEnergyUsage(homeId)

            if (billsResult is NetworkResult.Success && usageResult is NetworkResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bills = billsResult.data,
                    usage = usageResult.data
                )
                // Get analysis for latest bill if exists
                billsResult.data.firstOrNull()?.let { getAnalysis(it) }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load energy data"
                )
            }
        }
    }

    fun submitBill(amount: Double, usage: Double, start: String, end: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, submitSuccess = false)
            val request = CreateBillRequest(start, end, amount, usage)
            when (val result = repository.submitBill(homeId, request)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true
                    )
                    refreshData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.error.message
                    )
                }
                else -> {}
            }
        }
    }

    private fun getAnalysis(bill: BillDto) {
        viewModelScope.launch {
            when (val result = repository.getBillAnalysis(homeId, bill)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(analysis = result.data)
                }
                else -> {}
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(error = null, submitSuccess = false)
    }
}

class EnergyViewModelFactory(
    private val homeId: Int,
    private val repository: EnergyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EnergyViewModel(homeId, repository) as T
    }
}
