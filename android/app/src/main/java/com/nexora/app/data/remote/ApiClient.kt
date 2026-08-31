package com.nexora.app.data.remote

import com.nexora.app.BuildConfig
import com.nexora.app.data.local.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory and provider for configured OkHttpClient and Retrofit API instances.
 */
object ApiClient {

    private const val DEFAULT_TIMEOUT_SECONDS = 30L

    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Builds an OkHttpClient configured with authentication and logging interceptors.
     */
    fun createOkHttpClient(
        tokenManager: TokenManager? = null,
        sessionRepository: com.nexora.app.data.session.SessionRepository? = null,
        enableLogging: Boolean = BuildConfig.DEBUG
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (tokenManager != null && sessionRepository != null) {
            builder.addInterceptor(AuthInterceptor(tokenManager, sessionRepository))
        }

        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Creates a Retrofit instance using [baseUrl], [okHttpClient], and kotlinx.serialization converter.
     */
    fun createRetrofit(
        baseUrl: String = BuildConfig.BASE_URL,
        okHttpClient: OkHttpClient = createOkHttpClient(),
        json: Json = defaultJson
    ): Retrofit {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /**
     * Helper to instantiate a Retrofit service interface of type [T].
     */
    inline fun <reified T> createService(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenManager: TokenManager? = null,
        sessionRepository: com.nexora.app.data.session.SessionRepository? = null
    ): T {
        val okHttpClient = createOkHttpClient(tokenManager, sessionRepository)
        val retrofit = createRetrofit(baseUrl, okHttpClient)
        return retrofit.create(T::class.java)
    }
}
