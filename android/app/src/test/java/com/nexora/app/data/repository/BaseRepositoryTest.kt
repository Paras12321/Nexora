package com.nexora.app.data.repository

import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.HealthApi
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaseRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var healthApi: HealthApi
    private lateinit var repository: HealthRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = ApiClient.createRetrofit(
            baseUrl = mockWebServer.url("/").toString(),
            okHttpClient = ApiClient.createOkHttpClient(enableLogging = false)
        )
        healthApi = retrofit.create(HealthApi::class.java)
        repository = HealthRepositoryImpl(healthApi)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `successful response returns Success with deserialized model`() = runTest {
        val jsonResponseBody = """
            {
                "status": "ok",
                "service": "nexora-backend"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponseBody)
        )

        val result = repository.getHealthStatus()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("ok", data.status)
        assertEquals("nexora-backend", data.service)
    }

    @Test
    fun `http error parses detail string from error body`() = runTest {
        val errorJson = """
            {
                "detail": "Service temporarily unavailable"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.getHealthStatus()

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        val httpError = error as NetworkError.HttpError
        assertEquals(503, httpError.statusCode)
        assertEquals("Service temporarily unavailable", httpError.serverMessage)
    }

    @Test
    fun `malformed response body returns SerializationError`() = runTest {
        val malformedJson = "{ status: ok, service: missing_quotes }"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(malformedJson)
        )

        val result = repository.getHealthStatus()

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.SerializationError)
    }

    @Test
    fun `connection failure yields ConnectivityError`() = runTest {
        // Shutdown server prematurely to simulate connection drop/unreachable host
        mockWebServer.shutdown()

        val result = repository.getHealthStatus()

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.ConnectivityError)
    }
}
