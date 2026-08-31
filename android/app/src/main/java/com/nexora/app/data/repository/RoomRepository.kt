package com.nexora.app.data.repository

import com.nexora.app.data.mapper.toDomain
import com.nexora.app.data.model.AssignRoomMemberRequest
import com.nexora.app.data.model.CreateRoomRequest
import com.nexora.app.data.model.SetRoomPreferenceRequest
import com.nexora.app.data.model.UpdateRoomRequest
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.remote.RoomApiService
import com.nexora.app.domain.model.RoomMemberModel
import com.nexora.app.domain.model.RoomModel
import com.nexora.app.domain.model.RoomPreferenceModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface RoomRepository {
    suspend fun getRooms(homeId: Int): NetworkResult<List<RoomModel>>
    suspend fun createRoom(homeId: Int, name: String, description: String? = null): NetworkResult<RoomModel>
    suspend fun getRoomDetail(homeId: Int, roomId: Int): NetworkResult<RoomModel>
    suspend fun updateRoom(homeId: Int, roomId: Int, name: String, description: String? = null): NetworkResult<RoomModel>
    suspend fun deleteRoom(homeId: Int, roomId: Int): NetworkResult<Unit>
    suspend fun getRoomMembers(homeId: Int, roomId: Int): NetworkResult<List<RoomMemberModel>>
    suspend fun assignRoomMember(homeId: Int, roomId: Int, memberId: Int): NetworkResult<RoomMemberModel>
    suspend fun getRoomPreferences(homeId: Int, roomId: Int): NetworkResult<RoomPreferenceModel>
    suspend fun setRoomPreferences(homeId: Int, roomId: Int, preferences: Map<String, String>): NetworkResult<RoomPreferenceModel>
}

class RoomRepositoryImpl(
    private val roomApiService: RoomApiService
) : BaseRepository(), RoomRepository {

    override suspend fun getRooms(homeId: Int): NetworkResult<List<RoomModel>> {
        val result = safeApiCall { roomApiService.getRooms(homeId) }
        return result.map { list -> list.map { it.toDomain(defaultHomeId = homeId) } }
    }

    override suspend fun createRoom(homeId: Int, name: String, description: String?): NetworkResult<RoomModel> {
        val result = safeApiCall { roomApiService.createRoom(homeId, CreateRoomRequest(name, description)) }
        return result.map { it.toDomain(defaultHomeId = homeId) }
    }

    override suspend fun getRoomDetail(homeId: Int, roomId: Int): NetworkResult<RoomModel> {
        val result = safeApiCall { roomApiService.getRoomDetail(homeId, roomId) }
        return result.map { it.toDomain(defaultHomeId = homeId) }
    }

    override suspend fun updateRoom(homeId: Int, roomId: Int, name: String, description: String?): NetworkResult<RoomModel> {
        val result = safeApiCall { roomApiService.updateRoom(homeId, roomId, UpdateRoomRequest(name, description)) }
        return result.map { it.toDomain(defaultHomeId = homeId) }
    }

    override suspend fun deleteRoom(homeId: Int, roomId: Int): NetworkResult<Unit> {
        return safeApiCall { roomApiService.deleteRoom(homeId, roomId) }
    }

    override suspend fun getRoomMembers(homeId: Int, roomId: Int): NetworkResult<List<RoomMemberModel>> {
        val result = safeApiCall { roomApiService.getRoomMembers(homeId, roomId) }
        return result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun assignRoomMember(homeId: Int, roomId: Int, memberId: Int): NetworkResult<RoomMemberModel> {
        val result = safeApiCall { roomApiService.assignRoomMember(homeId, roomId, AssignRoomMemberRequest(memberId)) }
        return result.map { it.toDomain() }
    }

    override suspend fun getRoomPreferences(homeId: Int, roomId: Int): NetworkResult<RoomPreferenceModel> {
        val result = safeApiCall { roomApiService.getRoomPreferences(homeId, roomId) }
        return result.map { it.toDomain() }
    }

    override suspend fun setRoomPreferences(homeId: Int, roomId: Int, preferences: Map<String, String>): NetworkResult<RoomPreferenceModel> {
        val jsonObject = JsonObject(preferences.mapValues { JsonPrimitive(it.value) })
        val result = safeApiCall { roomApiService.setRoomPreferences(homeId, roomId, SetRoomPreferenceRequest(jsonObject)) }
        return result.map { it.toDomain() }
    }
}
