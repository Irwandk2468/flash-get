package com.example.consentmonitor
object MonitorLogger { private var cb: ((String)->Unit)? = null; fun attach(c: (String)->Unit){ cb = c }; fun log(s: String){ cb?.invoke(s) } }
