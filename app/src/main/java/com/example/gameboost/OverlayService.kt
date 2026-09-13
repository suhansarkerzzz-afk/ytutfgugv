package com.example.gameboost

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Foreground service that draws a small floating FPS counter over other apps.
 *
 * How the FPS number is derived:
 * We use Choreographer.postFrameCallback, which fires once per display vsync/frame.
 * Counting callbacks per second gives the actual UI thread frame rate of the
 * *current foreground content* the system is compositing — this is the same technique
 * used by most non-root FPS overlay apps. It is an approximation of a game's true
 * render FPS (which lives in the game's own GL/Vulkan thread and isn't exposed to
 * other apps without root), but it reliably reflects dropped frames / jank system-wide.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private var frameCount = 0
    private var lastReportTimeNanos = 0L
    private var running = false

    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frameCount++
            if (lastReportTimeNanos == 0L) lastReportTimeNanos = frameTimeNanos

            val elapsedNanos = frameTimeNanos - lastReportTimeNanos
            if (elapsedNanos >= 1_000_000_000L) {
                val fps = frameCount
                overlayView?.findViewById<TextView>(R.id.tvFps)?.text = "FPS: $fps"
                frameCount = 0
                lastReportTimeNanos = frameTimeNanos
            }
            choreographer.postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addOverlayView()
        running = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun startForegroundWithNotification() {
        val channelId = "gameboost_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FPS Overlay",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GameBoost")
            .setContentText("FPS overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun addOverlayView() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_fps, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        windowManager.addView(view, params)
        overlayView = view
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        choreographer.removeFrameCallback(frameCallback)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // view already detached
            }
        }
    }
}
