package com.nexora.app.data.repository

import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.model.LoginRequest
import com.nexora.app.data.model.RegisterRequest
import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.session.SessionRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authApiService: AuthApiService
    private lateinit var tokenManager: FakeTokenManager
    private lateinit var sessionRepository: SessionRepository
    private lateinit var repository: AuthRepository

    private class FakeTokenManager : TokenManager {
        private var token: String? = null
        override fun saveToken(token: String) { this.token = token }
        override fun getToken(): String? = token
        override fun clearToken() { this.token = null }
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenManager = FakeTokenManager()
        sessionRepository = SessionRepository(tokenManager)

        val retrofit = ApiClient.createRetrofit(
            baseUrl = mockWebServer.url("/").toString(),
            okHttpClient = ApiClient.createOkHttpClient(enableLogging = false)
        )
        authApiService = retrofit.create(AuthApiService::class.java)
        repository = AuthRepositoryImpl(authApiService, tokenManager, sessionRepository)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login success sends request payload with email username and password and saves token`() = runTest {
        val jsonSuccess = """
            {
                "token": "test_auth_token_123",
                "user": {
                    "id": 1,
                    "email": "user@nexora.com",
                    "first_name": "Nexora",
                    "last_name": "User",
                    "date_joined": "2026-01-01"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonSuccess)
        )

        val result = repository.login(LoginRequest(email = "user@nexora.com", password = "secretpassword"))

        assertTrue(result is NetworkResult.Success)
        val authResponse = (result as NetworkResult.Success).data
        assertEquals("test_auth_token_123", authResponse.token)
        assertEquals("test_auth_token_123", tokenManager.getToken())

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/auth/login/", recordedRequest.path)
        val requestBody = recordedRequest.body.readUtf8()
        assertTrue(requestBody.contains("\"email\":\"user@nexora.com\""))
        assertTrue(requestBody.contains("\"username\":\"user@nexora.com\""))
        assertTrue(requestBody.contains("\"password\":\"secretpassword\""))
    }

    @Test
    fun `login 400 bad request error returns HttpError with parsed detail`() = runTest {
        val errorJson = """
            {
                "non_field_errors": ["Unable to log in with provided credentials."]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login(LoginRequest(email = "wrong@nexora.com", password = "badpassword"))

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        val httpError = error as NetworkError.HttpError
        assertEquals(400, httpError.statusCode)
        assertEquals("Unable to log in with provided credentials.", httpError.message)
    }

    @Test
    fun `login 400 bad request with field validation error returns formatted message`() = runTest {
        val errorJson = """
            {
                "email": ["Enter a valid email address."],
                "password": ["This field may not be blank."]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login(LoginRequest(email = "invalid-email", password = ""))

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        val httpError = error as NetworkError.HttpError
        assertEquals(400, httpError.statusCode)
        assertTrue(httpError.message.contains("Email: Enter a valid email address."))
        assertTrue(httpError.message.contains("Password: This field may not be blank."))
    }

    @Test
    fun `login generic backend error does not fallback to fake success`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"detail\": \"Server error\"}")
        )

        val result = repository.login(LoginRequest(email = "user@nexora.com", password = "secretpassword"))

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        assertEquals(500, (error as NetworkError.HttpError).statusCode)
        assertEquals("Server error", error.message)
    }

    @Test
    fun `register generic backend error does not fallback to fake success`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"detail\": \"Invalid request. Please check your inputs and try again.\"}")
        )

        val result = repository.register(
            RegisterRequest(
                email = "newuser@nexora.com",
                password = "VeryStrongPassword123!",
                firstName = "New",
                lastName = "User"
            )
        )

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        assertEquals(400, (error as NetworkError.HttpError).statusCode)
        assertEquals("Invalid request. Please check your inputs and try again.", error.message)
    }
}
