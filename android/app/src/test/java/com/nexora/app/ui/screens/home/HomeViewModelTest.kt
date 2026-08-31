package com.nexora.app.ui.screens.home

import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.HomeRepository
import com.nexora.app.data.repository.RoomRepository
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.HomeModel
import com.nexora.app.domain.model.RoomMemberModel
import com.nexora.app.domain.model.RoomModel
import com.nexora.app.domain.model.RoomPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockHomeRepo: FakeHomeRepository
    private lateinit var mockRoomRepo: FakeRoomRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockHomeRepo = FakeHomeRepository()
        mockRoomRepo = FakeRoomRepository()
        viewModel = HomeViewModel(mockHomeRepo, mockRoomRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHomes updates uiState with loaded homes and auto selects first home`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.homes.size)
        assertEquals("Primary Home", state.homes[0].name)
        assertNotNull(state.selectedHome)
        assertEquals("Primary Home", state.selectedHome?.name)
    }

    @Test
    fun `createHome with blank name sets validation error`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.createHome("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Home name cannot be empty.", state.errorMessage)
    }

    @Test
    fun `inviteMember with invalid email sets validation error`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.inviteMember("notanemail")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Please enter a valid email address.", state.errorMessage)
    }
}

// --- Fakes for Unit Testing ---

class FakeHomeRepository : HomeRepository {
    private val homes = mutableListOf(
        HomeModel(1, "Primary Home", 10, "owner@example.com", "2026-08-30", "2026-08-30")
    )
    private val members = mutableListOf(
        HomeMemberModel(1, "owner@example.com", "Alice", "Smith", "owner", "2026-08-30")
    )

    override suspend fun getHomes(): NetworkResult<List<HomeModel>> = NetworkResult.Success(homes)
    override suspend fun createHome(name: String): NetworkResult<HomeModel> {
        val newHome = HomeModel(homes.size + 1, name, 10, "owner@example.com", "2026-08-31", "2026-08-31")
        homes.add(newHome)
        return NetworkResult.Success(newHome)
    }
    override suspend fun getHomeDetail(homeId: Int): NetworkResult<HomeModel> = NetworkResult.Success(homes.first { it.id == homeId })
    override suspend fun updateHome(homeId: Int, name: String): NetworkResult<HomeModel> {
        val updated = homes.first { it.id == homeId }.copy(name = name)
        return NetworkResult.Success(updated)
    }
    override suspend fun deleteHome(homeId: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun getHomeMembers(homeId: Int): NetworkResult<List<HomeMemberModel>> = NetworkResult.Success(members)
    override suspend fun inviteMember(homeId: Int, email: String): NetworkResult<HomeMemberModel> {
        val newMember = HomeMemberModel(members.size + 1, email, "", "", "member", "2026-08-31")
        members.add(newMember)
        return NetworkResult.Success(newMember)
    }
    override suspend fun removeMember(homeId: Int, memberId: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun leaveHome(homeId: Int): NetworkResult<String> = NetworkResult.Success("Left home")
    override suspend fun joinHome(inviteCode: String): NetworkResult<HomeModel> {
        val joined = HomeModel(homes.size + 1, "Joined Home", 10, "owner@example.com", "2026-08-31", "2026-08-31")
        homes.add(joined)
        return NetworkResult.Success(joined)
    }
}

class FakeRoomRepository : RoomRepository {
    private val rooms = mutableListOf(
        RoomModel(101, 1, "Living Room", "Main room", "2026-08-30", "2026-08-30")
    )

    override suspend fun getRooms(homeId: Int): NetworkResult<List<RoomModel>> = NetworkResult.Success(rooms)
    override suspend fun createRoom(homeId: Int, name: String, description: String?): NetworkResult<RoomModel> {
        val room = RoomModel(rooms.size + 100, homeId, name, description ?: "", "2026-08-31", "2026-08-31")
        rooms.add(room)
        return NetworkResult.Success(room)
    }
    override suspend fun getRoomDetail(homeId: Int, roomId: Int): NetworkResult<RoomModel> = NetworkResult.Success(rooms.first { it.id == roomId })
    override suspend fun updateRoom(homeId: Int, roomId: Int, name: String, description: String?): NetworkResult<RoomModel> {
        val room = rooms.first { it.id == roomId }.copy(name = name, description = description ?: "")
        return NetworkResult.Success(room)
    }
    override suspend fun deleteRoom(homeId: Int, roomId: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun getRoomMembers(homeId: Int, roomId: Int): NetworkResult<List<RoomMemberModel>> = NetworkResult.Success(emptyList())
    override suspend fun assignRoomMember(homeId: Int, roomId: Int, memberId: Int): NetworkResult<RoomMemberModel> {
        return NetworkResult.Success(RoomMemberModel(1, memberId, "user@example.com", "member", true, "2026-08-31"))
    }
    override suspend fun getRoomPreferences(homeId: Int, roomId: Int): NetworkResult<RoomPreferenceModel> {
        return NetworkResult.Success(RoomPreferenceModel(1, roomId, emptyMap(), "2026-08-31", "2026-08-31"))
    }
    override suspend fun setRoomPreferences(homeId: Int, roomId: Int, preferences: Map<String, String>): NetworkResult<RoomPreferenceModel> {
        return NetworkResult.Success(RoomPreferenceModel(1, roomId, preferences, "2026-08-31", "2026-08-31"))
    }
}
