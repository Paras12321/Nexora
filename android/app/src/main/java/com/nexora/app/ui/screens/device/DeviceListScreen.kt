package com.nexora.app.ui.screens.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexora.app.domain.model.DeviceModel
import com.nexora.app.domain.model.DeviceStatus
import com.nexora.app.ui.components.NexoraCard
import com.nexora.app.ui.theme.TextPrimary
import com.nexora.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceViewModel,
    onDeviceClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devices") },
                actions = {
                    IconButton(onClick = { viewModel.fetchDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            if (uiState.isLoading && uiState.devices.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null && uiState.devices.isEmpty()) {
                Text(
                    text = uiState.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (uiState.devices.isEmpty()) {
                Text(
                    text = "No devices found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val groupedDevices = uiState.devices.groupBy { it.room }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedDevices.forEach { (room, devices) ->
                        item {
                            Text(
                                text = room,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(devices) { device ->
                            DeviceItem(
                                device = device,
                                onClick = { onDeviceClick(device.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: DeviceModel,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(status = device.status)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = device.type.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = getDeviceIcon(device.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatusIndicator(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.Online -> Color.Green
        DeviceStatus.Offline -> Color.Red
        DeviceStatus.Unknown -> Color.Gray
    }
    Surface(
        modifier = Modifier.size(12.dp),
        shape = CircleShape,
        color = color
    ) {}
}

fun getDeviceIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "light" -> Icons.Default.Lightbulb
        "lock" -> Icons.Default.Lock
        else -> Icons.Default.DeviceUnknown
    }
}
