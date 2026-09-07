package com.example.domain.model

data class ObdDebugMessage(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: Direction,
    val command: String,
    val rawResponse: String? = null,
    val interpretation: String? = null
) {
    enum class Direction {
        TX, RX, INFO, ERROR
    }
}
