package com.nexora.app.data.remote

import com.nexora.app.data.model.ChangeSecurityModeRequest
import com.nexora.app.data.model.SecurityEventDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SecurityApiService {
    @GET("homes/{home_id}/security/")
    suspend fun getSecurityEvents(@Path("home_id") homeId: Int): Response<List<SecurityEventDto>>

    @POST("homes/{home_id}/security/")
    suspend fun changeSecurityMode(
        @Path("home_id") homeId: Int,
        @Body request: ChangeSecurityModeRequest
    ): Response<SecurityEventDto>
}
