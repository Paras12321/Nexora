package com.nexora.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexora.app.NexoraApp
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.HomeModel
import com.nexora.app.domain.model.RoomModel
import com.nexora.app.ui.components.PresenceSecurityCard
import com.nexora.app.ui.screens.device.DeviceItem
import com.nexora.app.ui.screens.device.DeviceViewModel
import com.nexora.app.ui.screens.device.DeviceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRoomDetail: (homeId: Int, roomId: Int) -> Unit,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToEnergy: (homeId: Int) -> Unit,
    onNavigateToAiInsights: (homeId: Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val context = LocalContext.current
    val app = context.applicationContext as NexoraApp
    
    val deviceViewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModelFactory(app.deviceRepository)
    )

    var showCreateHomeDialog by remember { mutableStateOf(false) }
    var showJoinHomeDialog by remember { mutableStateOf(false) }
    var showInviteMemberDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage, uiState.userFeedbackMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
        uiState.userFeedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isDropdownExpanded = true }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.selectedHome?.name ?: "Select / Create Home",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            uiState.homes.forEach { home ->
                                DropdownMenuItem(
                                    text = { Text(home.name) },
                                    onClick = {
                                        viewModel.selectHome(home)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create New Home", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    isDropdownExpanded = false
                                    showCreateHomeDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("→ Join Home with Code", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    isDropdownExpanded = false
                                    showJoinHomeDialog = true
                                }
                            )
                        }
                    }
                },
                actions = {
                    uiState.selectedHome?.let { home ->
                        IconButton(onClick = { onNavigateToAiInsights(home.id) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Insights")
                        }
                        IconButton(onClick = { onNavigateToEnergy(home.id) }) {
                            Icon(Icons.Default.Bolt, contentDescription = "Energy & Billing")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedHome != null) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTabIndex == 0) {
                            showCreateRoomDialog = true
                        } else {
                            showInviteMemberDialog = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (selectedTabIndex == 0) "Add Room" else "Invite Member"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }

            if (uiState.selectedHome == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Home Selected",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showCreateHomeDialog = true }) {
                            Text("Create a Home")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showJoinHomeDialog = true }) {
                            Text("Join a Home with Code")
                        }
                    }
                }
            } else {
                val selectedHome = uiState.selectedHome!!
                
                // Presence & Security Section (Task A4)
                val presenceViewModel: PresenceSecurityViewModel = viewModel(
                    key = "presence_${selectedHome.id}",
                    factory = PresenceSecurityViewModelFactory(selectedHome.id, app.presenceSecurityRepository)
                )
                val presenceState by presenceViewModel.uiState.collectAsState()
                
                PresenceSecurityCard(
                    presenceState = presenceState.presenceState,
                    securityMode = presenceState.securityMode,
                    onPresenceChange = { presenceViewModel.updatePresence(it) },
                    onSecurityModeChange = { presenceViewModel.changeSecurityMode(it) },
                    isLoading = presenceState.isLoading,
                    isStale = presenceState.isStale,
                    lastPresenceEvent = presenceState.lastPresenceEvent,
                    arrivalDetected = presenceState.arrivalDetected
                )

                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Rooms") },
                        icon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Devices") },
                        icon = { Icon(Icons.Default.Devices, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Members") },
                        icon = { Icon(Icons.Default.People, contentDescription = null) }
                    )
                }

                when (selectedTabIndex) {
                    0 -> RoomsTabContent(
                        rooms = uiState.rooms,
                        onRoomClick = { room ->
                            onNavigateToRoomDetail(selectedHome.id, room.id)
                        }
                    )
                    1 -> DevicesTabContent(
                        viewModel = deviceViewModel,
                        onDeviceClick = onNavigateToDeviceDetail
                    )
                    2 -> MembersTabContent(
                        members = uiState.members,
                        selectedHome = selectedHome,
                        onRemoveMember = { viewModel.removeMember(it) },
                        onLeaveHome = { viewModel.leaveHome() }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateHomeDialog) {
        CreateHomeDialog(
            onDismiss = { showCreateHomeDialog = false },
            onConfirm = { name ->
                viewModel.createHome(name)
                showCreateHomeDialog = false
            }
        )
    }

    if (showJoinHomeDialog) {
        JoinHomeDialog(
            onDismiss = { showJoinHomeDialog = false },
            onConfirm = { code ->
                viewModel.joinHome(code)
                showJoinHomeDialog = false
            }
        )
    }

    if (showInviteMemberDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteMemberDialog = false },
            onConfirm = { email ->
                viewModel.inviteMember(email)
                showInviteMemberDialog = false
            }
        )
    }

    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onConfirm = { name, desc ->
                viewModel.createRoom(name, desc)
                showCreateRoomDialog = false
            }
        )
    }
}

@Composable
fun RoomsTabContent(
    rooms: List<RoomModel>,
    onRoomClick: (RoomModel) -> Unit
) {
    if (rooms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No rooms in this home yet. Tap '+' to create one.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rooms) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRoomClick(room) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = room.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (room.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = room.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevicesTabContent(
    viewModel: DeviceViewModel,
    onDeviceClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading && uiState.devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No devices found in this home.")
        }
    } else {
        val groupedDevices = uiState.devices.groupBy { it.room }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedDevices.forEach { (room, devices) ->
                item {
                    Column {
                        Text(
                            text = room,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    }
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

@Composable
fun MembersTabContent(
    members: List<HomeMemberModel>,
    selectedHome: HomeModel?,
    onRemoveMember: (Int) -> Unit,
    onLeaveHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Home Members", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onLeaveHome) {
                Text("Leave Home")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(members) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (member.firstName.isNotBlank()) "${member.firstName} ${member.lastName}".trim() else member.email,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Role: ${member.role.uppercase()} • ${member.email}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (member.role.lowercase() != "owner") {
                            TextButton(onClick = { onRemoveMember(member.id) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateHomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var homeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Home") },
        text = {
            OutlinedTextField(
                value = homeName,
                onValueChange = { homeName = it },
                label = { Text("Home Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(homeName) },
                enabled = homeName.isNotBlank()
            ) {
                Text("Create")
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
fun JoinHomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Home") },
        text = {
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("Invite Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inviteCode) },
                enabled = inviteCode.isNotBlank()
            ) {
                Text("Join")
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
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Member to Home") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("User Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank()
            ) {
                Text("Invite")
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
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room Name (e.g. Living Room)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
