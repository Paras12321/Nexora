package com.nexora.app.data.googlehome

import com.nexora.app.data.mapper.toDeviceCapability
import com.nexora.app.data.model.DeviceDto
import com.nexora.app.domain.model.DeviceModel
import com.nexora.app.domain.model.DeviceStatus

fun GoogleDeviceEntity.toDeviceDto(): DeviceDto {
    return DeviceDto(
        id = id,
        name = name,
        room = roomName,
        type = type,
        status = when (status) {
            GoogleDeviceStatus.ONLINE -> "online"
            GoogleDeviceStatus.OFFLINE -> "offline"
            GoogleDeviceStatus.UNKNOWN -> "unknown"
        },
        capabilities = traits,
        attributes = attributes
    )
}

fun GoogleDeviceEntity.toDomainModel(): DeviceModel {
    return DeviceModel(
        id = id,
        name = name,
        room = roomName,
        type = type,
        status = when (status) {
            GoogleDeviceStatus.ONLINE -> DeviceStatus.Online
            GoogleDeviceStatus.OFFLINE -> DeviceStatus.Offline
            GoogleDeviceStatus.UNKNOWN -> DeviceStatus.Unknown
        },
        capabilities = traits.map { it.toDeviceCapability() },
        attributes = attributes
    )
}
