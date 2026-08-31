package com.nexora.app.data.repository

import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.remote.RoomApiService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var roomApiService: RoomApiService
    private lateinit var repository: RoomRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = ApiClient.createRetrofit(
            baseUrl = mockWebServer.url("/").toString(),
            okHttpClient = ApiClient.createOkHttpClient(enableLogging = false)
        )
        roomApiService = retrofit.create(RoomApiService::class.java)
        repository = RoomRepositoryImpl(roomApiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getRooms returns list of RoomModels`() = runTest {
        val json = """
            [
                {
                    "id": 101,
                    "home_id": 1,
                    "name": "Living Room",
                    "description": "Main living area",
                    "created_at": "2026-08-30T12:00:00Z",
                    "updated_at": "2026-08-30T12:00:00Z"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getRooms(homeId = 1)

        assertTrue(result is NetworkResult.Success)
        val rooms = (result as NetworkResult.Success).data
        assertEquals(1, rooms.size)
        assertEquals("Living Room", rooms[0].name)
        assertEquals("Main living area", rooms[0].description)
    }

    @Test
    fun `assignRoomMember posts member_id and handles 400 when already assigned`() = runTest {
        val errorJson = """
            {
                "detail": "This member is already assigned to the room."
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.assignRoomMember(homeId = 1, roomId = 101, memberId = 5)

        assertTrue(result is NetworkResult.Error)
        val error = (result as NetworkResult.Error).error
        assertTrue(error is NetworkError.HttpError)
        val httpError = error as NetworkError.HttpError
        assertEquals(400, httpError.statusCode)
        assertEquals("This member is already assigned to the room.", httpError.serverMessage)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/homes/1/rooms/101/members/", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("\"member_id\":5"))
    }

    @Test
    fun `setRoomPreferences updates preferences object and maps to domain`() = runTest {
        val responseJson = """
            {
                "id": 1,
                "room": 101,
                "preferences": {
                    "temperature_target": "23.5",
                    "lighting_mode": "warm"
                },
                "created_at": "2026-08-31T10:00:00Z",
                "updated_at": "2026-08-31T10:00:00Z"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val prefs = mapOf("temperature_target" to "23.5", "lighting_mode" to "warm")
        val result = repository.setRoomPreferences(homeId = 1, roomId = 101, preferences = prefs)

        assertTrue(result is NetworkResult.Success)
        val prefModel = (result as NetworkResult.Success).data
        assertEquals(101, prefModel.roomId)
        assertEquals("23.5", prefModel.preferencesMap["temperature_target"])
        assertEquals("warm", prefModel.preferencesMap["lighting_mode"])
    }
}
