package com.nexora.app.data.repository

import com.nexora.app.data.googlehome.GoogleHomeAuthManager
import com.nexora.app.data.googlehome.GoogleHomeClientManager
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.domain.model.DeviceCapability
import com.nexora.app.domain.model.DeviceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleHomeDeviceRepositoryTest {

    private lateinit var authManager: GoogleHomeAuthManager
    private lateinit var clientManager: GoogleHomeClientManager
    private lateinit var repository: GoogleHomeDeviceRepository

    @Before
    fun setUp() {
        authManager = GoogleHomeAuthManager()
        clientManager = GoogleHomeClientManager(authManager)
        repository = GoogleHomeDeviceRepository(authManager, clientManager)
    }

    @Test
    fun testGetDevicesPermissionGrantedEmitsMappedDevices() = runTest {
        authManager.grantPermissionDirectly()

        val result = repository.getDevices().first()
        assertTrue(result is NetworkResult.Success)

        val devices = (result as NetworkResult.Success).data
        assertEquals(5, devices.size)

        val light = devices.find { it.id == "google-device-1-main-light" }
        assertEquals("Main Light", light?.name)
        assertEquals("Living Room", light?.room)
        assertEquals("light", light?.type)
        assertEquals(DeviceStatus.Online, light?.status)
        assertTrue(light?.capabilities?.contains(DeviceCapability.Power) == true)
        assertTrue(light?.capabilities?.contains(DeviceCapability.Brightness) == true)
    }

    @Test
    fun testGetDevicesPermissionDeniedEmitsError() = runTest {
        authManager.denyPermissionDirectly("User refused consent")

        val result = repository.getDevices().first()
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun testGetDevicesPermissionRevokedEmitsError() = runTest {
        authManager.grantPermissionDirectly()
        repository.getDevices().first() // verify active
        
        authManager.revokePermissions()
        val result = repository.getDevices().first()
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun testGetDevicesEmptyHomeEmitsEmptyList() = runTest {
        authManager.grantPermissionDirectly()
        clientManager.setEmptyHome()

        val result = repository.getDevices().first()
        assertTrue(result is NetworkResult.Success)
        val devices = (result as NetworkResult.Success).data
        assertTrue(devices.isEmpty())
    }

    @Test
    fun testExecuteActionWhenPermissionGrantedSuccess() = runTest {
        authManager.grantPermissionDirectly()

        val result = repository.executeAction("google-device-1-main-light", "power", false)
        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun testExecuteActionWhenPermissionDeniedFails() = runTest {
        authManager.denyPermissionDirectly("Consent denied")

        val result = repository.executeAction("google-device-1-main-light", "power", false)
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun testExecuteActionOnOfflineDeviceFails() = runTest {
        authManager.grantPermissionDirectly()

        val result = repository.executeAction("google-device-5-offline-lamp", "power", true)
        assertTrue(result is NetworkResult.Error)
    }
}
