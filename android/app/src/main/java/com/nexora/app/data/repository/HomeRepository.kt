package com.nexora.app.data.repository

import com.nexora.app.data.mapper.toDomain
import com.nexora.app.data.model.CreateHomeRequest
import com.nexora.app.data.model.InviteMemberRequest
import com.nexora.app.data.model.UpdateHomeRequest
import com.nexora.app.data.remote.HomeApiService
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.HomeModel

interface HomeRepository {
    suspend fun getHomes(): NetworkResult<List<HomeModel>>
    suspend fun createHome(name: String): NetworkResult<HomeModel>
    suspend fun getHomeDetail(homeId: Int): NetworkResult<HomeModel>
    suspend fun updateHome(homeId: Int, name: String): NetworkResult<HomeModel>
    suspend fun deleteHome(homeId: Int): NetworkResult<Unit>
    suspend fun getHomeMembers(homeId: Int): NetworkResult<List<HomeMemberModel>>
    suspend fun inviteMember(homeId: Int, email: String): NetworkResult<HomeMemberModel>
    suspend fun removeMember(homeId: Int, memberId: Int): NetworkResult<Unit>
    suspend fun leaveHome(homeId: Int): NetworkResult<String>
}

class HomeRepositoryImpl(
    private val homeApiService: HomeApiService
) : BaseRepository(), HomeRepository {

    override suspend fun getHomes(): NetworkResult<List<HomeModel>> {
        val result = safeApiCall { homeApiService.getHomes() }
        return result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createHome(name: String): NetworkResult<HomeModel> {
        val result = safeApiCall { homeApiService.createHome(CreateHomeRequest(name)) }
        return result.map { it.toDomain() }
    }

    override suspend fun getHomeDetail(homeId: Int): NetworkResult<HomeModel> {
        val result = safeApiCall { homeApiService.getHomeDetail(homeId) }
        return result.map { it.toDomain() }
    }

    override suspend fun updateHome(homeId: Int, name: String): NetworkResult<HomeModel> {
        val result = safeApiCall { homeApiService.updateHome(homeId, UpdateHomeRequest(name)) }
        return result.map { it.toDomain() }
    }

    override suspend fun deleteHome(homeId: Int): NetworkResult<Unit> {
        return safeApiCall { homeApiService.deleteHome(homeId) }
    }

    override suspend fun getHomeMembers(homeId: Int): NetworkResult<List<HomeMemberModel>> {
        val result = safeApiCall { homeApiService.getHomeMembers(homeId) }
        return result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun inviteMember(homeId: Int, email: String): NetworkResult<HomeMemberModel> {
        val result = safeApiCall { homeApiService.inviteHomeMember(homeId, InviteMemberRequest(email)) }
        return result.map { it.toDomain() }
    }

    override suspend fun removeMember(homeId: Int, memberId: Int): NetworkResult<Unit> {
        return safeApiCall { homeApiService.removeHomeMember(homeId, memberId) }
    }

    override suspend fun leaveHome(homeId: Int): NetworkResult<String> {
        val result = safeApiCall { homeApiService.leaveHome(homeId) }
        return result.map { it.detail }
    }
}
