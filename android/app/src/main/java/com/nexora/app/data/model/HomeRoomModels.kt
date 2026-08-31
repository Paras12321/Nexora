package com.nexora.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Home DTOs & Requests ---

@Serializable
data class HomeDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("owner") val owner: Int,
    @SerialName("owner_email") val ownerEmail: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateHomeRequest(
    @SerialName("name") val name: String
)

@Serializable
data class UpdateHomeRequest(
    @SerialName("name") val name: String
)

@Serializable
data class HomeMemberDto(
    @SerialName("id") val id: Int,
    @SerialName("email") val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("role") val role: String,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class InviteMemberRequest(
    @SerialName("email") val email: String
)

@Serializable
data class JoinHomeRequest(
    @SerialName("invite_code") val inviteCode: String
)

@Serializable
data class DetailResponse(
    @SerialName("detail") val detail: String
)

// --- Room DTOs & Requests ---

@Serializable
data class RoomDto(
    @SerialName("id") val id: Int,
    @SerialName("home_id") val homeId: Int? = null,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateRoomRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null
)

@Serializable
data class UpdateRoomRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null
)

@Serializable
data class RoomMemberDto(
    @SerialName("id") val id: Int,
    @SerialName("member_id") val memberId: Int? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("assigned_at") val assignedAt: String? = null
)

@Serializable
data class AssignRoomMemberRequest(
    @SerialName("member_id") val memberId: Int
)

@Serializable
data class RoomPreferenceDto(
    @SerialName("id") val id: Int,
    @SerialName("room") val room: Int,
    @SerialName("preferences") val preferences: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SetRoomPreferenceRequest(
    @SerialName("preferences") val preferences: JsonObject
)
