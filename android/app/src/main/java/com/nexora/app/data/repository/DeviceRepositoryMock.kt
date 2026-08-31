package com.nexora.app.data.repository

import com.nexora.app.data.mapper.toDomainModel
import com.nexora.app.data.model.DeviceDto
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.domain.model.DeviceModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeviceRepositoryMock : DeviceRepository {

    private val _devices = MutableStateFlow<List<DeviceDto>>(
        listOf(
            DeviceDto(
                id = "1",
                name = "Main Light",
                room = "Living Room",
                type = "light",
                status = "online",
                capabilities = listOf("power", "brightness"),
                attributes = mapOf("power" to true, "brightness" to 80)
            ),
            DeviceDto(
                id = "2",
                name = "Fan",
                room = "Living Room",
                type = "fan",
                status = "online",
                capabilities = listOf("power", "fan_speed"),
                attributes = mapOf("power" to false, "fan_speed" to 2)
            ),
            DeviceDto(
                id = "3",
                name = "AC",
                room = "Bedroom",
                type = "ac",
                status = "online",
                capabilities = listOf("power", "target_temperature"),
                attributes = mapOf("power" to true, "target_temperature" to 24)
            ),
            DeviceDto(
                id = "4",
                name = "Smart Lock",
                room = "Entrance",
                type = "lock",
                status = "online",
                capabilities = listOf("lock"),
                attributes = mapOf("lock" to true)
            ),
            DeviceDto(
                id = "5",
                name = "Offline Lamp",
                room = "Bedroom",
                type = "light",
                status = "offline",
                capabilities = listOf("power"),
                attributes = mapOf("power" to false)
            )
        )
    )

    override fun getDevices(): Flow<NetworkResult<List<DeviceModel>>> = flow {
        emit(NetworkResult.Loading)
        delay(1000) // Simulate network delay
        _devices.collect { dtos ->
            emit(NetworkResult.Success(dtos.map { it.toDomainModel() }))
        }
    }

    override suspend fun executeAction(deviceId: String, capability: String, value: Any): NetworkResult<Unit> {
        delay(500) // Simulate action delay
        _devices.update { currentDevices ->
            currentDevices.map { device ->
                if (device.id == deviceId) {
                    val newAttributes = (device.attributes ?: emptyMap()).toMutableMap()
                    newAttributes[capability] = value
                    device.copy(attributes = newAttributes)
                } else {
                    device
                }
            }
        }
        return NetworkResult.Success(Unit)
    }
}
