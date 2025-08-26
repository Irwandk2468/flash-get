package com.example.consentmonitor

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity: AppCompatActivity() {
  private lateinit var tvStatus: TextView
  private lateinit var tvLog: TextView

  private val reqNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()){}
  private val reqLoc = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}

  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    setContentView(R.layout.activity_main)

    tvStatus = findViewById(R.id.tvStatus)
    tvLog = findViewById(R.id.tvLog)

    findViewById<Button>(R.id.btnConsent).setOnClickListener { startActivity(Intent(this, ConsentActivity::class.java)) }
    findViewById<Button>(R.id.btnNotifAccess).setOnClickListener { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
    findViewById<Button>(R.id.btnUsageAccess).setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    findViewById<Button>(R.id.btnLocation).setOnClickListener { reqLoc.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    findViewById<Button>(R.id.btnStart).setOnClickListener { startForegroundService(Intent(this, MonitorForegroundService::class.java)); tvStatus.text = "Status: Monitoring aktif" }
    findViewById<Button>(R.id.btnStop).setOnClickListener { stopService(Intent(this, MonitorForegroundService::class.java)); tvStatus.text = "Status: Idle" }

    if (Build.VERSION.SDK_INT >= 33) reqNotif.launch(Manifest.permission.POST_NOTIFICATIONS)

    MonitorLogger.attach { line -> runOnUiThread { tvLog.append("\n$line") } }
  }

  override fun onResume() {
    super.onResume()
    tvStatus.text = "Status: " + if (MonitorForegroundService.isRunning) "Monitoring aktif" else "Idle"
    val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    if (!enabled) tvLog.append("\n[!] Akses notifikasi belum aktif")
  }
}
