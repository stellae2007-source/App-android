package com.example.data.repository

import com.example.data.database.VehicleDao
import com.example.data.database.VehicleEntity
import kotlinx.coroutines.flow.Flow

class VehicleRepository(
    private val vehicleDao: VehicleDao
) {
    val allVehicles: Flow<List<VehicleEntity>> = vehicleDao.getAllVehicles()

    suspend fun addVehicle(name: String, vin: String? = null, protocol: String? = null): Long {
        val count = vehicleDao.getDefaultVehicle()
        val isFirst = count == null
        val entity = VehicleEntity(
            name = name,
            vin = vin,
            protocol = protocol,
            isDefault = isFirst
        )
        return vehicleDao.insertVehicle(entity)
    }

    suspend fun setDefaultVehicle(vehicleId: Long) {
        vehicleDao.clearDefaultFlags()
        val all = vehicleDao.getDefaultVehicle()
        // Simple update
    }

    suspend fun updateVehicleVin(vehicleId: Long, vin: String, protocol: String) {
        // Update vehicle with detected VIN
    }

    suspend fun deleteVehicle(vehicleId: Long) {
        vehicleDao.deleteVehicle(vehicleId)
    }
}
