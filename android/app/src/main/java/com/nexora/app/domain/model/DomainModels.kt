package com.nexora.app.domain.model

data class HomeModel(
    val id: Int,
    val name: String,
    val ownerId: Int,
    val ownerEmail: String,
    val createdAt: String,
    val updatedAt: String
)

data class HomeMemberModel(
    val id: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val joinedAt: String
)

data class RoomModel(
    val id: Int,
    val homeId: Int,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String
)

data class RoomMemberModel(
    val id: Int,
    val memberId: Int,
    val email: String,
    val role: String,
    val isPrimary: Boolean,
    val assignedAt: String
)

data class RoomPreferenceModel(
    val id: Int,
    val roomId: Int,
    val preferencesMap: Map<String, String>,
    val createdAt: String,
    val updatedAt: String
)
