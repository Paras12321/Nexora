package com.nexora.app.data.repository

import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.domain.model.DeviceModel
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevices(): Flow<NetworkResult<List<DeviceModel>>>
    suspend fun executeAction(deviceId: String, capability: String, value: Any): NetworkResult<Unit>
}
