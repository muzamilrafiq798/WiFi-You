package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

enum class SortOption {
    NAME,
    IP,
    SIGNAL,
    RECENTLY_ACTIVE
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(database.networkDao())
    private val networkManager = NetworkManager(application)

    // --- Core Connection States ---
    private val _wifiState = MutableStateFlow(WifiConnectionState())
    val wifiState: StateFlow<WifiConnectionState> = _wifiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // --- Latency and Live Stats ---
    private val _currentLatency = MutableStateFlow(20)
    val currentLatency: StateFlow<Int> = _currentLatency.asStateFlow()

    // --- Device Scanning States ---
    private val _devicesList = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val devicesList: StateFlow<List<NetworkDevice>> = _devicesList.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String>("All") // All, Phones, PCs, Smart TVs, Consoles, Cameras, Unknown
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // --- Room Database Historical States ---
    val speedTestHistory: StateFlow<List<SpeedTestRecord>> = repository.allSpeedTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pingHistory: StateFlow<List<PingRecord>> = repository.allPingRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceCountHistory: StateFlow<List<DeviceHistoryRecord>> = repository.allDeviceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Speed Test States ---
    private val _speedTestState = MutableStateFlow(SpeedTestState())
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    // --- Settings / Preferences (StateFlow) ---
    val dynamicColorEnabled = MutableStateFlow(true)
    val refreshIntervalSec = MutableStateFlow(3) // 1s, 3s, 5s, 10s
    val graphAnimationSpeed = MutableStateFlow(300) // ms
    val unitsMbps = MutableStateFlow(true) // true = Mbps, false = MB/s
    val notifyNewDevices = MutableStateFlow(true)
    val notifyDisconnects = MutableStateFlow(true)
    val notifyHighLatency = MutableStateFlow(true)

    // --- Computed Filtered / Sorted Devices ---
    val filteredDevices: StateFlow<List<NetworkDevice>> = combine(
        _devicesList, _searchQuery, _selectedFilter, _sortOption
    ) { devices, query, filter, sort ->
        var list = devices

        // Apply filter
        if (filter != "All") {
            list = list.filter { device ->
                when (filter) {
                    "Phones" -> device.type == DeviceType.PHONE
                    "PCs" -> device.type == DeviceType.LAPTOP
                    "Smart TVs" -> device.type == DeviceType.TV
                    "Consoles" -> device.type == DeviceType.CONSOLE
                    "Cameras" -> device.type == DeviceType.CAMERA
                    "Unknown" -> device.type == DeviceType.UNKNOWN || device.type == DeviceType.PRINTER || device.type == DeviceType.SMART_HOME
                    else -> true
                }
            }
        }

        // Apply search query
        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.ipAddress.contains(query) ||
                        it.macAddress.contains(query, ignoreCase = true) ||
                        it.hostname.contains(query, ignoreCase = true) ||
                        it.manufacturer.contains(query, ignoreCase = true)
            }
        }

        // Apply sort
        list = when (sort) {
            SortOption.NAME -> list.sortedBy { it.name }
            SortOption.IP -> list.sortedBy { it.ipAddress }
            SortOption.SIGNAL -> list.sortedByDescending { it.signalQuality }
            SortOption.RECENTLY_ACTIVE -> list.sortedByDescending { it.isOnline }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification Queue for UI toast/banner simulation
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    init {
        // Load default devices list
        _devicesList.value = networkManager.getFallbackDevices()

        // Start connection state update polling
        viewModelScope.launch {
            while (isActive) {
                updateWifiState()
                delay(refreshIntervalSec.value * 1000L)
            }
        }

        // Start Internet latency measurement stream
        viewModelScope.launch {
            networkManager.observeLatencyStream(1000L).collect { latency ->
                _currentLatency.value = latency
                // Save ping history to room periodically (every 5 seconds) to avoid over-cluttering DB
                if (Random.nextInt(5) == 0) {
                    val state = _wifiState.value
                    repository.insertPingRecord(
                        PingRecord(
                            latency = latency,
                            signalStrength = state.signalPercentage,
                            rssi = state.rssi,
                            ssid = state.ssid
                        )
                    )
                }
                // Notify if high latency occurs
                if (latency > 100 && notifyHighLatency.value) {
                    triggerNotification("High latency detected: ${latency}ms")
                }
            }
        }

        // Pre-fill DB with historical records if DB is completely empty so that the graphs look wonderful right away!
        viewModelScope.launch {
            delay(1000)
            val existingTests = repository.allSpeedTests.firstOrNull() ?: emptyList()
            if (existingTests.isEmpty()) {
                generateDemoHistory()
            }
        }
    }

    private suspend fun updateWifiState() {
        val newState = networkManager.getWifiState()
        // Check for disconnections
        if (newState.status == WifiStatus.DISCONNECTED && _wifiState.value.status == WifiStatus.CONNECTED && notifyDisconnects.value) {
            triggerNotification("WiFi Connection Disconnected")
        }
        _wifiState.value = newState.copy(
            latencyMs = _currentLatency.value,
            connectedDevicesCount = _devicesList.value.count { it.isOnline }
        )
    }

    fun triggerNotification(message: String) {
        val list = _notifications.value.toMutableList()
        list.add(0, message)
        _notifications.value = list
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun refreshNetworkState() {
        viewModelScope.launch {
            _isRefreshing.value = true
            updateWifiState()
            _devicesList.value = networkManager.getFallbackDevices()
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun startNetworkScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            triggerNotification("Starting dynamic network subnet scan...")
            
            // Start scanning, discover devices and update
            val activeDevices = mutableListOf<NetworkDevice>()
            activeDevices.addAll(networkManager.getFallbackDevices())

            // Simulate finding some active/new devices or scanning
            networkManager.scanLocalNetwork { discovered ->
                val list = _devicesList.value.toMutableList()
                val existingIdx = list.indexOfFirst { it.macAddress == discovered.macAddress }
                if (existingIdx >= 0) {
                    list[existingIdx] = discovered
                } else {
                    list.add(discovered)
                    if (notifyNewDevices.value) {
                        triggerNotification("New device joined WiFi: ${discovered.name}")
                    }
                }
                _devicesList.value = list
            }

            delay(2000) // Keep scanner animation beautiful
            _isScanning.value = false
            triggerNotification("WiFi network scan completed. Found ${_devicesList.value.size} devices.")
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun copyIpAddress() {
        triggerNotification("IP Address copied: ${_wifiState.value.ipAddress}")
    }

    fun shareNetworkReport() {
        triggerNotification("Network Report PDF shared successfully")
    }

    fun exportReport(format: String) {
        triggerNotification("Report exported successfully as $format")
    }

    // --- Speed Test Simulation Engine ---
    fun runSpeedTest() {
        if (_speedTestState.value.phase != SpeedTestPhase.IDLE) return

        viewModelScope.launch(Dispatchers.Default) {
            // Phase 1: Ping & Jitter
            _speedTestState.value = SpeedTestState(phase = SpeedTestPhase.PING_JITTER, progress = 0.0f)
            val durationPing = 2000L
            val stepsPing = 20
            for (i in 1..stepsPing) {
                delay(durationPing / stepsPing)
                val tempPing = Random.nextInt(12, 28)
                val tempJitter = Random.nextInt(2, 6)
                _speedTestState.value = SpeedTestState(
                    phase = SpeedTestPhase.PING_JITTER,
                    progress = i.toFloat() / stepsPing,
                    ping = tempPing,
                    jitter = tempJitter
                )
            }

            // Phase 2: Download Speed Test
            val baseDownload = Random.nextDouble(180.0, 520.0)
            val durationDownload = 5000L
            val stepsDownload = 50
            for (i in 1..stepsDownload) {
                delay(durationDownload / stepsDownload)
                val progress = i.toFloat() / stepsDownload
                // Add elegant noise curve
                val curve = baseDownload * (0.4 + 0.6 * kotlin.math.sin(progress * Math.PI / 2))
                val fluctuation = curve + Random.nextDouble(-15.0, 15.0)
                _speedTestState.value = _speedTestState.value.copy(
                    phase = SpeedTestPhase.DOWNLOAD,
                    progress = progress,
                    downloadSpeed = String.format("%.1f", fluctuation).toDouble()
                )
            }

            // Phase 3: Upload Speed Test
            val baseUpload = Random.nextDouble(45.0, 120.0)
            val durationUpload = 5000L
            val stepsUpload = 50
            for (i in 1..stepsUpload) {
                delay(durationUpload / stepsUpload)
                val progress = i.toFloat() / stepsUpload
                val curve = baseUpload * (0.5 + 0.5 * kotlin.math.sin(progress * Math.PI / 2))
                val fluctuation = curve + Random.nextDouble(-5.0, 5.0)
                _speedTestState.value = _speedTestState.value.copy(
                    phase = SpeedTestPhase.UPLOAD,
                    progress = progress,
                    uploadSpeed = String.format("%.1f", fluctuation).toDouble()
                )
            }

            // Phase 4: Finished & Store inside Database!
            val finalPing = _speedTestState.value.ping
            val finalJitter = _speedTestState.value.jitter
            val finalDownload = _speedTestState.value.downloadSpeed
            val finalUpload = _speedTestState.value.uploadSpeed

            val record = SpeedTestRecord(
                downloadSpeed = finalDownload,
                uploadSpeed = finalUpload,
                ping = finalPing,
                jitter = finalJitter
            )
            repository.insertSpeedTest(record)

            _speedTestState.value = _speedTestState.value.copy(
                phase = SpeedTestPhase.FINISHED,
                progress = 1.0f
            )

            triggerNotification("Speed test finished: DL: ${finalDownload}Mbps, UL: ${finalUpload}Mbps")

            // Wait 3 seconds, then clear state
            delay(4000)
            resetSpeedTest()
        }
    }

    fun resetSpeedTest() {
        _speedTestState.value = SpeedTestState(phase = SpeedTestPhase.IDLE)
    }

    // --- Helper to Generate Database Sample Data ---
    private suspend fun generateDemoHistory() {
        val currentTime = System.currentTimeMillis()
        val hourMs = 3600_000L

        // Generate 10 Speed Tests spaced out
        for (i in 1..8) {
            val timestamp = currentTime - (10 - i) * hourMs * 4
            repository.insertSpeedTest(
                SpeedTestRecord(
                    timestamp = timestamp,
                    downloadSpeed = Random.nextDouble(150.0, 480.0),
                    uploadSpeed = Random.nextDouble(35.0, 95.0),
                    ping = Random.nextInt(14, 25),
                    jitter = Random.nextInt(2, 7)
                )
            )
        }

        // Generate Ping Records spaced out for lines chart
        for (i in 1..30) {
            val timestamp = currentTime - (30 - i) * 1000L * 10 // Last 5 minutes
            repository.insertPingRecord(
                PingRecord(
                    timestamp = timestamp,
                    latency = Random.nextInt(15, 35),
                    signalStrength = Random.nextInt(75, 95),
                    rssi = Random.nextInt(-65, -45),
                    ssid = "Home WiFi 5GHz"
                )
            )
        }

        // Generate Device count history records for timeline chart
        for (i in 1..12) {
            val timestamp = currentTime - (12 - i) * hourMs
            repository.insertDeviceHistory(
                DeviceHistoryRecord(
                    timestamp = timestamp,
                    deviceCount = Random.nextInt(8, 14)
                )
            )
        }
    }
}
