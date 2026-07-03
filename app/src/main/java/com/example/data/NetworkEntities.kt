package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_tests")
data class SpeedTestRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeed: Double, // in Mbps
    val uploadSpeed: Double,   // in Mbps
    val ping: Int,            // in ms
    val jitter: Int           // in ms
)

@Entity(tableName = "ping_records")
data class PingRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latency: Int,         // in ms
    val signalStrength: Int,  // 0 to 100 percentage
    val rssi: Int,            // in dBm
    val ssid: String
)

@Entity(tableName = "device_history_records")
data class DeviceHistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceCount: Int
)
