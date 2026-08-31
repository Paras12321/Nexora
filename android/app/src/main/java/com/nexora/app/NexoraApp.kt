package com.nexora.app

import android.app.Application
import com.nexora.app.data.local.EncryptedTokenManager
import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.remote.HomeApiService
import com.nexora.app.data.remote.RoomApiService
import com.nexora.app.data.repository.AuthRepository
import com.nexora.app.data.repository.AuthRepositoryImpl
import com.nexora.app.data.repository.DeviceRepository
import com.nexora.app.data.repository.DeviceRepositoryMock
import com.nexora.app.data.repository.HomeRepository
import com.nexora.app.data.repository.HomeRepositoryImpl
import com.nexora.app.data.repository.RoomRepository
import com.nexora.app.data.repository.RoomRepositoryImpl
import com.nexora.app.data.remote.PresenceApiService
import com.nexora.app.data.remote.SecurityApiService
import com.nexora.app.data.repository.PresenceSecurityRepository
import com.nexora.app.data.repository.PresenceSecurityRepositoryImpl
import com.nexora.app.data.remote.EnergyApiService
import com.nexora.app.data.repository.EnergyRepository
import com.nexora.app.data.repository.EnergyRepositoryImpl

import com.nexora.app.data.googlehome.GoogleHomeAuthManager
import com.nexora.app.data.googlehome.GoogleHomeClientManager
import com.nexora.app.data.googlehome.GoogleHomeConfig
import com.nexora.app.data.repository.GoogleHomeDeviceRepository

import com.nexora.app.data.remote.AiApiService
import com.nexora.app.data.repository.AiRepository
import com.nexora.app.data.repository.AiRepositoryImpl

class NexoraApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var homeRepository: HomeRepository
        private set

    lateinit var roomRepository: RoomRepository
        private set

    lateinit var googleHomeConfig: GoogleHomeConfig
        private set

    lateinit var googleHomeAuthManager: GoogleHomeAuthManager
        private set

    lateinit var googleHomeClientManager: GoogleHomeClientManager
        private set

    lateinit var deviceRepository: DeviceRepository
        private set

    lateinit var presenceSecurityRepository: PresenceSecurityRepository
        private set

    lateinit var energyRepository: EnergyRepository
        private set

    lateinit var aiRepository: AiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        tokenManager = EncryptedTokenManager(this)
        
        val authApiService = ApiClient.createService<AuthApiService>(tokenManager = tokenManager)
        authRepository = AuthRepositoryImpl(authApiService, tokenManager)

        val homeApiService = ApiClient.createService<HomeApiService>(tokenManager = tokenManager)
        homeRepository = HomeRepositoryImpl(homeApiService)

        val roomApiService = ApiClient.createService<RoomApiService>(tokenManager = tokenManager)
        roomRepository = RoomRepositoryImpl(roomApiService)

        googleHomeConfig = GoogleHomeConfig()
        googleHomeAuthManager = GoogleHomeAuthManager(googleHomeConfig, tokenManager)
        googleHomeAuthManager.grantPermissionDirectly()
        googleHomeClientManager = GoogleHomeClientManager(googleHomeAuthManager)
        
        deviceRepository = GoogleHomeDeviceRepository(googleHomeAuthManager, googleHomeClientManager)

        val presenceApiService = ApiClient.createService<PresenceApiService>(tokenManager = tokenManager)
        val securityApiService = ApiClient.createService<SecurityApiService>(tokenManager = tokenManager)
        presenceSecurityRepository = PresenceSecurityRepositoryImpl(presenceApiService, securityApiService)

        val energyApiService = ApiClient.createService<EnergyApiService>(tokenManager = tokenManager)
        energyRepository = EnergyRepositoryImpl(energyApiService)

        val aiApiService = ApiClient.createService<AiApiService>(tokenManager = tokenManager)
        aiRepository = AiRepositoryImpl(aiApiService)
    }
}
