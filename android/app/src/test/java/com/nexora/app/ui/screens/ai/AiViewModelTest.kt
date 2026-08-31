package com.nexora.app.ui.screens.ai

import com.nexora.app.data.model.AiAnalysisResponse
import com.nexora.app.data.model.BillContributor
import com.nexora.app.data.model.DecisionApprovalResponse
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.DetailedBillAnalysis
import com.nexora.app.data.model.NaturalLanguageAiResponse
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAiRepository
    private lateinit var viewModel: AiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAiRepository()
        viewModel = AiViewModel(homeId = 1, aiRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadAllDataSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.billAnalysis)
        assertNotNull(state.energyInsightsContent)
        assertNotNull(state.automationRecommendationContent)
        assertEquals(1, state.pendingDecisions.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun testSendNaturalLanguageQuerySuccess() = runTest {
        viewModel.sendNaturalLanguageQuery("Why was my energy bill high?")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isAnalyzingMessage)
        assertNotNull(state.naturalLanguageResponse)
        assertEquals("I'll analyze your high energy usage.", state.naturalLanguageResponse?.message)
    }

    @Test
    fun testApproveDecisionSuccess() = runTest {
        viewModel.approveDecision(logId = 101)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.actionLoadingLogId)
        assertTrue(state.successMessage?.contains("approved successfully") == true)
    }

    @Test
    fun testRejectDecisionSuccess() = runTest {
        viewModel.rejectDecision(logId = 101)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.actionLoadingLogId)
        assertTrue(state.successMessage?.contains("rejected successfully") == true)
    }

    @Test
    fun testLoadAllDataFailureSetsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.loadAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.errorMessage)
    }
}

class FakeAiRepository : AiRepository {
    var shouldReturnError = false

    override suspend fun analyzeMessage(homeId: Int?, message: String): NetworkResult<NaturalLanguageAiResponse> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "AI failure"))
        return NetworkResult.Success(
            NaturalLanguageAiResponse(
                message = "I'll analyze your high energy usage.",
                intent = "analytics_request",
                confidence = 0.95
            )
        )
    }

    override suspend fun getBillAnalysis(
        homeId: Int,
        billAmount: Double,
        averageBill: Double,
        usageKwh: Double,
        billingPeriod: String
    ): NetworkResult<DetailedBillAnalysis> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Bill analysis failure"))
        val analysis = DetailedBillAnalysis(
            summary = "August 2026 Summary",
            whyHighLow = "22% higher due to HVAC",
            contributors = listOf(BillContributor("HVAC", 42.0, "Air conditioning")),
            recommendation = "Raise AC 2 degrees",
            timestamp = "2026-08-31 17:00",
            explanation = "HVAC usage was heavy in August."
        )
        return NetworkResult.Success(analysis)
    }

    override suspend fun getEnergyInsights(homeId: Int, usageKwh: Double): NetworkResult<AiAnalysisResponse> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Insights error"))
        return NetworkResult.Success(AiAnalysisResponse(status = "ok", content = "Energy usage looks optimal."))
    }

    override suspend fun getAutomationRecommendations(homeId: Int): NetworkResult<AiAnalysisResponse> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Automation error"))
        return NetworkResult.Success(AiAnalysisResponse(status = "ok", content = "Enable night setback automation."))
    }

    override suspend fun getDecisionLogs(homeId: Int): NetworkResult<List<DecisionLogDto>> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Decision log error"))
        val logs = listOf(
            DecisionLogDto(
                id = 101,
                source = "AI_Agent",
                decision = "Set AC to 24°C",
                reason = "Energy saving mode",
                status = "pending_approval",
                timestamp = "2026-08-31 17:00"
            )
        )
        return NetworkResult.Success(logs)
    }

    override suspend fun approveDecision(homeId: Int, logId: Int): NetworkResult<DecisionApprovalResponse> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Approval failed"))
        return NetworkResult.Success(DecisionApprovalResponse(detail = "Decision 101 executed."))
    }

    override suspend fun rejectDecision(homeId: Int, logId: Int): NetworkResult<DecisionApprovalResponse> {
        if (shouldReturnError) return NetworkResult.Error(NetworkError.HttpError(500, "Rejection failed"))
        return NetworkResult.Success(DecisionApprovalResponse(detail = "Decision 101 rejected."))
    }
}
