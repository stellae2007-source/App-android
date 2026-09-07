package com.example.domain.model

data class SupportedPids(
    val supportedPidsSet: Set<Int> = emptySet(),
    val totalDiscovered: Int = 0
) {
    fun isPidSupported(pid: Int): Boolean {
        return supportedPidsSet.contains(pid)
    }

    companion object {
        fun parseFromBitmask(basePid: Int, hexBytes: List<Int>): Set<Int> {
            val supported = mutableSetOf<Int>()
            var pidOffset = 1
            for (byteVal in hexBytes) {
                for (bit in 7 downTo 0) {
                    if ((byteVal and (1 shl bit)) != 0) {
                        supported.add(basePid + pidOffset)
                    }
                    pidOffset++
                }
            }
            return supported
        }
    }
}
