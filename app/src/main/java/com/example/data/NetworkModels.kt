package com.example.data

enum class WifiStatus {
    CONNECTED,
    DISCONNECTED,
    LIMITED
}

enum class DeviceType {
    PHONE,
    LAPTOP,
    TV,
    CONSOLE,
    ROUTER,
    PRINTER,
    CAMERA,
    SMART_HOME,
    UNKNOWN
}

enum class ConnectionType {
    WIFI,
    ETHERNET
}

data class WifiConnectionState(
    val ssid: String = "Disconnected",
    val status: WifiStatus = WifiStatus.DISCONNECTED,
    val ipAddress: String = "0.0.0.0",
    val gateway: String = "0.0.0.0",
    val signalStrengthLabel: String = "No Signal", // Excellent, Good, Fair, Weak
    val rssi: Int = -100, // dBm
    val signalPercentage: Int = 0,
    val frequencyGhz: Double = 0.0, // 2.4, 5.0, 6.0
    val linkSpeedMbps: Int = 0,
    val latencyMs: Int = 0,
    val internetStatus: String = "Offline", // Online, Offline, Limited
    val connectedDevicesCount: Int = 0,
    val externalIp: String = "0.0.0.0"
)

data class NetworkDevice(
    val id: String,
    val type: DeviceType,
    val name: String,
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val manufacturer: String,
    val connectionType: ConnectionType,
    val signalQuality: Int, // percentage 0-100
    val firstSeen: String,
    val lastSeen: String,
    val isOnline: Boolean,
    val deviceModel: String,
    val ipv6: String = "fe80::1",
    val dns: String = "8.8.8.8",
    val estimatedLatencyMs: Int = 15,
    val dataUsageUpMb: Double = 0.0,
    val dataUsageDownMb: Double = 0.0
)

enum class SpeedTestPhase {
    IDLE,
    PING_JITTER,
    DOWNLOAD,
    UPLOAD,
    FINISHED
}

data class SpeedTestState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progress: Float = 0.0f,
    val downloadSpeed: Double = 0.0, // Mbps
    val uploadSpeed: Double = 0.0,   // Mbps
    val ping: Int = 0,               // ms
    val jitter: Int = 0              // ms
)
