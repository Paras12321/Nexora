package com.nexora.app.data.remote

import com.nexora.app.data.model.CreatePresenceRequest
import com.nexora.app.data.model.PresenceEventDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PresenceApiService {
    @GET("homes/{home_id}/presence/")
    suspend fun getPresenceEvents(@Path("home_id") homeId: Int): Response<List<PresenceEventDto>>

    @POST("homes/{home_id}/presence/")
    suspend fun updatePresence(
        @Path("home_id") homeId: Int,
        @Body request: CreatePresenceRequest
    ): Response<PresenceEventDto>
}
