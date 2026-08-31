package com.nexora.app.ui.screens.energy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.app.data.model.BillDto
import com.nexora.app.ui.components.NexoraButton
import com.nexora.app.ui.components.NexoraCard
import com.nexora.app.ui.components.NexoraTextField
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(
    viewModel: EnergyViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSubmitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy & Billing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSubmitDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Submit Bill")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    EnergySummarySection(
                        totalKwh = uiState.usage.sumOf { it.usageKwh },
                        latestBillAmount = uiState.bills.firstOrNull()?.amount ?: 0.0
                    )
                }

                item {
                    AiAnalysisSection(uiState.analysis?.content)
                }

                item {
                    UsageContributorsSection()
                }

                item {
                    Text("Bill History", style = MaterialTheme.typography.headlineSmall)
                }

                if (uiState.bills.isEmpty()) {
                    item {
                        Text("No bills submitted yet.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    items(uiState.bills) { bill ->
                        BillHistoryItem(bill)
                    }
                }
            }
        }
    }

    if (showSubmitDialog) {
        SubmitBillDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showSubmitDialog = false },
            onSubmit = { amount, usage, start, end ->
                viewModel.submitBill(amount, usage, start, end)
                if (uiState.submitSuccess) showSubmitDialog = false
            }
        )
    }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            showSubmitDialog = false
            viewModel.clearFeedback()
        }
    }
}

@Composable
fun EnergySummarySection(totalKwh: Double, latestBillAmount: Double) {
    Column {
        Text("Usage Summary", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Usage",
                value = String.format("%.1f kWh", totalKwh),
                icon = Icons.Default.Bolt
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Latest Bill",
                value = String.format("$%.2f", latestBillAmount),
                icon = Icons.Default.History
            )
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier, title: String, value: String, icon: ImageVector) {
    NexoraCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AiAnalysisSection(analysis: String?) {
    NexoraCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("AI Energy Insights", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = analysis ?: "Submit a bill to get personalized energy insights and optimization recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun UsageContributorsSection() {
    Column {
        Text("Usage Contributors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ContributorItem("HVAC System", "42%", 0.42f)
            ContributorItem("Kitchen Appliances", "28%", 0.28f)
            ContributorItem("Lighting", "15%", 0.15f)
            ContributorItem("Others", "15%", 0.15f)
        }
    }
}

@Composable
fun ContributorItem(name: String, percentage: String, progress: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodySmall)
            Text(percentage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun BillHistoryItem(bill: BillDto) {
    NexoraCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${bill.periodStart} - ${bill.periodEnd}", style = MaterialTheme.typography.bodySmall)
                Text(String.format("%.0f kWh", bill.usageKwh), style = MaterialTheme.typography.bodyLarge)
            }
            Text(String.format("$%.2f", bill.amount), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubmitBillDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double, Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var usage by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Electricity Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NexoraTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Bill Amount ($)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                NexoraTextField(
                    value = usage,
                    onValueChange = { usage = it },
                    label = "Energy Usage (kWh)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                NexoraTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Period Start (YYYY-MM-DD)"
                )
                NexoraTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "Period End (YYYY-MM-DD)"
                )
            }
        },
        confirmButton = {
            NexoraButton(
                text = "Submit",
                isLoading = isSubmitting,
                modifier = Modifier.width(100.dp),
                onClick = {
                    val a = amount.toDoubleOrNull()
                    val u = usage.toDoubleOrNull()
                    if (a != null && u != null && startDate.isNotBlank() && endDate.isNotBlank()) {
                        onSubmit(a, u, startDate, endDate)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
