package com.proconfig.app.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object TrafficMonitor {

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    @Synchronized
    fun start(restPort: Int) {
        stop()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        job = s.launch {
            var conn: HttpURLConnection? = null
            try {
                delay(1500)
                val url = URL("http://127.0.0.1:$restPort/traffic")
                while (isActive) {
                    try {
                        conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 15000
                        conn.requestMethod = "GET"
                        val code = conn.responseCode
                        if (code == 200) {
                            val reader = BufferedReader(InputStreamReader(conn.inputStream))
                            var line: String? = null
                            while (isActive && reader.readLine().also { line = it } != null) {
                                val l = line ?: continue
                                val trimmed = l.trim()
                                if (trimmed.isEmpty()) continue
                                try {
                                    val o = org.json.JSONObject(trimmed)
                                    val up = o.optLong("up", 0)
                                    val down = o.optLong("down", 0)
                                    StateBus.postSpeed(up, down)
                                } catch (_: Exception) {
                                }
                            }
                            reader.close()
                        }
                    } catch (_: Exception) {
                    }
                    try {
                        conn?.disconnect()
                    } catch (_: Exception) {
                    }
                    if (isActive) delay(3000)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        try {
            job?.cancel()
        } catch (_: Exception) {
        }
        try {
            scope?.cancel()
        } catch (_: Exception) {
        }
        job = null
        scope = null
    }
}
