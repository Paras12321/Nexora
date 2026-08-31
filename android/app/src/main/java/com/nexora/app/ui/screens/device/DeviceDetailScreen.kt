package com.nexora.app.ui.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.app.domain.model.DeviceCapability
import com.nexora.app.domain.model.DeviceModel
import com.nexora.app.domain.model.DeviceStatus

import com.nexora.app.ui.components.NexoraCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    viewModel: DeviceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val device = uiState.devices.find { it.id == deviceId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "Device Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchDevices() }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (device == null) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Text(
                        text = "Device not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceHeader(device)
                    HorizontalDivider()
                    
                    if (device.status == DeviceStatus.Offline) {
                        Text(
                            text = "Device is offline. Some controls may be unavailable.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    device.capabilities.forEach { capability ->
                        NexoraCard(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                CapabilityControl(
                                    device = device,
                                    capability = capability,
                                    onAction = { value ->
                                        viewModel.executeAction(device.id, getCapabilityKey(capability), value)
                                    },
                                    enabled = device.status == DeviceStatus.Online && !uiState.actionLoading
                                )
                            }
                        }
                    }
                }
                
                if (uiState.actionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceHeader(device: DeviceModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = device.type.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = device.room,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusText = when(device.status) {
                DeviceStatus.Online -> "Online"
                DeviceStatus.Offline -> "Offline"
                DeviceStatus.Unknown -> "Unknown"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            StatusIndicator(status = device.status)
        }
    }
}

@Composable
fun CapabilityControl(
    device: DeviceModel,
    capability: DeviceCapability,
    onAction: (Any) -> Unit,
    enabled: Boolean
) {
    val currentAttributes = device.attributes
    
    when (capability) {
        DeviceCapability.Power -> {
            val isOn = (currentAttributes["power"] as? Boolean) ?: false
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Power", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isOn, onCheckedChange = { onAction(it) }, enabled = enabled)
            }
        }
        DeviceCapability.Brightness -> {
            val brightness = (currentAttributes["brightness"] as? Number)?.toFloat() ?: 0f
            Column {
                Text(text = "Brightness", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = brightness,
                    onValueChange = { onAction(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = enabled
                )
            }
        }
        DeviceCapability.TargetTemperature -> {
            val temp = (currentAttributes["target_temperature"] as? Number)?.toFloat() ?: 20f
            Column {
                Text(text = "Target Temperature: ${temp.toInt()}°C", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = temp,
                    onValueChange = { onAction(it.toInt()) },
                    valueRange = 16f..30f,
                    enabled = enabled
                )
            }
        }
        DeviceCapability.FanSpeed -> {
            val speed = (currentAttributes["fan_speed"] as? Number)?.toFloat() ?: 1f
            Column {
                Text(text = "Fan Speed: ${speed.toInt()}", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = speed,
                    onValueChange = { onAction(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    enabled = enabled
                )
            }
        }
        DeviceCapability.Lock -> {
            val isLocked = (currentAttributes["lock"] as? Boolean) ?: false
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (isLocked) "Locked" else "Unlocked", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isLocked, onCheckedChange = { onAction(it) }, enabled = enabled)
            }
        }
        else -> {
            // Placeholder for other capabilities
            Text(text = "Control for $capability not implemented yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun getCapabilityKey(capability: DeviceCapability): String {
    return when (capability) {
        DeviceCapability.Power -> "power"
        DeviceCapability.Brightness -> "brightness"
        DeviceCapability.ColorTemperature -> "color_temperature"
        DeviceCapability.RGB -> "rgb"
        DeviceCapability.TargetTemperature -> "target_temperature"
        DeviceCapability.FanSpeed -> "fan_speed"
        DeviceCapability.Lock -> "lock"
        is DeviceCapability.Custom -> capability.name
    }
}
