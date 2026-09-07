package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.database.TripPointEntity
import com.example.data.database.TripSessionEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun generateCsv(session: TripSessionEntity, points: List<TripPointEntity>): String {
        val sb = StringBuilder()
        sb.append("timestamp,latitude,longitude,gps_speed,obd_speed,rpm,coolant_temperature,engine_load,throttle,maf,map,voltage,fuel_level\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        for (p in points) {
            val dateStr = sdf.format(Date(p.timestamp))
            sb.append("${dateStr},")
            sb.append("${p.gpsLat ?: ""},")
            sb.append("${p.gpsLon ?: ""},")
            sb.append("${p.gpsSpeed ?: ""},")
            sb.append("${p.speedKmh ?: ""},")
            sb.append("${p.rpm ?: ""},")
            sb.append("${p.coolantTemp ?: ""},")
            sb.append("${p.engineLoad ?: ""},")
            sb.append("${p.throttle ?: ""},")
            sb.append("${p.maf ?: ""},")
            sb.append("${p.mapKpa ?: ""},")
            sb.append("${p.batteryVoltage ?: ""},")
            sb.append("${p.fuelLevel ?: ""}\n")
        }
        return sb.toString()
    }

    fun generateJson(session: TripSessionEntity, points: List<TripPointEntity>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"session\": {\n")
        sb.append("    \"id\": ${session.id},\n")
        sb.append("    \"vehicle\": \"${session.vehicleName}\",\n")
        sb.append("    \"startTime\": ${session.startTime},\n")
        sb.append("    \"endTime\": ${session.endTime},\n")
        sb.append("    \"durationSeconds\": ${session.durationSeconds},\n")
        sb.append("    \"distanceKm\": ${session.distanceKm},\n")
        sb.append("    \"maxSpeedKmh\": ${session.maxSpeedKmh},\n")
        sb.append("    \"avgSpeedKmh\": ${session.avgSpeedKmh},\n")
        sb.append("    \"maxRpm\": ${session.maxRpm},\n")
        sb.append("    \"maxCoolantTemp\": ${session.maxCoolantTemp},\n")
        sb.append("    \"estimatedFuelLiters\": ${session.estimatedFuelLiters}\n")
        sb.append("  },\n")
        sb.append("  \"telemetry\": [\n")
        points.forEachIndexed { index, p ->
            sb.append("    {\n")
            sb.append("      \"timestamp\": ${p.timestamp},\n")
            sb.append("      \"obdSpeed\": ${p.speedKmh ?: "null"},\n")
            sb.append("      \"rpm\": ${p.rpm ?: "null"},\n")
            sb.append("      \"coolant\": ${p.coolantTemp ?: "null"},\n")
            sb.append("      \"load\": ${p.engineLoad ?: "null"},\n")
            sb.append("      \"throttle\": ${p.throttle ?: "null"},\n")
            sb.append("      \"maf\": ${p.maf ?: "null"},\n")
            sb.append("      \"voltage\": ${p.batteryVoltage ?: "null"},\n")
            sb.append("      \"gpsLat\": ${p.gpsLat ?: "null"},\n")
            sb.append("      \"gpsLon\": ${p.gpsLon ?: "null"}\n")
            sb.append("    }${if (index < points.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun generateGpx(session: TripSessionEntity, points: List<TripPointEntity>): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"OBD Monitor\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata><name>Trip ${session.id}</name></metadata>\n")
        sb.append("  <trk><name>${session.vehicleName} - Viaje</name><trkseg>\n")
        for (p in points) {
            if (p.gpsLat != null && p.gpsLon != null) {
                val timeStr = sdf.format(Date(p.timestamp))
                sb.append("    <trkpt lat=\"${p.gpsLat}\" lon=\"${p.gpsLon}\">\n")
                sb.append("      <time>$timeStr</time>\n")
                if (p.gpsSpeed != null) {
                    sb.append("      <speed>${p.gpsSpeed / 3.6f}</speed>\n")
                }
                sb.append("    </trkpt>\n")
            }
        }
        sb.append("  </trkseg></trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    fun shareExport(context: Context, filename: String, content: String, mimeType: String) {
        try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, filename)
            FileWriter(file).use { it.write(content) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Exportación OBD Monitor - $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir datos del viaje"))
        } catch (e: Exception) {
            // Share intent fallback: share plain text directly
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content.take(10000))
                putExtra(Intent.EXTRA_SUBJECT, filename)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir datos"))
        }
    }
}
