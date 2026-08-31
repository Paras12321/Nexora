package com.nexora.app.data.googlehome

import com.nexora.app.data.remote.NetworkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleHomeAuthManagerTest {

    private lateinit var authManager: GoogleHomeAuthManager

    @Before
    fun setUp() {
        val config = GoogleHomeConfig(
            projectId = "test-project",
            clientId = "test-client-id",
            redirectScheme = "com.nexora.app"
        )
        authManager = GoogleHomeAuthManager(config = config)
    }

    @Test
    fun testBuildAuthorizationUrl() {
        val url = authManager.buildAuthorizationUrl()
        assertTrue(url.contains("client_id=test-client-id"))
        assertTrue(url.contains("redirect_uri=com.nexora.app%3A%2F%2Foauth2redirect"))
        assertTrue(authManager.authState.value is GoogleHomeAuthState.Authorizing)
    }

    @Test
    fun testHandleRedirectUriGranted() {
        val redirectUri = "com.nexora.app://oauth2redirect?code=sample_auth_code_12345"
        val result = authManager.handleRedirectUri(redirectUri)

        assertTrue(result is NetworkResult.Success)
        val currentState = authManager.authState.value
        assertTrue(currentState is GoogleHomeAuthState.Granted)
        assertEquals("gh_access_sample_auth_c", (currentState as GoogleHomeAuthState.Granted).accessToken)
    }

    @Test
    fun testHandleRedirectUriDenied() {
        val redirectUri = "com.nexora.app://oauth2redirect?error=access_denied"
        val result = authManager.handleRedirectUri(redirectUri)

        assertTrue(result is NetworkResult.Error)
        val currentState = authManager.authState.value
        assertTrue(currentState is GoogleHomeAuthState.Denied)
        assertFalse(authManager.isGranted())
    }

    @Test
    fun testRevokePermissions() {
        authManager.grantPermissionDirectly()
        assertTrue(authManager.isGranted())

        authManager.revokePermissions()
        assertTrue(authManager.authState.value is GoogleHomeAuthState.Revoked)
        assertFalse(authManager.isGranted())
    }
}
