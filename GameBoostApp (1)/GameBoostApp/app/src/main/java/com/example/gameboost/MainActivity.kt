package com.example.gameboost

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gameboost.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayRunning = false

    private val statsUpdater = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        setupVolumeSeekBar()
        setupOverlayButton()
        setupGamingModeButton()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        handler.post(statsUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statsUpdater)
    }

    // ---------- CPU / Battery / RAM ----------

    private fun updateStats() {
        val cpu = SystemMonitor.cpuUsagePercent()
        binding.tvCpuUsage.text = if (cpu >= 0) "CPU Usage: $cpu%" else "CPU Usage: N/A on this device"

        val battery = SystemMonitor.batteryPercent(this)
        val charging = if (SystemMonitor.isCharging(this)) " (charging)" else ""
        binding.tvBattery.text = "Battery: $battery%$charging"

        val ram = SystemMonitor.freeRamMb(this)
        binding.tvRam.text = "Free RAM: $ram MB"
    }

    // ---------- FPS overlay ----------

    private fun setupOverlayButton() {
        binding.btnToggleOverlay.setOnClickListener {
            if (!overlayRunning) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Please allow \"Display over other apps\" for GameBoost", Toast.LENGTH_LONG).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    return@setOnClickListener
                }
                startService(Intent(this, OverlayService::class.java))
                overlayRunning = true
                binding.btnToggleOverlay.text = "Stop FPS Overlay"
            } else {
                stopService(Intent(this, OverlayService::class.java))
                overlayRunning = false
                binding.btnToggleOverlay.text = "Start FPS Overlay"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (overlayRunning && !Settings.canDrawOverlays(this)) {
            // permission was revoked while overlay was "on"
            overlayRunning = false
            binding.btnToggleOverlay.text = "Start FPS Overlay"
        }
    }

    // ---------- Gaming Mode (Do Not Disturb) ----------

    private fun setupGamingModeButton() {
        binding.btnGamingMode.setOnClickListener {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                Toast.makeText(this, "Allow Do Not Disturb access for Gaming Mode", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                return@setOnClickListener
            }

            val currentlyOn = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
            if (currentlyOn) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                binding.btnGamingMode.text = "Enable Gaming Mode (DND)"
                Toast.makeText(this, "Gaming Mode off", Toast.LENGTH_SHORT).show()
            } else {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                binding.btnGamingMode.text = "Disable Gaming Mode"
                Toast.makeText(this, "Gaming Mode on: notifications silenced", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Volume ----------

    private fun setupVolumeSeekBar() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekVolume.max = max
        binding.seekVolume.progress = current

        binding.seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ---------- Android 13+ notification permission (needed for the foreground service) ----------

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
