package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.NetworkViewModel
import com.example.ui.theme.ColorSuccess
import com.example.ui.theme.ColorWarning
import com.example.ui.theme.ColorError
import com.example.ui.theme.ColorInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDevices: () -> Unit
) {
    val wifiState by viewModel.wifiState.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Hero Connection Banner Card
            item {
                HeroConnectionBanner(wifiState = wifiState)
            }

            // Quick Network Health Stats Ribbon
            item {
                NetworkHealthRibbon(wifiState = wifiState)
            }

            // Central IP and Gateway Metrics (Expandable/Expressive Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "My Local IP",
                        value = wifiState.ipAddress,
                        icon = Icons.Outlined.Devices,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.copyIpAddress() }
                    )
                    MetricCard(
                        title = "Local Gateway",
                        value = wifiState.gateway,
                        icon = Icons.Outlined.Router,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${wifiState.gateway}"))
                            try { context.startActivity(intent) } catch (e: Exception) {
                                viewModel.triggerNotification("No browser available to open router page")
                            }
                        }
                    )
                }
            }

            // WiFi Connection Hardware Specs card
            item {
                WifiSpecsCard(wifiState = wifiState)
            }

            // Devices Connected Tracker Card
            item {
                ConnectedDevicesCountCard(
                    deviceCount = wifiState.connectedDevicesCount,
                    onNavigateToDevices = onNavigateToDevices
                )
            }

            // Speed Test Launcher CTA Banner
            item {
                SpeedTestLauncherBanner(
                    onRunSpeedTest = { viewModel.runSpeedTest() }
                )
            }

            // Quick Actions Hub
            item {
                QuickActionsHub(
                    onRefresh = { viewModel.refreshNetworkState() },
                    onCopyIp = { viewModel.copyIpAddress() },
                    onShareReport = { viewModel.shareNetworkReport() },
                    onOpenRouter = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${wifiState.gateway}"))
                        try { context.startActivity(intent) } catch (e: Exception) {
                            viewModel.triggerNotification("Failed to open router page")
                        }
                    }
                )
            }
        }

        // Animated Full-Screen Speed Test overlay
        AnimatedVisibility(
            visible = speedTestState.phase != SpeedTestPhase.IDLE,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(350)
            )
        ) {
            SpeedTestOverlay(
                state = speedTestState,
                onCancel = { viewModel.resetSpeedTest() }
            )
        }
    }
}

@Composable
fun HeroConnectionBanner(wifiState: WifiConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "wifi_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsating Connection Status Radar Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (wifiState.status == WifiStatus.CONNECTED) pulseScale else 1.0f)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (wifiState.status == WifiStatus.CONNECTED) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = "WiFi Signal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic SSID Title
            Text(
                text = wifiState.ssid,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Connected Status Badge
            val badgeColor = when (wifiState.status) {
                WifiStatus.CONNECTED -> ColorSuccess
                WifiStatus.LIMITED -> ColorWarning
                WifiStatus.DISCONNECTED -> ColorError
            }
            val badgeText = when (wifiState.status) {
                WifiStatus.CONNECTED -> "Connected"
                WifiStatus.LIMITED -> "Limited Access"
                WifiStatus.DISCONNECTED -> "Disconnected"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(badgeColor, CircleShape)
                )
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (MaterialTheme.colorScheme.primary == Color.White) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NetworkHealthRibbon(wifiState: WifiConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal Strength
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.SignalWifi4Bar, contentDescription = "Signal", tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Signal Strength", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${wifiState.signalStrengthLabel} (${wifiState.signalPercentage}%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Live Ping / Latency
            val latencyColor = when {
                wifiState.latencyMs <= 25 -> ColorSuccess
                wifiState.latencyMs <= 60 -> ColorWarning
                else -> ColorError
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Speed, contentDescription = "Latency", tint = latencyColor)
                Column {
                    Text("Internet Latency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${wifiState.latencyMs} ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = latencyColor)
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun WifiSpecsCard(wifiState: WifiConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Hardware Specifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecRowItem(icon = Icons.Outlined.WifiTethering, title = "Frequency Channel", value = "${wifiState.frequencyGhz} GHz Band")
                SpecRowItem(icon = Icons.Outlined.TrendingUp, title = "Link Speed Cap", value = "${wifiState.linkSpeedMbps} Mbps")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecRowItem(icon = Icons.Outlined.WifiLock, title = "Security protocol", value = "WPA3 Personal")
                SpecRowItem(icon = Icons.Outlined.Podcasts, title = "Signal RSSI Level", value = "${wifiState.rssi} dBm")
            }
        }
    }
}

@Composable
fun SpecRowItem(icon: ImageVector, title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.width(150.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConnectedDevicesCountCard(deviceCount: Int, onNavigateToDevices: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDevices() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "$deviceCount Active Devices",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "Currently connected on your subnet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun SpeedTestLauncherBanner(onRunSpeedTest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Need to check speed?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Run a comprehensive download & upload test with latency checks in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRunSpeedTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Speed Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickActionsHub(
    onRefresh: () -> Unit,
    onCopyIp: () -> Unit,
    onShareReport: () -> Unit,
    onOpenRouter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Quick System Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(icon = Icons.Default.Refresh, label = "Refresh", onClick = onRefresh)
            QuickActionButton(icon = Icons.Default.ContentCopy, label = "Copy IP", onClick = onCopyIp)
            QuickActionButton(icon = Icons.Default.Share, label = "Share Report", onClick = onShareReport)
            QuickActionButton(icon = Icons.Default.SettingsInputHdmi, label = "Router Admin", onClick = onOpenRouter)
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Full Screen / Dialog Style Speed Test Animated Overlay ---
@Composable
fun SpeedTestOverlay(state: SpeedTestState, onCancel: () -> Unit) {
    val progressAnim by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(300, easing = LinearEasing),
        label = "test_progress"
    )

    val dialRotation = when (state.phase) {
        SpeedTestPhase.DOWNLOAD -> (state.downloadSpeed / 600.0 * 240.0).coerceAtMost(240.0).toFloat()
        SpeedTestPhase.UPLOAD -> (state.uploadSpeed / 200.0 * 240.0).coerceAtMost(240.0).toFloat()
        else -> 0f
    }
    val animatedDialRotation by animateFloatAsState(
        targetValue = dialRotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "dial_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Details
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "WIFI SPEED MONITOR",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (state.phase) {
                            SpeedTestPhase.PING_JITTER -> "Analyzing Signal Response & Jitter..."
                            SpeedTestPhase.DOWNLOAD -> "Testing Download Performance..."
                            SpeedTestPhase.UPLOAD -> "Testing Upload Bandwidth..."
                            SpeedTestPhase.FINISHED -> "Speed Evaluation Finished"
                            else -> "Initializing Speed Check"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                // Interactive Radial Canvas Speedometer
                Box(
                    modifier = Modifier
                        .size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasSize = size
                        val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                        val radius = canvasSize.width / 2 - 20.dp.toPx()

                        // Draw background track arc
                        drawArc(
                            color = surfaceVariantColor,
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        // Draw progress speed arc
                        drawArc(
                            color = primaryColor,
                            startAngle = 150f,
                            sweepAngle = animatedDialRotation,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        // Draw gauge dial markers
                        val divisions = 10
                        for (i in 0..divisions) {
                            val angle = 150f + (240f / divisions) * i
                            val angleRad = angle * PI / 180
                            val startOffset = Offset(
                                center.x + (radius - 12.dp.toPx()) * cos(angleRad).toFloat(),
                                center.y + (radius - 12.dp.toPx()) * sin(angleRad).toFloat()
                            )
                            val endOffset = Offset(
                                center.x + radius * cos(angleRad).toFloat(),
                                center.y + radius * sin(angleRad).toFloat()
                            )
                            drawLine(
                                color = if (angle - 150f <= animatedDialRotation) primaryColor else primaryColor.copy(alpha = 0.25f),
                                start = startOffset,
                                end = endOffset,
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Draw indicator needle pointing
                        val needleAngleRad = (150f + animatedDialRotation) * PI / 180
                        val needleEnd = Offset(
                            center.x + (radius - 24.dp.toPx()) * cos(needleAngleRad).toFloat(),
                            center.y + (radius - 24.dp.toPx()) * sin(needleAngleRad).toFloat()
                        )
                        drawLine(
                            color = primaryColor,
                            start = center,
                            end = needleEnd,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center circle knob
                        drawCircle(
                            color = primaryColor,
                            radius = 12.dp.toPx(),
                            center = center
                        )
                    }

                    // Live digital numbers in center of speedometer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Text(
                            text = when (state.phase) {
                                SpeedTestPhase.DOWNLOAD -> "${state.downloadSpeed}"
                                SpeedTestPhase.UPLOAD -> "${state.uploadSpeed}"
                                else -> "0.0"
                            },
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Mbps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Numerical Speed Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SpeedStatDisplay(
                        label = "DOWNLOAD",
                        value = "${state.downloadSpeed} Mbps",
                        icon = Icons.Default.ArrowDownward,
                        isActive = state.phase == SpeedTestPhase.DOWNLOAD
                    )
                    SpeedStatDisplay(
                        label = "UPLOAD",
                        value = "${state.uploadSpeed} Mbps",
                        icon = Icons.Default.ArrowUpward,
                        isActive = state.phase == SpeedTestPhase.UPLOAD
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SpeedStatDisplay(
                        label = "PING",
                        value = "${state.ping} ms",
                        icon = Icons.Default.Timelapse,
                        isActive = state.phase == SpeedTestPhase.PING_JITTER
                    )
                    SpeedStatDisplay(
                        label = "JITTER",
                        value = "${state.jitter} ms",
                        icon = Icons.Default.Analytics,
                        isActive = state.phase == SpeedTestPhase.PING_JITTER
                    )
                }

                // Horizontal overall Linear progress indicator
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressAnim },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(progressAnim * 100).toInt()}% Analysed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedStatDisplay(
    label: String,
    value: String,
    icon: ImageVector,
    isActive: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pulse_stat"
    )

    Card(
        modifier = Modifier
            .width(130.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


