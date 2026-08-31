package com.nexora.app.data.mapper

import com.nexora.app.data.model.HomeDto
import com.nexora.app.data.model.HomeMemberDto
import com.nexora.app.data.model.RoomDto
import com.nexora.app.data.model.RoomMemberDto
import com.nexora.app.data.model.RoomPreferenceDto
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.HomeModel
import com.nexora.app.domain.model.RoomMemberModel
import com.nexora.app.domain.model.RoomModel
import com.nexora.app.domain.model.RoomPreferenceModel
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

fun HomeDto.toDomain(): HomeModel {
    return HomeModel(
        id = id,
        name = name,
        ownerId = owner,
        ownerEmail = ownerEmail ?: "",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )
}

fun HomeMemberDto.toDomain(): HomeMemberModel {
    return HomeMemberModel(
        id = id,
        email = email ?: "",
        firstName = firstName ?: "",
        lastName = lastName ?: "",
        role = role,
        joinedAt = joinedAt ?: ""
    )
}

fun RoomDto.toDomain(defaultHomeId: Int = 0): RoomModel {
    return RoomModel(
        id = id,
        homeId = homeId ?: defaultHomeId,
        name = name,
        description = description ?: "",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )
}

fun RoomMemberDto.toDomain(): RoomMemberModel {
    return RoomMemberModel(
        id = id,
        memberId = memberId ?: id,
        email = email ?: "",
        role = role ?: "",
        isPrimary = isPrimary,
        assignedAt = assignedAt ?: ""
    )
}

fun RoomPreferenceDto.toDomain(): RoomPreferenceModel {
    val map = preferences.mapValues { (_, value) ->
        if (value is JsonPrimitive) {
            value.content
        } else {
            value.toString()
        }
    }
    return RoomPreferenceModel(
        id = id,
        roomId = room,
        preferencesMap = map,
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )
}
