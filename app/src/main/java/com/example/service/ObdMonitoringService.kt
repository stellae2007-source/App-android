package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ObdApplication
import com.example.R
import com.example.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ObdMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var updatesJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Iniciando monitorización...", "Conectando con adaptador OBD2"))

        startObservingData()

        return START_STICKY
    }

    private fun startObservingData() {
        val app = application as? ObdApplication ?: return
        val repo = app.obdRepository

        updatesJob?.cancel()
        updatesJob = serviceScope.launch {
            combine(repo.connectionState, repo.liveData) { state, data ->
                Pair(state, data)
            }.collect { (state, data) ->
                val title = when (state) {
                    ConnectionState.MONITORING -> "OBD Monitor Activo"
                    ConnectionState.RECONNECTING -> "Reconectando con el vehículo..."
                    ConnectionState.CONNECTING -> "Conectando Bluetooth..."
                    else -> "OBD Monitor: ${state.displayName}"
                }

                val content = if (state == ConnectionState.MONITORING) {
                    val rpmStr = data.rpm?.let { "$it RPM" } ?: "-- RPM"
                    val speedStr = data.speed?.let { "$it km/h" } ?: "-- km/h"
                    val tempStr = data.coolantTemperature?.let { "$it°C" } ?: "--°C"
                    "$speedStr • $rpmStr • Temp: $tempStr"
                } else {
                    state.displayName
                }

                val notification = buildNotification(title, content)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ObdApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        updatesJob?.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.obdmonitor.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, ObdMonitoringService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ObdMonitoringService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
