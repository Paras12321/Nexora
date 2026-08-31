package com.nexora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.app.ui.theme.PrimaryTeal

@Composable
fun PresenceSecurityCard(
    presenceState: String,
    securityMode: String,
    onPresenceChange: (String) -> Unit,
    onSecurityModeChange: (String) -> Unit,
    isLoading: Boolean = false,
    isStale: Boolean = false,
    lastPresenceEvent: com.nexora.app.data.model.PresenceEventDto? = null,
    arrivalDetected: Boolean = false
) {
    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smart Presence & Security",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryTeal
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryTeal
                    )
                } else if (isStale) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Stale Data",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presence Section
            PresenceSection(presenceState, onPresenceChange)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Security Section
            SecuritySection(securityMode, onSecurityModeChange)
            
            val lastPresence = lastPresenceEvent
            if (lastPresence != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Last event: ${lastPresence.state.capitalize()} detected via ${lastPresence.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (arrivalDetected) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PrimaryTeal.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Celebration,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Welcome home! Arrival detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (presenceState == "away" && securityMode == "disarmed") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No one is home. Consider arming the system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceSection(
    state: String,
    onStateChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Presence Status",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresenceChip(
                label = "Home",
                isSelected = state == "home",
                icon = Icons.Default.Home,
                onClick = { onStateChange("home") },
                modifier = Modifier.weight(1f)
            )
            PresenceChip(
                label = "Away",
                isSelected = state == "away",
                icon = Icons.Default.DirectionsRun,
                onClick = { onStateChange("away") },
                modifier = Modifier.weight(1f)
            )
            PresenceChip(
                label = "Unknown",
                isSelected = state == "unknown",
                icon = Icons.Default.Help,
                onClick = { onStateChange("unknown") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SecuritySection(
    mode: String,
    onModeChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Security Mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecurityButton(
                label = "Disarm",
                isSelected = mode == "disarmed",
                icon = Icons.Default.LockOpen,
                color = Color(0xFF4CAF50), // Green
                onClick = { onModeChange("disarmed") },
                modifier = Modifier.weight(1f)
            )
            SecurityButton(
                label = "Home",
                isSelected = mode == "armed_home",
                icon = Icons.Default.Security,
                color = Color(0xFFFF9800), // Orange
                onClick = { onModeChange("armed_home") },
                modifier = Modifier.weight(1f)
            )
            SecurityButton(
                label = "Away",
                isSelected = mode == "armed_away",
                icon = Icons.Default.Lock,
                color = Color(0xFFF44336), // Red
                onClick = { onModeChange("armed_away") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresenceChip(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryTeal.copy(alpha = 0.2f),
            selectedLabelColor = PrimaryTeal,
            selectedLeadingIconColor = PrimaryTeal
        )
    )
}

@Composable
private fun SecurityButton(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) color else MaterialTheme.colorScheme.outline)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
