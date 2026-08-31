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
import com.nexora.app.data.repository.HomeRepository
import com.nexora.app.data.repository.HomeRepositoryImpl
import com.nexora.app.data.repository.RoomRepository
import com.nexora.app.data.repository.RoomRepositoryImpl

class NexoraApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var homeRepository: HomeRepository
        private set

    lateinit var roomRepository: RoomRepository
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
    }
}
