package com.nexora.app.data.remote

import com.nexora.app.data.local.InMemoryTokenManager
import com.nexora.app.data.session.SessionRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `attaches token header when token is present`() {
        val tokenManager = InMemoryTokenManager("sample_token_123")
        val sessionRepository = SessionRepository(tokenManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager, sessionRepository))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Token sample_token_123", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `does not attach header when token is null`() {
        val tokenManager = InMemoryTokenManager(null)
        val sessionRepository = SessionRepository(tokenManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager, sessionRepository))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `preserves existing authorization header`() {
        val tokenManager = InMemoryTokenManager("sample_token_123")
        val sessionRepository = SessionRepository(tokenManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager, sessionRepository))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Authorization", "Bearer custom_jwt")
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer custom_jwt", recordedRequest.getHeader("Authorization"))
    }
}
