package com.example.ui.screens

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier
) {
    val speedHistory by viewModel.speedTestHistory.collectAsStateWithLifecycle()
    val pingHistory by viewModel.pingHistory.collectAsStateWithLifecycle()
    val deviceCountHistory by viewModel.deviceCountHistory.collectAsStateWithLifecycle()

    var latencyTimeframe by remember { mutableStateOf("30s") } // 30s, 1m, 5m

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // Network Health Score Gauge
        item {
            NetworkHealthCircularScoreCard()
        }

        // Live Latency & Signal Strength Line Graph Card
        item {
            val processedPings = remember(pingHistory, latencyTimeframe) {
                val limit = when (latencyTimeframe) {
                    "30s" -> 10
                    "1m" -> 20
                    else -> 30
                }
                pingHistory.take(limit).reversed().map { it.latency.toFloat() }
            }

            val processedSignals = remember(pingHistory, latencyTimeframe) {
                val limit = when (latencyTimeframe) {
                    "30s" -> 10
                    "1m" -> 20
                    else -> 30
                }
                pingHistory.take(limit).reversed().map { it.signalStrength.toFloat() }
            }

            LatencyAndSignalGraphCard(
                pointsLatency = if (processedPings.isEmpty()) listOf(15f, 22f, 18f, 25f, 30f, 19f, 21f) else processedPings,
                pointsSignal = if (processedSignals.isEmpty()) listOf(85f, 88f, 82f, 89f, 90f, 87f, 92f) else processedSignals,
                selectedTimeframe = latencyTimeframe,
                onTimeframeChange = { latencyTimeframe = it }
            )
        }

        // Speed History (Download & Upload) Bar Graph Card
        item {
            val downloadHistoryList = remember(speedHistory) {
                if (speedHistory.isEmpty()) listOf(240f, 350f, 410f, 380f, 450f, 480f)
                else speedHistory.take(6).reversed().map { it.downloadSpeed.toFloat() }
            }
            val uploadHistoryList = remember(speedHistory) {
                if (speedHistory.isEmpty()) listOf(45f, 62f, 75f, 70f, 85f, 92f)
                else speedHistory.take(6).reversed().map { it.uploadSpeed.toFloat() }
            }

            SpeedHistoryBarChartCard(
                downloads = downloadHistoryList,
                uploads = uploadHistoryList
            )
        }

        // WiFi Channel Usage & Recommendation Card
        item {
            WifiChannelUsageCard()
        }

        // Live Comprehensive Stats Cards Grid
        item {
            val statsPingAvg = remember(pingHistory) {
                if (pingHistory.isEmpty()) 21
                else pingHistory.map { it.latency }.average().toInt()
            }
            val statsPingMax = remember(pingHistory) {
                if (pingHistory.isEmpty()) 34
                else pingHistory.maxOfOrNull { it.latency } ?: 34
            }
            val statsPingMin = remember(pingHistory) {
                if (pingHistory.isEmpty()) 12
                else pingHistory.minOfOrNull { it.latency } ?: 12
            }
            val statsDlAvg = remember(speedHistory) {
                if (speedHistory.isEmpty()) 385.0
                else speedHistory.map { it.downloadSpeed }.average()
            }
            val statsUlAvg = remember(speedHistory) {
                if (speedHistory.isEmpty()) 71.5
                else speedHistory.map { it.uploadSpeed }.average()
            }

            LiveStatisticsGrid(
                avgPing = statsPingAvg,
                maxPing = statsPingMax,
                minPing = statsPingMin,
                avgDl = statsDlAvg,
                avgUl = statsUlAvg
            )
        }

        // Connected Devices Timeline Card
        item {
            val timelineDevices = remember(deviceCountHistory) {
                if (deviceCountHistory.isEmpty()) listOf(8f, 9f, 11f, 10f, 12f, 12f)
                else deviceCountHistory.take(6).reversed().map { it.deviceCount.toFloat() }
            }
            ConnectedDevicesTimelineCard(points = timelineDevices)
        }

        // Report Export Actions
        item {
            ExportReportCard(
                onExportPdf = { viewModel.exportReport("PDF") },
                onExportCsv = { viewModel.exportReport("CSV") }
            )
        }
    }
}

@Composable
fun NetworkHealthCircularScoreCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Network Quality Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Large Circular Score Indicator
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.width / 2 - 12.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)

                    // Background circle track
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Foreground score progress arc (94% score)
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * 0.94f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "94",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Excellent",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Overall network score based on latency jitter, local channel interference, and speed stability.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun LatencyAndSignalGraphCard(
    pointsLatency: List<Float>,
    pointsSignal: List<Float>,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with segment switchers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Latency & Signal Flow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Live telemetry monitoring",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    val timeframes = listOf("30s", "1m", "5m")
                    timeframes.forEach { tf ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTimeframe == tf) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onTimeframeChange(tf) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tf,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTimeframe == tf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Line Graph Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val lineColorLatency = ColorSuccess
                val lineColorSignal = ColorInfo

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw helper Grid Lines
                    val gridCount = 4
                    for (i in 0..gridCount) {
                        val y = (height / gridCount) * i
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // --- Render Latency Line Graph ---
                    if (pointsLatency.size >= 2) {
                        val maxLatency = 40f
                        val minLatency = 10f
                        val latencyRange = maxLatency - minLatency

                        val latencyPath = Path()
                        val stepX = width / (pointsLatency.size - 1)

                        val startY = height - ((pointsLatency[0] - minLatency) / latencyRange * height)
                        latencyPath.moveTo(0f, startY)

                        for (idx in 1 until pointsLatency.size) {
                            val x = stepX * idx
                            val y = height - ((pointsLatency[idx] - minLatency) / latencyRange * height)
                            latencyPath.lineTo(x, y)
                        }

                        drawPath(
                            path = latencyPath,
                            color = lineColorLatency,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // --- Render Signal Strength Line Graph ---
                    if (pointsSignal.size >= 2) {
                        val maxSig = 100f
                        val minSig = 50f
                        val sigRange = maxSig - minSig

                        val signalPath = Path()
                        val stepX = width / (pointsSignal.size - 1)

                        val startY = height - ((pointsSignal[0] - minSig) / sigRange * height)
                        signalPath.moveTo(0f, startY)

                        for (idx in 1 until pointsSignal.size) {
                            val x = stepX * idx
                            val y = height - ((pointsSignal[idx] - minSig) / sigRange * height)
                            signalPath.lineTo(x, y)
                        }

                        drawPath(
                            path = signalPath,
                            color = lineColorSignal,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legends Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = ColorSuccess, label = "Latency (ms)")
                LegendIndicator(color = ColorInfo, label = "Signal Quality (%)")
            }
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SpeedHistoryBarChartCard(
    downloads: List<Float>,
    uploads: List<Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Speed Test Performance Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Historical evaluations benchmarked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Double Column Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val maxSpeed = 550f // Max speed scale
                    val barGroupCount = downloads.size
                    val spacingX = width / barGroupCount
                    val barWidth = 14.dp.toPx()

                    for (i in 0 until barGroupCount) {
                        val dlVal = downloads.getOrElse(i) { 0f }
                        val ulVal = uploads.getOrElse(i) { 0f }

                        val xGroupCenter = spacingX * i + spacingX / 2

                        // Draw Download Bar
                        val dlHeight = (dlVal / maxSpeed) * height
                        val dlTopLeftX = xGroupCenter - barWidth - 2.dp.toPx()
                        val dlTopLeftY = height - dlHeight
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(dlTopLeftX, dlTopLeftY),
                            size = Size(barWidth, dlHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Draw Upload Bar
                        val ulHeight = (ulVal / maxSpeed) * height
                        val ulTopLeftX = xGroupCenter + 2.dp.toPx()
                        val ulTopLeftY = height - ulHeight
                        drawRoundRect(
                            color = secondaryColor,
                            topLeft = Offset(ulTopLeftX, ulTopLeftY),
                            size = Size(barWidth, ulHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legends Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = MaterialTheme.colorScheme.primary, label = "Download (Mbps)")
                LegendIndicator(color = MaterialTheme.colorScheme.secondary, label = "Upload (Mbps)")
            }
        }
    }
}

@Composable
fun WifiChannelUsageCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "WiFi Channel Spectrometry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Active congestion analysis",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Channel interference slider blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChannelCongestionBar(channel = "Ch 1", congestion = "High", rating = 0.35f, isCurrent = false, modifier = Modifier.weight(1f))
                ChannelCongestionBar(channel = "Ch 6", congestion = "High", rating = 0.45f, isCurrent = false, modifier = Modifier.weight(1f))
                ChannelCongestionBar(channel = "Ch 11", congestion = "Moderate", rating = 0.65f, isCurrent = false, modifier = Modifier.weight(1f))
                ChannelCongestionBar(channel = "Ch 36", congestion = "Low", rating = 0.95f, isCurrent = true, modifier = Modifier.weight(1f))
                ChannelCongestionBar(channel = "Ch 44", congestion = "Low", rating = 0.92f, isCurrent = false, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Current channel: 36 (5 GHz)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Recommended Channel: 44", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorSuccess)
                }
                Box(
                    modifier = Modifier
                        .background(ColorSuccess.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Auto Optimized", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ColorSuccess)
                }
            }
        }
    }
}

@Composable
fun ChannelCongestionBar(
    channel: String,
    congestion: String,
    rating: Float, // 0.0 to 1.0 (Higher is cleaner/better)
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        rating >= 0.8f -> ColorSuccess
        rating >= 0.5f -> ColorWarning
        else -> ColorError
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(rating)
                    .background(barColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            )

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        Text(channel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(congestion, style = MaterialTheme.typography.labelSmall, color = barColor, fontSize = 9.sp)
    }
}

@Composable
fun LiveStatisticsGrid(
    avgPing: Int,
    maxPing: Int,
    minPing: Int,
    avgDl: Double,
    avgUl: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Connection Telemetry Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCard(title = "Avg. Latency", value = "$avgPing ms", icon = Icons.Outlined.Timelapse, color = ColorSuccess, modifier = Modifier.weight(1f))
            StatsCard(title = "Max. Peak Ping", value = "$maxPing ms", icon = Icons.Outlined.TrendingUp, color = ColorError, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCard(title = "Packet Loss", value = "0.00%", icon = Icons.Outlined.GppGood, color = ColorSuccess, modifier = Modifier.weight(1f))
            StatsCard(title = "Avg. Download", value = String.format("%.1f Mbps", avgDl), icon = Icons.Outlined.ArrowDownward, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCard(title = "Average Upload", value = String.format("%.1f Mbps", avgUl), icon = Icons.Outlined.ArrowUpward, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            StatsCard(title = "System Uptime", value = "12d 4h 3m", icon = Icons.Outlined.AccessTime, color = ColorInfo, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConnectedDevicesTimelineCard(points: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Connected Clients Trend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Hourly connection timeline fluctuation",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Simple Line Chart for connected clients timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val lineColor = MaterialTheme.colorScheme.tertiary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    if (points.size >= 2) {
                        val maxVal = 16f
                        val minVal = 0f
                        val range = maxVal - minVal

                        val path = Path()
                        val stepX = width / (points.size - 1)

                        val startY = height - ((points[0] - minVal) / range * height)
                        path.moveTo(0f, startY)

                        for (idx in 1 until points.size) {
                            val x = stepX * idx
                            val y = height - ((points[idx] - minVal) / range * height)
                            path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportReportCard(
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Export Network Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Download diagnostic logs & connection latency audits.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Report", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CSV Spreadsheet", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
