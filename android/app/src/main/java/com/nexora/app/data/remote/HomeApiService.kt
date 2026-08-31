package com.nexora.app.data.remote

import com.nexora.app.data.model.CreateHomeRequest
import com.nexora.app.data.model.DetailResponse
import com.nexora.app.data.model.HomeDto
import com.nexora.app.data.model.HomeMemberDto
import com.nexora.app.data.model.InviteMemberRequest
import com.nexora.app.data.model.JoinHomeRequest
import com.nexora.app.data.model.UpdateHomeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HomeApiService {

    @GET("homes/")
    suspend fun getHomes(): Response<List<HomeDto>>

    @POST("homes/")
    suspend fun createHome(@Body body: CreateHomeRequest): Response<HomeDto>

    @GET("homes/{home_id}/")
    suspend fun getHomeDetail(@Path("home_id") homeId: Int): Response<HomeDto>

    @PUT("homes/{home_id}/")
    suspend fun updateHome(
        @Path("home_id") homeId: Int,
        @Body body: UpdateHomeRequest
    ): Response<HomeDto>

    @DELETE("homes/{home_id}/")
    suspend fun deleteHome(@Path("home_id") homeId: Int): Response<Unit>

    @GET("homes/{home_id}/members/")
    suspend fun getHomeMembers(@Path("home_id") homeId: Int): Response<List<HomeMemberDto>>

    @POST("homes/{home_id}/members/")
    suspend fun inviteHomeMember(
        @Path("home_id") homeId: Int,
        @Body body: InviteMemberRequest
    ): Response<HomeMemberDto>

    @DELETE("homes/{home_id}/members/{member_id}/")
    suspend fun removeHomeMember(
        @Path("home_id") homeId: Int,
        @Path("member_id") memberId: Int
    ): Response<Unit>

    @POST("homes/{home_id}/leave/")
    suspend fun leaveHome(@Path("home_id") homeId: Int): Response<DetailResponse>

    @POST("homes/join/")
    suspend fun joinHome(@Body body: JoinHomeRequest): Response<HomeDto>
}
