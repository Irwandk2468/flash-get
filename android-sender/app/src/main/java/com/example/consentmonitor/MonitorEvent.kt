package com.example.consentmonitor
data class MonitorEvent(
  val type: String,
  val app: String? = null,
  val title: String? = null,
  val body: String? = null,
  val lat: Double? = null,
  val lon: Double? = null,
  val ts: Long = System.currentTimeMillis()
)
