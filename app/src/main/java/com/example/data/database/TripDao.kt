package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TripSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TripPointEntity>)

    @Query("SELECT * FROM trip_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TripSessionEntity>>

    @Query("SELECT * FROM trip_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): TripSessionEntity?

    @Query("SELECT * FROM trip_telemetry_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<TripPointEntity>

    @Query("DELETE FROM trip_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM trip_sessions")
    suspend fun clearAllSessions()
}
