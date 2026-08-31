package com.nexora.app.data.mapper

import com.nexora.app.data.model.DeviceDto
import com.nexora.app.domain.model.DeviceCapability
import com.nexora.app.domain.model.DeviceModel
import com.nexora.app.domain.model.DeviceStatus

fun DeviceDto.toDomainModel(): DeviceModel {
    return DeviceModel(
        id = id,
        name = name,
        room = room,
        type = type,
        status = when (status.lowercase()) {
            "online" -> DeviceStatus.Online
            "offline" -> DeviceStatus.Offline
            else -> DeviceStatus.Unknown
        },
        capabilities = capabilities.map { it.toDeviceCapability() },
        attributes = attributes ?: emptyMap()
    )
}

fun String.toDeviceCapability(): DeviceCapability {
    return when (this.lowercase()) {
        "power" -> DeviceCapability.Power
        "brightness" -> DeviceCapability.Brightness
        "color_temperature" -> DeviceCapability.ColorTemperature
        "rgb" -> DeviceCapability.RGB
        "target_temperature" -> DeviceCapability.TargetTemperature
        "fan_speed" -> DeviceCapability.FanSpeed
        "lock" -> DeviceCapability.Lock
        else -> DeviceCapability.Custom(this)
    }
}
