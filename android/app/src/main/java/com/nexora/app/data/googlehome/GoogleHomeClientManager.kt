package com.nexora.app.data.googlehome

import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GoogleHomeClientManager(
    private val authManager: GoogleHomeAuthManager
) {
    private val mutex = Mutex()
    private val lastCommandTimes = mutableMapOf<String, Long>()
    private val lastCommandValues = mutableMapOf<String, Any>()

    private val _structures = MutableStateFlow<List<GoogleStructure>>(emptyList())
    val structures: StateFlow<List<GoogleStructure>> = _structures.asStateFlow()

    private val _rooms = MutableStateFlow<List<GoogleRoom>>(emptyList())
    val rooms: StateFlow<List<GoogleRoom>> = _rooms.asStateFlow()

    private val _devices = MutableStateFlow<List<GoogleDeviceEntity>>(emptyList())
    val devices: StateFlow<List<GoogleDeviceEntity>> = _devices.asStateFlow()

    companion object {
        const val DEBOUNCE_THRESHOLD_MS = 300L
    }

    init {
        initializeMockHomeGraph()
    }

    fun initializeMockHomeGraph() {
        val defaultStructure = GoogleStructure(id = "struct-1", name = "Main Residence")
        val defaultRooms = listOf(
            GoogleRoom(id = "room-101", structureId = "struct-1", name = "Living Room"),
            GoogleRoom(id = "room-102", structureId = "struct-1", name = "Bedroom"),
            GoogleRoom(id = "room-103", structureId = "struct-1", name = "Entrance")
        )
        val defaultDevices = listOf(
            GoogleDeviceEntity(
                id = "google-device-1-main-light",
                structureId = "struct-1",
                roomId = "room-101",
                roomName = "Living Room",
                name = "Main Light",
                type = "light",
                status = GoogleDeviceStatus.ONLINE,
                traits = listOf("power", "brightness"),
                attributes = mapOf("power" to true, "brightness" to 80)
            ),
            GoogleDeviceEntity(
                id = "google-device-2-fan",
                structureId = "struct-1",
                roomId = "room-101",
                roomName = "Living Room",
                name = "Ceiling Fan",
                type = "fan",
                status = GoogleDeviceStatus.ONLINE,
                traits = listOf("power", "fan_speed"),
                attributes = mapOf("power" to false, "fan_speed" to 2)
            ),
            GoogleDeviceEntity(
                id = "google-device-3-ac",
                structureId = "struct-1",
                roomId = "room-102",
                roomName = "Bedroom",
                name = "Air Conditioner",
                type = "ac",
                status = GoogleDeviceStatus.ONLINE,
                traits = listOf("power", "target_temperature"),
                attributes = mapOf("power" to true, "target_temperature" to 24)
            ),
            GoogleDeviceEntity(
                id = "google-device-4-smart-lock",
                structureId = "struct-1",
                roomId = "room-103",
                roomName = "Entrance",
                name = "Smart Lock",
                type = "lock",
                status = GoogleDeviceStatus.ONLINE,
                traits = listOf("lock"),
                attributes = mapOf("lock" to true)
            ),
            GoogleDeviceEntity(
                id = "google-device-5-offline-lamp",
                structureId = "struct-1",
                roomId = "room-102",
                roomName = "Bedroom",
                name = "Offline Lamp",
                type = "light",
                status = GoogleDeviceStatus.OFFLINE,
                traits = listOf("power"),
                attributes = mapOf("power" to false)
            )
        )

        _structures.value = listOf(defaultStructure)
        _rooms.value = defaultRooms
        _devices.value = defaultDevices
    }

    fun setEmptyHome() {
        _structures.value = emptyList()
        _rooms.value = emptyList()
        _devices.value = emptyList()
    }

    fun setDevicesDirectly(deviceList: List<GoogleDeviceEntity>) {
        _devices.value = deviceList
    }

    suspend fun executeDeviceCommand(
        googleDeviceId: String,
        capability: String,
        value: Any
    ): NetworkResult<Unit> {
        if (!authManager.isGranted()) {
            return NetworkResult.Error(
                NetworkError.HttpError(403, "Google Home authorization not granted")
            )
        }

        val commandKey = "$googleDeviceId:$capability"
        val currentTime = System.currentTimeMillis()

        mutex.withLock {
            val lastTime = lastCommandTimes[commandKey] ?: 0L
            val lastValue = lastCommandValues[commandKey]

            // Debounce / duplicate tap protection
            if (currentTime - lastTime < DEBOUNCE_THRESHOLD_MS && lastValue == value) {
                // Ignore duplicate tap
                return NetworkResult.Success(Unit)
            }

            lastCommandTimes[commandKey] = currentTime
            lastCommandValues[commandKey] = value
        }

        val targetDevice = _devices.value.find { it.id == googleDeviceId }
            ?: return NetworkResult.Error(
                NetworkError.HttpError(404, "Device not found in Google Home graph: $googleDeviceId")
            )

        if (targetDevice.status == GoogleDeviceStatus.OFFLINE) {
            return NetworkResult.Error(
                NetworkError.HttpError(503, "Device '${targetDevice.name}' is offline in Google Home")
            )
        }

        if (targetDevice.status == GoogleDeviceStatus.UNKNOWN) {
            return NetworkResult.Error(
                NetworkError.HttpError(503, "Device '${targetDevice.name}' status is unknown")
            )
        }

        if (!targetDevice.traits.contains(capability.lowercase())) {
            return NetworkResult.Error(
                NetworkError.HttpError(400, "Device '${targetDevice.name}' does not support trait '$capability'")
            )
        }

        // Simulate network execution delay
        delay(100)

        // Update local device state in state flow
        _devices.update { currentDevices ->
            currentDevices.map { dev ->
                if (dev.id == googleDeviceId) {
                    val updatedAttributes = dev.attributes.toMutableMap()
                    updatedAttributes[capability] = value
                    dev.copy(attributes = updatedAttributes)
                } else {
                    dev
                }
            }
        }

        return NetworkResult.Success(Unit)
    }
}
