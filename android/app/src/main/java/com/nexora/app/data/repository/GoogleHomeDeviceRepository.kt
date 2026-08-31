package com.nexora.app.data.repository

import com.nexora.app.data.googlehome.GoogleHomeAuthManager
import com.nexora.app.data.googlehome.GoogleHomeAuthState
import com.nexora.app.data.googlehome.GoogleHomeClientManager
import com.nexora.app.data.googlehome.toDomainModel
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.domain.model.DeviceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class GoogleHomeDeviceRepository(
    val authManager: GoogleHomeAuthManager,
    val clientManager: GoogleHomeClientManager
) : DeviceRepository {

    override fun getDevices(): Flow<NetworkResult<List<DeviceModel>>> {
        return combine(
            authManager.authState,
            clientManager.devices
        ) { authState, googleDevices ->
            when (authState) {
                is GoogleHomeAuthState.Granted -> {
                    val domainModels = googleDevices.map { it.toDomainModel() }
                    NetworkResult.Success(domainModels)
                }
                is GoogleHomeAuthState.Denied -> {
                    NetworkResult.Error(
                        NetworkError.HttpError(403, "Google Home permission denied: ${authState.reason}")
                    )
                }
                is GoogleHomeAuthState.Revoked -> {
                    NetworkResult.Error(
                        NetworkError.HttpError(403, "Google Home access has been revoked.")
                    )
                }
                GoogleHomeAuthState.Unauthenticated,
                GoogleHomeAuthState.Authorizing -> {
                    NetworkResult.Error(
                        NetworkError.HttpError(401, "Google Home authentication required.")
                    )
                }
            }
        }
    }

    override suspend fun executeAction(
        deviceId: String,
        capability: String,
        value: Any
    ): NetworkResult<Unit> {
        return when (val state = authManager.authState.value) {
            is GoogleHomeAuthState.Granted -> {
                clientManager.executeDeviceCommand(deviceId, capability, value)
            }
            is GoogleHomeAuthState.Denied -> {
                NetworkResult.Error(
                    NetworkError.HttpError(403, "Permission denied: ${state.reason}")
                )
            }
            is GoogleHomeAuthState.Revoked -> {
                NetworkResult.Error(
                    NetworkError.HttpError(403, "Google Home permission revoked")
                )
            }
            else -> {
                NetworkResult.Error(
                    NetworkError.HttpError(401, "Google Home authentication required")
                )
            }
        }
    }
}
