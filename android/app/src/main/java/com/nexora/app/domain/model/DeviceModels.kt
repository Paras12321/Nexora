package com.nexora.app.domain.model

data class DeviceModel(
    val id: String,
    val name: String,
    val room: String,
    val type: String,
    val status: DeviceStatus,
    val capabilities: List<DeviceCapability>,
    val attributes: Map<String, Any> = emptyMap()
)

sealed class DeviceStatus {
    data object Online : DeviceStatus()
    data object Offline : DeviceStatus()
    data object Unknown : DeviceStatus()
}

sealed class DeviceCapability {
    data object Power : DeviceCapability()
    data object Brightness : DeviceCapability()
    data object ColorTemperature : DeviceCapability()
    data object RGB : DeviceCapability()
    data object TargetTemperature : DeviceCapability()
    data object FanSpeed : DeviceCapability()
    data object Lock : DeviceCapability()
    data class Custom(val name: String) : DeviceCapability()
}
