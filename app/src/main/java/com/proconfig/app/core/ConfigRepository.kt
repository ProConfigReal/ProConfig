package com.proconfig.app.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.proconfig.app.util.Prefs

object ConfigRepository {

    private const val FILE_CONFIGS = "configs.json"
    private const val FILE_SUBS = "subscriptions.json"
    private val gson = Gson()

    private fun configsFile(context: Context) = context.getFileStreamPath(FILE_CONFIGS)
    private fun subsFile(context: Context) = context.getFileStreamPath(FILE_SUBS)

    @Synchronized
    fun loadConfigs(context: Context): MutableList<ProxyConfig> {
        return try {
            val f = configsFile(context)
            if (!f.exists()) mutableListOf()
            else {
                val type = object : TypeToken<MutableList<ProxyConfig>>() {}.type
                gson.fromJson(f.readText(), type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun saveConfigs(context: Context, list: List<ProxyConfig>) {
        try {
            configsFile(context).writeText(gson.toJson(list))
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun loadSubscriptions(context: Context): MutableList<Subscription> {
        return try {
            val f = subsFile(context)
            if (!f.exists()) mutableListOf()
            else {
                val type = object : TypeToken<MutableList<Subscription>>() {}.type
                gson.fromJson(f.readText(), type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun saveSubscriptions(context: Context, list: List<Subscription>) {
        try {
            subsFile(context).writeText(gson.toJson(list))
        } catch (_: Exception) {
        }
    }

    fun addConfig(context: Context, c: ProxyConfig) {
        val list = loadConfigs(context)
        list.add(0, c)
        saveConfigs(context, list)
    }

    fun updateConfig(context: Context, c: ProxyConfig) {
        val list = loadConfigs(context)
        val idx = list.indexOfFirst { it.id == c.id }
        if (idx >= 0) {
            val latency = list[idx].latency
            list[idx] = c
            if (c.latency < 0) list[idx].latency = latency
            saveConfigs(context, list)
        }
    }

    fun deleteConfig(context: Context, id: String) {
        val list = loadConfigs(context)
        list.removeAll { it.id == id }
        saveConfigs(context, list)
        if (Prefs.selectedId == id) Prefs.selectedId = ""
    }

    fun selected(context: Context): ProxyConfig? {
        val id = Prefs.selectedId
        if (id.isEmpty()) return null
        return loadConfigs(context).firstOrNull { it.id == id }
    }

    fun replaceSubConfigs(context: Context, sub: Subscription, fresh: List<ProxyConfig>): Int {
        val list = loadConfigs(context)
        val old = list.filter { it.subId == sub.id }
        list.removeAll { it.subId == sub.id }
        for (nf in fresh) {
            val prev = old.firstOrNull {
                it.proto == nf.proto && it.address == nf.address && it.port == nf.port &&
                        it.uuid == nf.uuid && it.password == nf.password
            }
            if (prev != null) nf.latency = prev.latency
        }
        list.addAll(fresh)
        saveConfigs(context, list)
        sub.count = fresh.size
        sub.lastUpdate = System.currentTimeMillis()
        val subs = loadSubscriptions(context)
        val idx = subs.indexOfFirst { it.id == sub.id }
        if (idx >= 0) {
            subs[idx] = sub
            saveSubscriptions(context, subs)
        }
        return fresh.size
    }

    fun deleteSubscription(context: Context, sub: Subscription) {
        val list = loadConfigs(context)
        list.removeAll { it.subId == sub.id }
        saveConfigs(context, list)
        val subs = loadSubscriptions(context)
        subs.removeAll { it.id == sub.id }
        saveSubscriptions(context, subs)
    }
}
