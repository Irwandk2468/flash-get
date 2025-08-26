package com.example.consentmonitor
import android.content.*
class BootReceiver: BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent){ if (ConsentStore.isConsented(c)) c.startForegroundService(Intent(c, MonitorForegroundService::class.java)) } }
