package com.roha.ffaessextractor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ExtractorVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.roha.ffaessextractor.START"
        const val CHANNEL_ID = "extractor_channel"
        const val NOTIF_ID = 9002
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fridaJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            startForeground(NOTIF_ID, buildNotification())
            establishDummyVpn()
            startFridaLoop()
        }
        return START_STICKY
    }

    private fun establishDummyVpn() {
        try {
            val builder = Builder()
                .setSession("FF AES Extractor")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(false)
            vpnInterface = builder.establish()
        } catch (_: Exception) {
            // still continue without VPN tunnel if blocked
        }
    }

    private fun startFridaLoop() {
        fridaJob?.cancel()
        fridaJob = scope.launch {
            FridaManager.runExtractor(
                context = this@ExtractorVpnService,
                onKey = { type, hex, str ->
                    KeyStore().append(type, hex, str)
                    updateNotification("Captured $type")
                }
            )
        }
    }

    private fun buildNotification(status: String = "Waiting for keys..."): Notification {
        createChannel()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AES Extractor — ${FridaManager.targetName}")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(status))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Extractor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        fridaJob?.cancel()
        scope.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) { }
        super.onDestroy()
    }
}
