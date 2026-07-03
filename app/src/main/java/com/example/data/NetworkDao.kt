package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {

    // --- Speed Tests ---
    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC")
    fun getAllSpeedTests(): Flow<List<SpeedTestRecord>>

    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSpeedTests(limit: Int): Flow<List<SpeedTestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedTest(record: SpeedTestRecord)

    // --- Ping / Latency & Signal Records ---
    @Query("SELECT * FROM ping_records ORDER BY timestamp DESC")
    fun getAllPingRecords(): Flow<List<PingRecord>>

    @Query("SELECT * FROM ping_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPingRecords(limit: Int): Flow<List<PingRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPingRecord(record: PingRecord)

    @Query("DELETE FROM ping_records WHERE timestamp < :threshold")
    suspend fun pruneOldPingRecords(threshold: Long)

    // --- Device History ---
    @Query("SELECT * FROM device_history_records ORDER BY timestamp DESC")
    fun getAllDeviceHistory(): Flow<List<DeviceHistoryRecord>>

    @Query("SELECT * FROM device_history_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDeviceHistory(limit: Int): Flow<List<DeviceHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceHistory(record: DeviceHistoryRecord)
}
