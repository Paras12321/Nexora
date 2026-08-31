package com.nexora.app.data.repository

import com.nexora.app.data.remote.AiApiService
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.model.AiAnalysisResponse
import com.nexora.app.data.model.DecisionApprovalResponse
import com.nexora.app.data.model.DecisionLogDto
import com.nexora.app.data.model.NaturalLanguageAiResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeValue
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AiRepositoryTest {

    private lateinit var fakeApiService: FakeAiApiService
    private lateinit var repository: AiRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeAiApiService()
        repository = AiRepositoryImpl(fakeApiService)
    }

    @Test
    fun testAnalyzeMessageSuccess() = runTest {
        fakeApiService.shouldReturnError = false
        val result = repository.analyzeMessage(1, "Turn off lights")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("I'll turn off the bedroom light for you.", data.message)
        assertEquals("device_control", data.intent)
        assertTrue(data.requiresConfirmation)
    }

    @Test
    fun testGetBillAnalysisSuccessAndFormatting() = runTest {
        fakeApiService.shouldReturnError = false
        val result = repository.getBillAnalysis(1, 220.0, 180.0, 850.0, "2026-08")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.whyHighLow.contains("higher than historical average"))
        assertEquals(4, data.contributors.size)
        assertNotNull(data.timestamp)
    }

    @Test
    fun testGetDecisionLogsSuccess() = runTest {
        fakeApiService.shouldReturnError = false
        val result = repository.getDecisionLogs(1)

        assertTrue(result is NetworkResult.Success)
        val logs = (result as NetworkResult.Success).data
        assertEquals(1, logs.size)
        assertEquals("pending_approval", logs[0].status)
    }

    @Test
    fun testApproveDecisionSuccess() = runTest {
        fakeApiService.shouldReturnError = false
        val result = repository.approveDecision(1, 101)

        assertTrue(result is NetworkResult.Success)
        val response = (result as NetworkResult.Success).data
        assertEquals("Decision approved and executed.", response.detail)
    }

    @Test
    fun testRejectDecisionSuccess() = runTest {
        fakeApiService.shouldReturnError = false
        val result = repository.rejectDecision(1, 101)

        assertTrue(result is NetworkResult.Success)
        val response = (result as NetworkResult.Success).data
        assertEquals("Decision rejected.", response.detail)
    }

    @Test
    fun testHttpError403ForbiddenHandling() = runTest {
        fakeApiService.shouldReturnHttpError = true
        fakeApiService.errorCode = 403

        val result = repository.getDecisionLogs(1)

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        assertEquals(403, (error as NetworkError.HttpError).statusCode)
    }

    @Test
    fun testTimeoutErrorHandling() = runTest {
        fakeApiService.shouldThrowException = true

        val result = repository.analyzeMessage(1, "What is energy usage?")

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.ConnectivityError)
    }

    @Test
    fun testEmptyDecisionLogsHandling() = runTest {
        fakeApiService.emptyLogs = true

        val result = repository.getDecisionLogs(1)

        assertTrue(result is NetworkResult.Success)
        val logs = (result as NetworkResult.Success).data
        assertTrue(logs.isEmpty())
    }
}

class FakeAiApiService : AiApiService {
    var shouldReturnError = false
    var shouldReturnHttpError = false
    var errorCode = 500
    var shouldThrowException = false
    var emptyLogs = false

    override suspend fun analyzeMessage(request: com.nexora.app.data.model.NaturalLanguageAiRequest): Response<NaturalLanguageAiResponse> {
        if (shouldThrowException) throw IOException("Connection timeout")
        if (shouldReturnHttpError) {
            return Response.error(errorCode, "Forbidden".toResponseBody("application/json".toMediaTypeValue()))
        }
        val response = NaturalLanguageAiResponse(
            message = "I'll turn off the bedroom light for you.",
            intent = "device_control",
            confidence = 0.9,
            policyStatus = "requires_confirmation",
            requiresConfirmation = true,
            decisionLogId = 101
        )
        return Response.success(response)
    }

    override suspend fun getAiLegacyAnalysis(request: com.nexora.app.data.model.AiAnalysisRequest): Response<AiAnalysisResponse> {
        if (shouldThrowException) throw IOException("Network timeout")
        if (shouldReturnHttpError) {
            return Response.error(errorCode, "Internal error".toResponseBody("application/json".toMediaTypeValue()))
        }
        val response = AiAnalysisResponse(
            status = "ok",
            content = "Your August bill is 22% higher than average. Reduce non-essential cooling.",
            decision = "recommend_energy_savings",
            requiresApproval = true,
            confidence = 0.95
        )
        return Response.success(response)
    }

    override suspend fun getDecisionLogs(homeId: Int): Response<List<DecisionLogDto>> {
        if (shouldThrowException) throw IOException("Timeout")
        if (shouldReturnHttpError) {
            return Response.error(errorCode, "Error".toResponseBody("application/json".toMediaTypeValue()))
        }
        if (emptyLogs) {
            return Response.success(emptyList())
        }
        val logs = listOf(
            DecisionLogDto(
                id = 101,
                source = "AI_Agent",
                decision = "turn_off_bedroom_light",
                reason = "No motion detected for 30 minutes",
                status = "pending_approval",
                timestamp = "2026-08-31 17:00"
            )
        )
        return Response.success(logs)
    }

    override suspend fun approveDecisionLog(
        homeId: Int,
        logId: Int,
        request: com.nexora.app.data.model.DecisionApprovalRequest
    ): Response<DecisionApprovalResponse> {
        if (shouldThrowException) throw IOException("Timeout")
        if (shouldReturnHttpError) {
            return Response.error(errorCode, "Error".toResponseBody("application/json".toMediaTypeValue()))
        }
        val message = if (request.action == "approve") "Decision approved and executed." else "Decision rejected."
        return Response.success(DecisionApprovalResponse(detail = message))
    }
}
