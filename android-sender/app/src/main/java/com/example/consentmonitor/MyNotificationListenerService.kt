package com.example.consentmonitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotificationListenerService: NotificationListenerService() {
  override fun onNotificationPosted(sbn: StatusBarNotification) {
    if (!ConsentStore.isConsented(this)) return
    val ex = sbn.notification.extras
    val title = ex.getString(Notification.EXTRA_TITLE) ?: ""
    val text  = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    val big   = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
    val lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" | ") { it.toString() } ?: ""
    val body = when { big.isNotBlank() -> big; lines.isNotBlank() -> lines; else -> text }
    val ev = MonitorEvent(type = "notification", app = sbn.packageName ?: "unknown", title = title, body = body.take(4000))
    MonitorLogger.log("[Notif] ${sbn.packageName} • $title — $body")
    Uploader.enqueue(this, ev)
  }
  override fun onListenerConnected() { MonitorLogger.log("[Notif] Listener connected") }
}
