package com.nexora.app.data.model

data class DeviceDto(
    val id: String,
    val name: String,
    val room: String,
    val type: String,
    val status: String,
    val capabilities: List<String>,
    val attributes: Map<String, Any>? = null
)
