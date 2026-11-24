package com.roha.ffaessextractor

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

object FridaManager {
    var targetPackage: String = "com.garena.game.ffi"
    var targetName: String = "Free Fire"

    private const val FRIDA_SERVER = "/data/local/tmp/frida-server"
    private const val HOOK_JS = "ff_hook.js"

    fun ensureRootAndServer(ctx: Context): Boolean {
        if (!suOk()) return false
        if (!serverRunning()) {
            su("chmod 755 $FRIDA_SERVER")
            su("$FRIDA_SERVER -D &")
            Thread.sleep(1500)
        }
        return serverRunning()
    }

    fun runExtractor(context: Context, onKey: (String, String, String) -> Unit) {
        val script = HookScript.content
        val scriptFile = File(context.filesDir, HOOK_JS).apply {
            writeText(script)
        }

        // Attach to running process by package name
        val cmd = "frida -U -n $targetPackage -l ${scriptFile.absolutePath} --runtime=v8 2>&1"

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val reader = process.inputStream.bufferedReader()

            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.contains("HEX :") -> {
                            val hex = line.substringAfter("HEX :")
                                .trim()
                                .substringBefore("║")
                                .trim()
                            if (hex.length >= 16) {
                                val type = if (line.contains("KEY") || previousWasKey) "KEY" else "IV"
                                onKey(type, hex, "")
                            }
                        }
                        line.contains("AES KEY FOUND") -> {
                            // next HEX line is key
                        }
                        line.contains("AES IV FOUND") -> {
                            // next HEX line is iv
                        }
                        line.contains("[NATIVE KEY]") -> {
                            val hex = line.substringAfter("[NATIVE KEY]")
                                .trim()
                                .removePrefix(":")
                                .trim()
                            if (hex.isNotEmpty()) onKey("NATIVE_KEY", hex, "")
                        }
                        line.contains("[NATIVE IV]") -> {
                            val hex = line.substringAfter("[NATIVE IV]")
                                .trim()
                                .removePrefix(":")
                                .trim()
                            if (hex.isNotEmpty()) onKey("NATIVE_IV", hex, "")
                        }
                    }
                }
            }
            process.waitFor(30, TimeUnit.MINUTES)
        } catch (e: Exception) {
            // log silently
        }
    }

    private var previousWasKey = false

    private fun suOk(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor(3, TimeUnit.SECONDS)
            out.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }

    private fun serverRunning(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pgrep -f frida-server"))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor(2, TimeUnit.SECONDS)
            out.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun su(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor(5, TimeUnit.SECONDS)
        } catch (_: Exception) { }
    }
}
