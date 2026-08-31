package com.nexora.app.data.remote

import com.nexora.app.data.model.AssignRoomMemberRequest
import com.nexora.app.data.model.CreateRoomRequest
import com.nexora.app.data.model.RoomDto
import com.nexora.app.data.model.RoomMemberDto
import com.nexora.app.data.model.RoomPreferenceDto
import com.nexora.app.data.model.SetRoomPreferenceRequest
import com.nexora.app.data.model.UpdateRoomRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RoomApiService {

    @GET("homes/{home_id}/rooms/")
    suspend fun getRooms(@Path("home_id") homeId: Int): Response<List<RoomDto>>

    @POST("homes/{home_id}/rooms/")
    suspend fun createRoom(
        @Path("home_id") homeId: Int,
        @Body body: CreateRoomRequest
    ): Response<RoomDto>

    @GET("homes/{home_id}/rooms/{room_id}/")
    suspend fun getRoomDetail(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int
    ): Response<RoomDto>

    @PUT("homes/{home_id}/rooms/{room_id}/")
    suspend fun updateRoom(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int,
        @Body body: UpdateRoomRequest
    ): Response<RoomDto>

    @DELETE("homes/{home_id}/rooms/{room_id}/")
    suspend fun deleteRoom(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int
    ): Response<Unit>

    @GET("homes/{home_id}/rooms/{room_id}/members/")
    suspend fun getRoomMembers(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int
    ): Response<List<RoomMemberDto>>

    @POST("homes/{home_id}/rooms/{room_id}/members/")
    suspend fun assignRoomMember(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int,
        @Body body: AssignRoomMemberRequest
    ): Response<RoomMemberDto>

    @GET("homes/{home_id}/rooms/{room_id}/preferences/")
    suspend fun getRoomPreferences(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int
    ): Response<RoomPreferenceDto>

    @POST("homes/{home_id}/rooms/{room_id}/preferences/")
    suspend fun setRoomPreferences(
        @Path("home_id") homeId: Int,
        @Path("room_id") roomId: Int,
        @Body body: SetRoomPreferenceRequest
    ): Response<RoomPreferenceDto>
}
