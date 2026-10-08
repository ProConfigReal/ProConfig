package com.proconfig.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SubscriptionManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetch(url: String): Result<List<ProxyConfig>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ProConfig/1.0")
                .build()
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                }
                val body = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val content = maybeBase64(body)
                val configs = ArrayList<ProxyConfig>()
                for (line in content.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val cfg = ConfigParser.parse(trimmed)
                    if (cfg != null) configs.add(cfg)
                }
                if (configs.isEmpty()) {
                    return@withContext Result.failure(Exception("No valid profiles found"))
                }
                Result.success(configs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun maybeBase64(content: String): String {
        if (content.contains("://")) return content
        return try {
            val cleaned = content.replace("-", "+").replace("_", "/").replace("\n", "").replace("\r", "").trim()
            val padded = when (cleaned.length % 4) {
                2 -> "$cleaned=="
                3 -> "$cleaned="
                else -> cleaned
            }
            String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            content
        }
    }
}
