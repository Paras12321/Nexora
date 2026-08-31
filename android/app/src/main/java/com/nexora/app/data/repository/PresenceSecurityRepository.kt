package com.nexora.app.data.repository

import com.nexora.app.data.model.*
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.remote.PresenceApiService
import com.nexora.app.data.remote.SecurityApiService

interface PresenceSecurityRepository {
    suspend fun getPresenceEvents(homeId: Int): NetworkResult<List<PresenceEventDto>>
    suspend fun updatePresence(homeId: Int, state: String): NetworkResult<PresenceEventDto>
    suspend fun getSecurityEvents(homeId: Int): NetworkResult<List<SecurityEventDto>>
    suspend fun changeSecurityMode(homeId: Int, mode: String): NetworkResult<SecurityEventDto>
}

class PresenceSecurityRepositoryImpl(
    private val presenceApi: PresenceApiService,
    private val securityApi: SecurityApiService
) : BaseRepository(), PresenceSecurityRepository {

    override suspend fun getPresenceEvents(homeId: Int): NetworkResult<List<PresenceEventDto>> {
        return safeApiCall { presenceApi.getPresenceEvents(homeId) }
    }

    override suspend fun updatePresence(homeId: Int, state: String): NetworkResult<PresenceEventDto> {
        return safeApiCall { presenceApi.updatePresence(homeId, CreatePresenceRequest(state)) }
    }

    override suspend fun getSecurityEvents(homeId: Int): NetworkResult<List<SecurityEventDto>> {
        return safeApiCall { securityApi.getSecurityEvents(homeId) }
    }

    override suspend fun changeSecurityMode(homeId: Int, mode: String): NetworkResult<SecurityEventDto> {
        return safeApiCall { securityApi.changeSecurityMode(homeId, ChangeSecurityModeRequest(mode)) }
    }
}
