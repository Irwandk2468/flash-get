package com.example.consentmonitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlin.concurrent.thread

object UsageStatsCollector {
  @Volatile private var running = false
  fun start(ctx: Context) {
    if (running) return; running = true
    thread(name = "UsageStatsCollector") {
      val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
      while (running) {
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000L, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
          events.getNextEvent(e)
          if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            Uploader.enqueue(ctx, MonitorEvent(type = "app_foreground", app = e.packageName, title = "App to foreground", ts = e.timeStamp))
            MonitorLogger.log("[Usage] Foreground: ${e.packageName}")
          }
        }
        Thread.sleep(30_000L)
      }
    }
  }
  fun stop(){ running = false }
}
