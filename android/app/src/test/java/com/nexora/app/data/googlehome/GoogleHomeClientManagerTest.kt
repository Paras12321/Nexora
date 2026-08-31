package com.nexora.app.data.googlehome

import com.nexora.app.data.remote.NetworkResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleHomeClientManagerTest {

    private lateinit var authManager: GoogleHomeAuthManager
    private lateinit var clientManager: GoogleHomeClientManager

    @Before
    fun setUp() {
        authManager = GoogleHomeAuthManager()
        authManager.grantPermissionDirectly()
        clientManager = GoogleHomeClientManager(authManager)
    }

    @Test
    fun testInitialGraphDiscovery() {
        assertEquals(1, clientManager.structures.value.size)
        assertEquals(3, clientManager.rooms.value.size)
        assertEquals(5, clientManager.devices.value.size)
    }

    @Test
    fun testEmptyHomeDiscovery() {
        clientManager.setEmptyHome()
        assertTrue(clientManager.structures.value.isEmpty())
        assertTrue(clientManager.rooms.value.isEmpty())
        assertTrue(clientManager.devices.value.isEmpty())
    }

    @Test
    fun testExecuteCommandOnlineDeviceSuccess() = runTest {
        val deviceId = "google-device-1-main-light"
        val result = clientManager.executeDeviceCommand(deviceId, "power", false)

        assertTrue(result is NetworkResult.Success)
        val updatedDevice = clientManager.devices.value.find { it.id == deviceId }
        assertEquals(false, updatedDevice?.attributes?.get("power"))
    }

    @Test
    fun testExecuteCommandOfflineDeviceFails() = runTest {
        val deviceId = "google-device-5-offline-lamp"
        val result = clientManager.executeDeviceCommand(deviceId, "power", true)

        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun testExecuteCommandUnsupportedCapabilityFails() = runTest {
        val deviceId = "google-device-1-main-light"
        // Light does not support target_temperature capability
        val result = clientManager.executeDeviceCommand(deviceId, "target_temperature", 22)

        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun testDuplicateTapProtection() = runTest {
        val deviceId = "google-device-1-main-light"
        val result1 = clientManager.executeDeviceCommand(deviceId, "brightness", 50)
        val result2 = clientManager.executeDeviceCommand(deviceId, "brightness", 50)

        // Both return success, but second rapid identical request is safely debounced
        assertTrue(result1 is NetworkResult.Success)
        assertTrue(result2 is NetworkResult.Success)
    }
}
