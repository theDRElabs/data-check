package com.drelabs.datacheck.util

object Format {
    fun bytes(bytes: Long): String {
        if (bytes < 0) return "?"
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024 -> String.format("%.2f GB", mb / 1024)
            mb >= 10 -> String.format("%.0f MB", mb)
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", bytes / 1024.0)
        }
    }
}
