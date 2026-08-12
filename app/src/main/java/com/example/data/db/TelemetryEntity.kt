package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_logs")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val androidVersion: String,
    val deviceModel: String,
    val firstLaunchDate: String,
    val appVersion: String,
    val publicIp: String,
    val totalLecturesCount: Int,
    val proxyMode: String,
    val timestamp: Long = System.currentTimeMillis()
)
