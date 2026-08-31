package com.nexora.app.data.repository

import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.HomeApiService
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

class HomeRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var homeApiService: HomeApiService
    private lateinit var repository: HomeRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = ApiClient.createRetrofit(
            baseUrl = mockWebServer.url("/").toString(),
            okHttpClient = ApiClient.createOkHttpClient(enableLogging = false)
        )
        homeApiService = retrofit.create(HomeApiService::class.java)
        repository = HomeRepositoryImpl(homeApiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getHomes returns list of mapped HomeModels`() = runTest {
        val json = """
            [
                {
                    "id": 1,
                    "name": "My Sweet Home",
                    "owner": 10,
                    "owner_email": "owner@example.com",
                    "created_at": "2026-08-30T10:00:00Z",
                    "updated_at": "2026-08-30T10:00:00Z"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getHomes()

        assertTrue(result is NetworkResult.Success)
        val homes = (result as NetworkResult.Success).data
        assertEquals(1, homes.size)
        assertEquals("My Sweet Home", homes[0].name)
        assertEquals("owner@example.com", homes[0].ownerEmail)
    }

    @Test
    fun `createHome sends request and returns new HomeModel`() = runTest {
        val jsonResponse = """
            {
                "id": 2,
                "name": "Beach House",
                "owner": 10,
                "owner_email": "owner@example.com",
                "created_at": "2026-08-31T12:00:00Z",
                "updated_at": "2026-08-31T12:00:00Z"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.createHome("Beach House")

        assertTrue(result is NetworkResult.Success)
        val home = (result as NetworkResult.Success).data
        assertEquals(2, home.id)
        assertEquals("Beach House", home.name)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/homes/", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("Beach House"))
    }

    @Test
    fun `inviteMember returns 404 when user not found`() = runTest {
        val errorJson = """
            {
                "detail": "No user with this email exists."
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.inviteMember(homeId = 1, email = "nonexistent@example.com")

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        val httpError = error as NetworkError.HttpError
        assertEquals(404, httpError.statusCode)
        assertEquals("No user with this email exists.", httpError.serverMessage)
    }

    @Test
    fun `leaveHome sends POST request and parses detail response`() = runTest {
        val json = """
            {
                "detail": "You have left the home."
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.leaveHome(homeId = 1)

        assertTrue(result is NetworkResult.Success)
        assertEquals("You have left the home.", (result as NetworkResult.Success).data)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/homes/1/leave/", recordedRequest.path)
    }
}
