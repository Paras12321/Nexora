package com.nexora.app.ui.screens.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.app.domain.model.HomeMemberModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    viewModel: RoomDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAssignMemberDialog by remember { mutableStateOf(false) }
    var showEditPrefDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.room?.name ?: "Room Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.room == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val room = uiState.room
            if (room == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Room details not found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Room Information Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                if (room.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = room.description,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // Preferences Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Room Preferences",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    IconButton(onClick = { showEditPrefDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Preferences")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val prefsMap = uiState.preference?.preferencesMap ?: emptyMap()
                                if (prefsMap.isEmpty()) {
                                    Text(
                                        text = "No preferences set yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    prefsMap.forEach { (key, value) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = key.replace("_", " ").capitalize(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Assigned Members Header & Card
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Assigned Members (${uiState.assignedMembers.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                            OutlinedButton(onClick = { showAssignMemberDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Assign Member")
                            }
                        }
                    }

                    if (uiState.assignedMembers.isEmpty()) {
                        item {
                            Text(
                                text = "No home members assigned to this room yet.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(uiState.assignedMembers) { assignment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column {
                                        Text(
                                            text = assignment.email.ifBlank { "Member ID: ${assignment.memberId}" },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (assignment.role.isNotBlank()) {
                                            Text(
                                                text = "Role: ${assignment.role}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignMemberDialog) {
        AssignRoomMemberDialog(
            availableMembers = uiState.availableHomeMembers,
            onDismiss = { showAssignMemberDialog = false },
            onConfirm = { memberId ->
                viewModel.assignMember(memberId)
                showAssignMemberDialog = false
            }
        )
    }

    if (showEditPrefDialog) {
        EditPreferencesDialog(
            currentPreferences = uiState.preference?.preferencesMap ?: emptyMap(),
            onDismiss = { showEditPrefDialog = false },
            onConfirm = { newPrefs ->
                viewModel.updatePreferences(newPrefs)
                showEditPrefDialog = false
            }
        )
    }
}

@Composable
fun AssignRoomMemberDialog(
    availableMembers: List<HomeMemberModel>,
    onDismiss: () -> Unit,
    onConfirm: (memberId: Int) -> Unit
) {
    var selectedMember by remember { mutableStateOf<HomeMemberModel?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Home Member to Room") },
        text = {
            Column {
                Text("Select a member:")
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMember?.email ?: "Select Member")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text("${member.email} (${member.role})") },
                                onClick = {
                                    selectedMember = member
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedMember?.let { onConfirm(it.id) }
                },
                enabled = selectedMember != null
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditPreferencesDialog(
    currentPreferences: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    var tempTarget by remember { mutableStateOf(currentPreferences["temperature_target"] ?: "22.0") }
    var lightingMode by remember { mutableStateOf(currentPreferences["lighting_mode"] ?: "auto") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Room Preferences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tempTarget,
                    onValueChange = { tempTarget = it },
                    label = { Text("Target Temperature (°C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lightingMode,
                    onValueChange = { lightingMode = it },
                    label = { Text("Lighting Mode (e.g. auto, warm, dim)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = currentPreferences.toMutableMap().apply {
                        put("temperature_target", tempTarget.trim())
                        put("lighting_mode", lightingMode.trim())
                    }
                    onConfirm(updated)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
