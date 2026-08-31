package com.nexora.app

import android.app.Application
import com.nexora.app.data.local.EncryptedTokenManager
import com.nexora.app.data.local.TokenManager
import com.nexora.app.data.remote.ApiClient
import com.nexora.app.data.remote.AuthApiService
import com.nexora.app.data.repository.AuthRepository
import com.nexora.app.data.repository.AuthRepositoryImpl

class NexoraApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        val tokenManager: TokenManager = EncryptedTokenManager(this)
        val authApiService = ApiClient.createService<AuthApiService>(tokenManager = tokenManager)
        
        authRepository = AuthRepositoryImpl(authApiService, tokenManager)
    }
}
