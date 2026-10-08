package com.proconfig.app.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.getSharedPreferences("proconfig_prefs", Context.MODE_PRIVATE)
        }
    }

    var socksPort: Int
        get() = sp.getInt("socks_port", 10808)
        set(value) = sp.edit().putInt("socks_port", value).apply()

    var mtu: Int
        get() = sp.getInt("mtu", 8500)
        set(value) = sp.edit().putInt("mtu", value).apply()

    var themeMode: String
        get() = sp.getString("theme_mode", "dark") ?: "dark"
        set(value) = sp.edit().putString("theme_mode", value).apply()

    var language: String
        get() = sp.getString("language", "") ?: ""
        set(value) = sp.edit().putString("language", value).apply()

    var autoConnect: Boolean
        get() = sp.getBoolean("auto_connect", false)
        set(value) = sp.edit().putBoolean("auto_connect", value).apply()

    var notificationSpeed: Boolean
        get() = sp.getBoolean("notification_speed", true)
        set(value) = sp.edit().putBoolean("notification_speed", value).apply()

    var selectedId: String
        get() = sp.getString("selected_id", "") ?: ""
        set(value) = sp.edit().putString("selected_id", value).apply()
}
