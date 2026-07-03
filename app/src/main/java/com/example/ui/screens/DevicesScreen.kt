package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.NetworkViewModel
import com.example.ui.SortOption
import com.example.ui.theme.ColorSuccess
import com.example.ui.theme.ColorInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.filteredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search and Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stylized Pixel Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by Name, IP, MAC...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            // Sort Filter Toggle Button
            IconButton(
                onClick = { showSortDialog = true },
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort Options",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Horizontal Category Filter Chips Slider
        val filtersList = listOf("All", "Phones", "PCs", "Smart TVs", "Consoles", "Cameras", "Unknown")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(filtersList) { filterLabel ->
                val isSelected = selectedFilter == filterLabel
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(filterLabel) },
                    label = { Text(filterLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = null
                )
            }
        }

        // Active Subnet Scan CTA Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isScanning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(Icons.Default.WifiFind, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(
                            text = if (isScanning) "Scanning local subnet..." else "Continuous Monitoring Active",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanning) "Searching active nodes IP 1..254" else "${devices.size} total active clients found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isScanning) {
                    Button(
                        onClick = { viewModel.startNetworkScan() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Scan Subnet", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Device List view
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = "No Devices Found",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "No Connected Devices Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Try relaxing search query or category filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onCopyIp = { viewModel.copyIpAddress() }
                    )
                }
            }
        }
    }

    // Sort Selection Dialog Window
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Connected Clients") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortDialogOption(
                        label = "Device Host Name",
                        selected = sortOption == SortOption.NAME,
                        onClick = {
                            viewModel.setSortOption(SortOption.NAME)
                            showSortDialog = false
                        }
                    )
                    SortDialogOption(
                        label = "IP Network Address",
                        selected = sortOption == SortOption.IP,
                        onClick = {
                            viewModel.setSortOption(SortOption.IP)
                            showSortDialog = false
                        }
                    )
                    SortDialogOption(
                        label = "Signal Strength Quality",
                        selected = sortOption == SortOption.SIGNAL,
                        onClick = {
                            viewModel.setSortOption(SortOption.SIGNAL)
                            showSortDialog = false
                        }
                    )
                    SortDialogOption(
                        label = "Recently Active State",
                        selected = sortOption == SortOption.RECENTLY_ACTIVE,
                        onClick = {
                            viewModel.setSortOption(SortOption.RECENTLY_ACTIVE)
                            showSortDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SortDialogOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun DeviceCard(
    device: NetworkDevice,
    onCopyIp: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "arrow_rot"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Content Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Icon Hex Block
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (device.isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDeviceIcon(device.type),
                            contentDescription = null,
                            tint = if (device.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Metadata Text Rows
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Small green dot for online status
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (device.isOnline) ColorSuccess else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                        }
                        Text(
                            text = "IP: ${device.ipAddress} • ${device.manufacturer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Expand Collapse Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Signal Chip Indicator
                    if (device.connectionType == ConnectionType.WIFI) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${device.signalQuality}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .rotate(arrowRotation)
                            .size(20.dp)
                    )
                }
            }

            // Spring Expandable Details Container
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Deep Specifications Layout Table
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem(label = "Hostname", value = device.hostname, modifier = Modifier.weight(1f))
                        DetailItem(label = "MAC Address", value = device.macAddress, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem(label = "IPv6 Address", value = device.ipv6, modifier = Modifier.weight(1f))
                        DetailItem(label = "Connection Node", value = if (device.connectionType == ConnectionType.WIFI) "WiFi (5GHz AX)" else "Gateway LAN (Core)", modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem(label = "DNS Servers", value = device.dns, modifier = Modifier.weight(1f))
                        DetailItem(label = "Device Model", value = device.deviceModel, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem(label = "Bandwidth Usage (Down)", value = String.format("%.1f MB", device.dataUsageDownMb), modifier = Modifier.weight(1f))
                        DetailItem(label = "Bandwidth Usage (Up)", value = String.format("%.1f MB", device.dataUsageUpMb), modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem(label = "Subnet Gateway", value = "192.168.1.1", modifier = Modifier.weight(1f))
                        DetailItem(label = "Est. Latency / Ping", value = if (device.isOnline) "${device.estimatedLatencyMs} ms" else "Offline", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dynamic Quick action on the device card itself
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCopyIp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy IP Address", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun getDeviceIcon(type: DeviceType): ImageVector {
    return when (type) {
        DeviceType.PHONE -> Icons.Outlined.PhoneAndroid
        DeviceType.LAPTOP -> Icons.Outlined.LaptopMac
        DeviceType.TV -> Icons.Outlined.Tv
        DeviceType.CONSOLE -> Icons.Outlined.SportsEsports
        DeviceType.ROUTER -> Icons.Outlined.Router
        DeviceType.PRINTER -> Icons.Outlined.Print
        DeviceType.CAMERA -> Icons.Outlined.PhotoCamera
        DeviceType.SMART_HOME -> Icons.Outlined.Home
        DeviceType.UNKNOWN -> Icons.Outlined.HelpOutline
    }
}
