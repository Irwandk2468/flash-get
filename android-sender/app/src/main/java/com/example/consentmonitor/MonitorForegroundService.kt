package com.example.consentmonitor

import android.app.*
import android.content.*
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class MonitorForegroundService: Service() {
  companion object { var isRunning = false; private const val CH_ID = "monitor_ch" }
  private var fused: FusedLocationProviderClient? = null
  private var cb: LocationCallback? = null

  override fun onCreate() {
    super.onCreate()
    isRunning = true
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH_ID, "Consent Monitor", NotificationManager.IMPORTANCE_LOW))
    val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    startForeground(1001, NotificationCompat.Builder(this, CH_ID).setContentTitle("Consent Monitor").setContentText("Monitoring aktif").setSmallIcon(android.R.drawable.ic_menu_info_details).setContentIntent(pi).setOngoing(true).build())

    UsageStatsCollector.start(this)

    fused = LocationServices.getFusedLocationProviderClient(this)
    val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).setMinUpdateIntervalMillis(15_000L).build()
    cb = object: LocationCallback() {
      override fun onLocationResult(r: LocationResult) {
        val loc: Location = r.lastLocation ?: return
        Uploader.enqueue(applicationContext, MonitorEvent(type = "location", lat = loc.latitude, lon = loc.longitude, title = "Location update"))
        MonitorLogger.log("[Loc] ${loc.latitude}, ${loc.longitude}")
      }
    }
    try { fused?.requestLocationUpdates(req, cb as LocationCallback, mainLooper) } catch (_: SecurityException) { MonitorLogger.log("[Loc] permission not granted") }
  }

  override fun onDestroy() { super.onDestroy(); isRunning = false; UsageStatsCollector.stop(); cb?.let { fused?.removeLocationUpdates(it) } }
  override fun onBind(i: Intent?): IBinder? = null
}
