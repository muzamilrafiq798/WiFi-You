package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val refreshInterval by viewModel.refreshIntervalSec.collectAsStateWithLifecycle()
    val animSpeed by viewModel.graphAnimationSpeed.collectAsStateWithLifecycle()
    val unitsMbps by viewModel.unitsMbps.collectAsStateWithLifecycle()
    val notifyNewDev by viewModel.notifyNewDevices.collectAsStateWithLifecycle()
    val notifyDisc by viewModel.notifyDisconnects.collectAsStateWithLifecycle()
    val notifyLatency by viewModel.notifyHighLatency.collectAsStateWithLifecycle()

    var showIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Personalization Header
            item {
                SettingsHeader(title = "Material You UI Design")
            }

            // Dynamic Monet Colors Toggle
            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.Palette,
                    title = "Dynamic Colors (Monet)",
                    subtitle = "Adapts user colors to active system wallpaper",
                    checked = dynamicColor,
                    onCheckedChange = { viewModel.dynamicColorEnabled.value = it }
                )
            }

            // Bandwidth Speed Units Selector
            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.Speed,
                    title = "Measure in Megabits (Mbps)",
                    subtitle = "Disabling measures network speed in Megabytes (MB/s)",
                    checked = unitsMbps,
                    onCheckedChange = { viewModel.unitsMbps.value = it }
                )
            }

            // Network Monitoring Preferences Header
            item {
                SettingsHeader(title = "Network Monitor Specs")
            }

            // Polling Refresh Interval Selector
            item {
                SettingsClickableRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Telemetry Polling Interval",
                    valueLabel = "${refreshInterval}s",
                    onClick = { showIntervalDialog = true }
                )
            }

            // Graph Animation Speed Calibration Slider
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Graph Sweep Duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Telemetry chart redraw rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value = animSpeed.toFloat(),
                            onValueChange = { viewModel.graphAnimationSpeed.value = it.toInt() },
                            valueRange = 100f..1000f,
                            steps = 9
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("100ms (Hyper)", style = MaterialTheme.typography.labelSmall)
                            Text("${animSpeed} ms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("1000ms (Smooth)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Alert Notification Controls Header
            item {
                SettingsHeader(title = "Diagnostics Alerts & Notifications")
            }

            // New Subnet Joins Switch
            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.NotificationAdd,
                    title = "New Subnet Join Alerts",
                    subtitle = "Notify when a new unknown IP joins home WiFi",
                    checked = notifyNewDev,
                    onCheckedChange = { viewModel.notifyNewDevices.value = it }
                )
            }

            // High Latency Alert Switch
            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.WarningAmber,
                    title = "Latency Spikes Warning",
                    subtitle = "Notify if ping latency exceeds critical 100ms",
                    checked = notifyLatency,
                    onCheckedChange = { viewModel.notifyHighLatency.value = it }
                )
            }

            // Wi-Fi Disconnection Alert Switch
            item {
                SettingsToggleRow(
                    icon = Icons.Outlined.WifiOff,
                    title = "Network Disconnection Warnings",
                    subtitle = "Push alerts on local gateway dropoffs",
                    checked = notifyDisc,
                    onCheckedChange = { viewModel.notifyDisconnects.value = it }
                )
            }

            // Software Diagnostics Card
            item {
                DiagnosticsInformationCard()
            }
        }
    }

    // Interval Dialog Selector
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Select Polling Interval") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 10).forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.refreshIntervalSec.value = sec
                                    showIntervalDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${sec} Seconds ${if (sec == 1) "(Real-time)" else ""}", style = MaterialTheme.typography.bodyLarge)
                            RadioButton(selected = refreshInterval == sec, onClick = {
                                viewModel.refreshIntervalSec.value = sec
                                showIntervalDialog = false
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(valueLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DiagnosticsInformationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("System Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("WiFi You Version 1.0.0 Stable (API 36 Build)", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Text("Engine: Coroutines + Room Local Stack. Edge-To-Edge Dynamic Monet rendering.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
