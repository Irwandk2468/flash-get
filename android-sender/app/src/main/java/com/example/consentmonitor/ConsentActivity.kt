package com.example.consentmonitor

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ConsentActivity: AppCompatActivity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.consent_title))
      .setMessage(getString(R.string.consent_text))
      .setPositiveButton(getString(R.string.agree)) { d, _ -> ConsentStore.setConsented(this, true); d.dismiss(); finish() }
      .setNegativeButton(getString(R.string.disagree)) { d, _ -> ConsentStore.setConsented(this, false); d.dismiss(); finish() }
      .show()
  }
}
