package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ColorError

enum class AppTab {
    HOME,
    DEVICES,
    ANALYTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var inSettingsScreen by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    // Slide and fade animations for entering/leaving full settings screen
    AnimatedContent(
        targetState = inSettingsScreen,
        transitionSpec = {
            if (targetState) {
                // Enter Settings
                (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
            } else {
                // Exit Settings
                (slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
            }
        },
        label = "settings_transition"
    ) { showSettings ->
        if (showSettings) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { inSettingsScreen = false }
            )
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "WiFi You",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            // Alert Notifications Badge Button
                            IconButton(onClick = { showNotificationSheet = true }) {
                                BadgedBox(
                                    badge = {
                                        if (notifications.isNotEmpty()) {
                                            Badge(
                                                containerColor = ColorError,
                                                contentColor = Color.White
                                            ) {
                                                Text("${notifications.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alerts"
                                    )
                                }
                            }

                            // Application Settings Button
                            IconButton(onClick = { inSettingsScreen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.HOME,
                            onClick = { currentTab = AppTab.HOME },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.DEVICES,
                            onClick = { currentTab = AppTab.DEVICES },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.DEVICES) Icons.Filled.Devices else Icons.Outlined.Devices,
                                    contentDescription = "Devices"
                                )
                            },
                            label = { Text("Devices") }
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.ANALYTICS,
                            onClick = { currentTab = AppTab.ANALYTICS },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.ANALYTICS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                    contentDescription = "Analytics"
                                )
                            },
                            label = { Text("Analytics") }
                        )
                    }
                },
                modifier = modifier.fillMaxSize()
            ) { innerPadding ->
                // Smooth transition fade-through between navigation bottom tabs
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(250),
                    modifier = Modifier.padding(innerPadding),
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToDevices = { currentTab = AppTab.DEVICES }
                        )
                        AppTab.DEVICES -> DevicesScreen(
                            viewModel = viewModel
                        )
                        AppTab.ANALYTICS -> AnalyticsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Alerts Notifications Slide Sheet
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Network Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearNotifications() }) {
                            Text("Clear All", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text("No unresolved alerts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(notifications) { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(ColorError, CircleShape)
                                    )
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showNotificationSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close Panel")
                }
            }
        }
    }
}
