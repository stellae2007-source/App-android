package com.example.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    // Standard SPP (Serial Port Profile) UUID for Bluetooth Classic ELM327
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceModel>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var lastTargetDevice: BluetoothDeviceModel? = null
    private var reconnectJob: Job? = null
    private var autoReconnectEnabled = true

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                    if (state == BluetoothAdapter.STATE_ON) {
                        refreshPairedDevices()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let { d ->
                        val name = try {
                            if (hasConnectPermission()) d.name ?: "Dispositivo Desconocido" else "Dispositivo"
                        } catch (e: SecurityException) {
                            "Dispositivo"
                        }
                        val address = d.address ?: "00:00:00:00:00:00"
                        val isBonded = d.bondState == BluetoothDevice.BOND_BONDED

                        val model = BluetoothDeviceModel(
                            name = name,
                            address = address,
                            isBonded = isBonded,
                            isLikelyObd = BluetoothDeviceModel.checkLikelyObd(name),
                            rssi = rssi
                        )

                        val current = _discoveredDevices.value.toMutableList()
                        val existingIndex = current.indexOfFirst { it.address == address }
                        if (existingIndex >= 0) {
                            current[existingIndex] = model
                        } else {
                            current.add(model)
                        }
                        _discoveredDevices.value = current.sortedWith(
                            compareByDescending<BluetoothDeviceModel> { it.isLikelyObd }
                                .thenByDescending { it.isBonded }
                                .thenBy { it.name }
                        )
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_connectionState.value == ConnectionState.SCANNING) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            context.registerReceiver(bluetoothReceiver, filter)
        } catch (e: Exception) {
            // Ignored if receiver fails
        }
        refreshPairedDevices()
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasConnectPermission()) return
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        try {
            val bonded = adapter.bondedDevices ?: emptySet()
            _pairedDevices.value = bonded.map { device ->
                val name = device.name ?: "OBD Adapter"
                BluetoothDeviceModel(
                    name = name,
                    address = device.address,
                    isBonded = true,
                    isLikelyObd = BluetoothDeviceModel.checkLikelyObd(name)
                )
            }.sortedByDescending { it.isLikelyObd }
        } catch (e: Exception) {
            // Permission or adapter error
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasScanPermission()) return
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            _connectionState.value = ConnectionState.SCANNING
            adapter.startDiscovery()
            refreshPairedDevices()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!hasScanPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceModel: BluetoothDeviceModel): Boolean = withContext(Dispatchers.IO) {
        lastTargetDevice = deviceModel
        reconnectJob?.cancel()

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.ERROR
            return@withContext false
        }

        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.ERROR
            return@withContext false
        }

        stopDiscovery()
        disconnectInternal(clearTarget = false)

        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = deviceModel.name
        _connectedDeviceAddress.value = deviceModel.address

        try {
            val device = adapter.getRemoteDevice(deviceModel.address)

            // Attempt 1: Standard RFCOMM socket
            var currentSocket: BluetoothSocket? = null
            try {
                currentSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                currentSocket.connect()
            } catch (e1: IOException) {
                // Attempt 2: Fallback to insecure RFCOMM socket (crucial for ELM327 clones that reject secure pairing)
                try {
                    currentSocket?.close()
                } catch (ignored: Exception) {}

                try {
                    currentSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    currentSocket.connect()
                } catch (e2: IOException) {
                    // Attempt 3: Reflection fallback for channel 1 (standard for ELM327)
                    try {
                        currentSocket?.close()
                    } catch (ignored: Exception) {}

                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    currentSocket = method.invoke(device, 1) as BluetoothSocket
                    currentSocket.connect()
                }
            }

            socket = currentSocket
            inputStream = currentSocket?.inputStream
            outputStream = currentSocket?.outputStream

            _connectionState.value = ConnectionState.CONNECTED
            return@withContext true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            disconnectInternal(clearTarget = false)
            if (autoReconnectEnabled) {
                triggerAutoReconnect()
            }
            return@withContext false
        }
    }

    suspend fun sendRaw(data: String): Unit = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IOException("No hay conexión con el adaptador Bluetooth")
        val bytes = (data + "\r").toByteArray(Charsets.US_ASCII)
        out.write(bytes)
        out.flush()
    }

    suspend fun readUntilPrompt(timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val input = inputStream ?: throw IOException("No hay stream de lectura activo")
        val buffer = StringBuilder()
        val startTime = System.currentTimeMillis()

        while (coroutineScope.isActive) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                if (buffer.isNotEmpty()) {
                    return@withContext buffer.toString()
                }
                throw IOException("TIMEOUT: El adaptador ELM327 no respondió a tiempo")
            }

            if (input.available() > 0) {
                val byteRead = input.read()
                if (byteRead == -1) {
                    throw IOException("El adaptador ha cerrado la conexión Bluetooth")
                }
                val char = byteRead.toChar()
                if (char == '>') {
                    // Prompt reached
                    break
                }
                if (char != '\r') {
                    buffer.append(char)
                }
            } else {
                delay(8)
            }
        }

        return@withContext buffer.toString().trim()
    }

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun setAutoReconnect(enabled: Boolean) {
        autoReconnectEnabled = enabled
    }

    fun triggerAutoReconnect() {
        if (!autoReconnectEnabled) return
        val target = lastTargetDevice ?: return
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            delay(4000)
            if (isActive) {
                connect(target)
            }
        }
    }

    private fun disconnectInternal(clearTarget: Boolean = true) {
        try {
            inputStream?.close()
        } catch (e: Exception) {}
        try {
            outputStream?.close()
        } catch (e: Exception) {}
        try {
            socket?.close()
        } catch (e: Exception) {}

        socket = null
        inputStream = null
        outputStream = null

        if (clearTarget) {
            lastTargetDevice = null
            reconnectJob?.cancel()
            _connectedDeviceName.value = null
            _connectedDeviceAddress.value = null
        }
    }

    fun disconnect() {
        autoReconnectEnabled = false
        disconnectInternal(clearTarget = true)
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun cleanup() {
        disconnect()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {}
    }
}
