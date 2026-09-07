package com.example.domain.model

enum class ConnectionState(val displayName: String) {
    DISCONNECTED("Desconectado"),
    SCANNING("Buscando dispositivos..."),
    CONNECTING("Conectando Bluetooth..."),
    CONNECTED("Bluetooth Conectado"),
    INITIALIZING("Inicializando ELM327..."),
    READY("Listo"),
    MONITORING("Monitorizando en tiempo real"),
    RECONNECTING("Intentando reconectar..."),
    ERROR("Error de comunicación")
}
