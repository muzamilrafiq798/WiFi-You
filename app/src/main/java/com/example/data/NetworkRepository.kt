package com.example.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {

    val allSpeedTests: Flow<List<SpeedTestRecord>> = networkDao.getAllSpeedTests()
    fun getRecentSpeedTests(limit: Int): Flow<List<SpeedTestRecord>> = networkDao.getRecentSpeedTests(limit)
    suspend fun insertSpeedTest(record: SpeedTestRecord) = networkDao.insertSpeedTest(record)

    val allPingRecords: Flow<List<PingRecord>> = networkDao.getAllPingRecords()
    fun getRecentPingRecords(limit: Int): Flow<List<PingRecord>> = networkDao.getRecentPingRecords(limit)
    suspend fun insertPingRecord(record: PingRecord) = networkDao.insertPingRecord(record)
    suspend fun pruneOldPingRecords(threshold: Long) = networkDao.pruneOldPingRecords(threshold)

    val allDeviceHistory: Flow<List<DeviceHistoryRecord>> = networkDao.getAllDeviceHistory()
    fun getRecentDeviceHistory(limit: Int): Flow<List<DeviceHistoryRecord>> = networkDao.getRecentDeviceHistory(limit)
    suspend fun insertDeviceHistory(record: DeviceHistoryRecord) = networkDao.insertDeviceHistory(record)
}
