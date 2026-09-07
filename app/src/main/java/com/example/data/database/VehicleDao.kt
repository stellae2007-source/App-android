package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY lastConnected DESC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultVehicle(): VehicleEntity?

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicle(vehicleId: Long)
}
