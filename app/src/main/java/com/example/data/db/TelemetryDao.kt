package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: TelemetryEntity)

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 10")
    fun getLatestTelemetry(): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_logs ORDER BY id ASC LIMIT 1")
    suspend fun getFirstLaunchTelemetry(): TelemetryEntity?
}
