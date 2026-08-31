package com.nexora.app.data.googlehome

import com.nexora.app.BuildConfig

data class GoogleHomeConfig(
    val projectId: String = BuildConfig.GOOGLE_HOME_PROJECT_ID,
    val clientId: String = BuildConfig.GOOGLE_OAUTH_CLIENT_ID,
    val redirectScheme: String = BuildConfig.GOOGLE_OAUTH_REDIRECT_SCHEME,
    val redirectHost: String = "oauth2redirect",
    val scopes: List<String> = listOf(
        "https://www.googleapis.com/auth/homegraph",
        "https://www.googleapis.com/auth/sdm.service"
    )
) {
    val redirectUri: String
        get() = "$redirectScheme://$redirectHost"

    fun isConfigured(): Boolean {
        return projectId.isNotBlank() && clientId.isNotBlank()
    }
}
