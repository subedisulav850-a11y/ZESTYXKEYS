package com.roha.ffaessextractor

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KeyStore {
    private val file = File("/sdcard/Download/ff_keys.txt")

    fun append(type: String, hex: String, str: String) {
        try {
            if (!file.parentFile!!.exists()) file.parentFile!!.mkdirs()
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val line = "[$ts] $type\n  HEX: $hex\n  STR: $str\n\n"
            file.appendText(line)
        } catch (_: Exception) { }
    }

    fun readAll(): String {
        return try {
            if (file.exists()) file.readText() else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun clear() {
        try {
            if (file.exists()) file.writeText("")
        } catch (_: Exception) { }
    }
}
