package com.nexora.app.data.googlehome

data class GoogleStructure(
    val id: String,
    val name: String
)

data class GoogleRoom(
    val id: String,
    val structureId: String,
    val name: String
)

enum class GoogleDeviceStatus {
    ONLINE,
    OFFLINE,
    UNKNOWN
}

data class GoogleDeviceEntity(
    val id: String,
    val structureId: String,
    val roomId: String,
    val roomName: String,
    val name: String,
    val type: String,
    val status: GoogleDeviceStatus,
    val traits: List<String>,
    val attributes: Map<String, Any> = emptyMap()
)
