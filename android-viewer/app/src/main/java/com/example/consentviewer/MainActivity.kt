package com.example.consentviewer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File

data class MonitorEvent(
  val type:String?=null,
  val app:String?=null,
  val title:String?=null,
  val body:String?=null,
  val ts:Long?=null,
  val lat:Double?=null,
  val lon:Double?=null
)

class MainActivity: AppCompatActivity(){
  private lateinit var tvSummary: TextView
  private lateinit var tvEvents: TextView
  private var cache: List<MonitorEvent> = emptyList()

  override fun onCreate(b:Bundle?){
    super.onCreate(b)
    setContentView(R.layout.activity_main)
    tvSummary = findViewById(R.id.tvSummary)
    tvEvents = findViewById(R.id.tvEvents)
    val etDev = findViewById<EditText>(R.id.etDeviceId)
    val etDays = findViewById<EditText>(R.id.etDays)
    val btnLoad = findViewById<Button>(R.id.btnLoad)
    val btnCsv = findViewById<Button>(R.id.btnCsv)

    FirebaseApp.initializeApp(this)
    Firebase.auth.signInAnonymously()

    btnLoad.setOnClickListener {
      val dev = etDev.text.toString().trim()
      val days = etDays.text.toString().toIntOrNull() ?: 7
      if (dev.isEmpty()) return@setOnClickListener
      val since = System.currentTimeMillis() - days*24*3600_000L

      val db = Firebase.firestore
      db.collection("devices").document(dev).collection("events")
        .whereGreaterThanOrEqualTo("ts", since)
        .orderBy("ts", Query.Direction.DESCENDING)
        .limit(5000)
        .get().addOnSuccessListener { snap ->
          val ev = snap.documents.mapNotNull { it.toObject(MonitorEvent::class.java) }
          cache = ev
          val notif = ev.count { it.type == "notification" }
          val apps = ev.filter { it.type == "app_foreground" }.groupBy { it.app ?: "?" }.mapValues { it.value.size }
          val top = apps.entries.sortedByDescending { it.value }.take(5).joinToString { "${it.key}(${it.value})" }
          tvSummary.text = "Event: ${ev.size} • Notifikasi: ${notif} • TopApps: ${top}"
          tvEvents.text = ev.joinToString("\n") { e ->
            val t = e.ts?.let { java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(java.util.Date(it)) } ?: "-"
            when(e.type){
              "notification" -> "[${t}] 🔔 ${e.app} • ${e.title ?: ""} — ${e.body ?: ""}"
              "app_foreground" -> "[${t}] ▶️ ${e.app}"
              "location" -> "[${t}] 📍 ${e.lat}, ${e.lon}"
              else -> "[${t}] ${e.type}"
            }
          }
        }
    }

    btnCsv.setOnClickListener {
      if (cache.isEmpty()) return@setOnClickListener
      val sb = StringBuilder("ts,type,app,title,body,lat,lon\n")
      cache.forEach { e ->
        fun q(s:String?) = "\""+ (s ?: "").replace("\"","\"\"").replace("\n"," ").replace("\r"," ") + "\""
        sb.append("${e.ts ?: 0},${q(e.type)},${q(e.app)},${q(e.title)},${q(e.body)},${e.lat?.toString() ?: ""},${e.lon?.toString() ?: ""}\n")
      }
      val file = File(cacheDir, "events.csv")
      file.writeText(sb.toString())
      val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", file)
      val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      startActivity(Intent.createChooser(send, "Bagikan CSV"))
    }
  }
}
