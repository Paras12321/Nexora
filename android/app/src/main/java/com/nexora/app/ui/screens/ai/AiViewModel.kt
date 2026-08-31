package com.nexora.app.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.DetailedBillAnalysis
import com.nexora.app.data.model.NaturalLanguageAiResponse
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiUiState(
    val isLoading: Boolean = false,
    val isAnalyzingMessage: Boolean = false,
    val billAnalysis: DetailedBillAnalysis? = null,
    val energyInsightsContent: String? = null,
    val automationRecommendationContent: String? = null,
    val naturalLanguageResponse: NaturalLanguageAiResponse? = null,
    val pendingDecisions: List<DecisionLogDto> = emptyList(),
    val actionLoadingLogId: Int? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AiViewModel(
    val homeId: Int,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val billResult = aiRepository.getBillAnalysis(
                homeId = homeId,
                billAmount = 220.0,
                averageBill = 180.0,
                usageKwh = 850.0,
                billingPeriod = "2026-08"
            )
            val insightsResult = aiRepository.getEnergyInsights(homeId, usageKwh = 850.0)
            val automationResult = aiRepository.getAutomationRecommendations(homeId)
            val decisionsResult = aiRepository.getDecisionLogs(homeId)

            var errorMsg: String? = null
            var billAnalysisData: DetailedBillAnalysis? = null
            var insightsText: String? = null
            var automationText: String? = null
            var decisionsList: List<DecisionLogDto> = emptyList()

            when (billResult) {
                is NetworkResult.Success -> billAnalysisData = billResult.data
                is NetworkResult.Error -> errorMsg = formatError(billResult.error)
                else -> {}
            }

            when (insightsResult) {
                is NetworkResult.Success -> insightsText = insightsResult.data.content
                is NetworkResult.Error -> if (errorMsg == null) errorMsg = formatError(insightsResult.error)
                else -> {}
            }

            when (automationResult) {
                is NetworkResult.Success -> automationText = automationResult.data.content
                is NetworkResult.Error -> if (errorMsg == null) errorMsg = formatError(automationResult.error)
                else -> {}
            }

            when (decisionsResult) {
                is NetworkResult.Success -> {
                    decisionsList = decisionsResult.data.filter { it.status == "pending_approval" }
                }
                is NetworkResult.Error -> if (errorMsg == null) errorMsg = formatError(decisionsResult.error)
                else -> {}
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                billAnalysis = billAnalysisData,
                energyInsightsContent = insightsText,
                automationRecommendationContent = automationText,
                pendingDecisions = decisionsList,
                errorMessage = errorMsg
            )
        }
    }

    fun sendNaturalLanguageQuery(query: String) {
        if (query.isBlank() || _uiState.value.isAnalyzingMessage) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzingMessage = true, errorMessage = null)
            when (val result = aiRepository.analyzeMessage(homeId, query)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzingMessage = false,
                        naturalLanguageResponse = result.data
                    )
                    // Refresh decision logs if a decision entry was generated
                    if (result.data.decisionLogId != null || result.data.requiresConfirmation) {
                        refreshDecisionLogs()
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzingMessage = false,
                        errorMessage = formatError(result.error)
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAnalyzingMessage = false)
                }
            }
        }
    }

    fun approveDecision(logId: Int) {
        if (_uiState.value.actionLoadingLogId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoadingLogId = logId, errorMessage = null)
            when (val result = aiRepository.approveDecision(homeId, logId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingLogId = null,
                        successMessage = "Decision approved successfully: ${result.data.detail}"
                    )
                    refreshDecisionLogs()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingLogId = null,
                        errorMessage = formatError(result.error)
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(actionLoadingLogId = null)
                }
            }
        }
    }

    fun rejectDecision(logId: Int) {
        if (_uiState.value.actionLoadingLogId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoadingLogId = logId, errorMessage = null)
            when (val result = aiRepository.rejectDecision(homeId, logId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingLogId = null,
                        successMessage = "Decision rejected successfully: ${result.data.detail}"
                    )
                    refreshDecisionLogs()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingLogId = null,
                        errorMessage = formatError(result.error)
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(actionLoadingLogId = null)
                }
            }
        }
    }

    private fun refreshDecisionLogs() {
        viewModelScope.launch {
            when (val result = aiRepository.getDecisionLogs(homeId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        pendingDecisions = result.data.filter { it.status == "pending_approval" }
                    )
                }
                else -> {}
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun formatError(error: NetworkError): String {
        return when (error) {
            is NetworkError.HttpError -> error.serverMessage ?: "HTTP Error ${error.statusCode}"
            is NetworkError.ConnectivityError -> error.userFriendlyMessage
            is NetworkError.SerializationError -> error.userFriendlyMessage
            is NetworkError.UnknownError -> error.userFriendlyMessage
        }
    }
}

class AiViewModelFactory(
    private val homeId: Int,
    private val aiRepository: AiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiViewModel::class.java)) {
            return AiViewModel(homeId, aiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
