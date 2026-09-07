package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.bluetooth.BluetoothManager
import com.example.data.database.AppDatabase
import com.example.data.obd.Elm327Manager
import com.example.data.obd.Obd2Manager
import com.example.data.repository.ObdRepository
import com.example.data.repository.TripRecorder
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ObdApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var bluetoothManager: BluetoothManager
        private set

    lateinit var elm327Manager: Elm327Manager
        private set

    lateinit var obd2Manager: Obd2Manager
        private set

    lateinit var tripRecorder: TripRecorder
        private set

    lateinit var vehicleRepository: VehicleRepository
        private set

    lateinit var obdRepository: ObdRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()

        database = AppDatabase.getInstance(this)
        bluetoothManager = BluetoothManager(this, applicationScope)
        elm327Manager = Elm327Manager(bluetoothManager) { debugMsg ->
            if (::obdRepository.isInitialized) {
                obdRepository.addDebugLog(debugMsg)
            }
        }
        obd2Manager = Obd2Manager(bluetoothManager, elm327Manager, applicationScope) { debugMsg ->
            if (::obdRepository.isInitialized) {
                obdRepository.addDebugLog(debugMsg)
            }
        }
        tripRecorder = TripRecorder(database.tripDao(), applicationScope)
        vehicleRepository = VehicleRepository(database.vehicleDao())

        obdRepository = ObdRepository(
            bluetoothManager = bluetoothManager,
            elm327Manager = elm327Manager,
            obd2Manager = obd2Manager,
            tripRecorder = tripRecorder,
            vehicleDao = database.vehicleDao(),
            coroutineScope = applicationScope
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Monitorización OBD-II",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la conexión con el vehículo mientras conduces"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "obd_monitoring_channel"
        lateinit var instance: ObdApplication
            private set
    }
}
