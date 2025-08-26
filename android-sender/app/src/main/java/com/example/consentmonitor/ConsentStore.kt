package com.example.consentmonitor

import android.content.Context
import android.provider.Settings

object ConsentStore {
  private const val PREFS = "consent_prefs"
  private const val KEY = "consented"
  fun isConsented(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
  fun setConsented(ctx: Context, v: Boolean) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, v).apply() }
  fun deviceId(ctx: Context) = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
}
