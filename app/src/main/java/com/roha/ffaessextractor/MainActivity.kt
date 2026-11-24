package com.roha.ffaessextractor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.roha.ffaessextractor.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val keyStore = KeyStore()

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startService()
        else Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestBatteryExemption()
        requestNotificationPermission()
        loadSavedKeys()

        binding.btnStart.setOnClickListener {
            val pkg = binding.etPackage.text.toString().trim()
            val name = binding.etGameName.text.toString().trim().ifEmpty { "Game" }
            if (pkg.isEmpty()) {
                Toast.makeText(this, "Package required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FridaManager.targetPackage = pkg
            FridaManager.targetName = name
            prepareAndStart()
        }

        binding.btnStop.setOnClickListener {
            stopService(Intent(this, ExtractorVpnService::class.java))
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
        }

        binding.btnClear.setOnClickListener {
            keyStore.clear()
            binding.tvKeys.text = "No keys yet"
        }

        binding.btnRefresh.setOnClickListener { loadSavedKeys() }
    }

    private fun prepareAndStart() {
        lifecycleScope.launch {
            val rootOk = withContext(Dispatchers.IO) {
                FridaManager.ensureRootAndServer(this@MainActivity)
            }
            if (!rootOk) {
                Toast.makeText(
                    this@MainActivity,
                    "Root / frida-server required",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val intent = VpnService.prepare(this@MainActivity)
            if (intent != null) vpnPermission.launch(intent)
            else startService()
        }
    }

    private fun startService() {
        val i = Intent(this, ExtractorVpnService::class.java).apply {
            action = ExtractorVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(this, i)
        Toast.makeText(this, "Extractor running in background", Toast.LENGTH_SHORT).show()
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (_: Exception) { }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadSavedKeys() {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) { keyStore.readAll() }
            binding.tvKeys.text = text.ifEmpty { "No keys yet" }
        }
    }
}
