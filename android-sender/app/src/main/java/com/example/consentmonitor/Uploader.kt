package com.example.consentmonitor

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.*

object Uploader {
  private var init = false
  private fun ensure(ctx: Context) {
    if (init) return
    try { FirebaseApp.initializeApp(ctx); Firebase.auth.signInAnonymously(); init = true } catch (e: Exception) { MonitorLogger.log("[Uploader] Firebase init failed: $e") }
  }
  fun enqueue(ctx: Context, e: MonitorEvent) {
    if (!ConsentStore.isConsented(ctx)) return
    ensure(ctx)
    val dev = ConsentStore.deviceId(ctx)
    CoroutineScope(Dispatchers.IO).launch {
      try { Firebase.firestore.collection("devices").document(dev).collection("events").add(e); MonitorLogger.log("[Uploader] sent") }
      catch (ex: Exception) { MonitorLogger.log("[Uploader] error: $ex") }
    }
  }
}
