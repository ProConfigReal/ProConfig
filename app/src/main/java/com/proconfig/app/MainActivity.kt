package com.proconfig.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.proconfig.app.databinding.ActivityMainBinding
import com.proconfig.app.service.ProVpnService
import com.proconfig.app.service.StateBus
import com.proconfig.app.ui.AboutFragment
import com.proconfig.app.ui.ConfigsFragment
import com.proconfig.app.ui.HomeFragment
import com.proconfig.app.ui.SettingsFragment
import com.proconfig.app.ui.SubscriptionsFragment
import com.proconfig.app.util.Prefs

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startVpnService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { switchTo(HomeFragment()); true }
                R.id.nav_configs -> { switchTo(ConfigsFragment()); true }
                R.id.nav_subs -> { switchTo(SubscriptionsFragment()); true }
                R.id.nav_settings -> { switchTo(SettingsFragment()); true }
                R.id.nav_about -> { switchTo(AboutFragment()); true }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            switchTo(HomeFragment())
            if (Prefs.autoConnect && StateBus.state.value == StateBus.DISCONNECTED) {
                requestConnect()
            }
        }
    }

    fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    fun navigateTo(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    fun requestConnect() {
        if (StateBus.state.value != StateBus.DISCONNECTED) {
            startService(
                Intent(this, ProVpnService::class.java).setAction(ProVpnService.ACTION_DISCONNECT)
            )
            return
        }
        try {
            val intent: Intent? = VpnService.prepare(this)
            if (intent != null) {
                vpnPermissionLauncher.launch(intent)
            } else {
                startVpnService()
            }
        } catch (e: Exception) {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, ProVpnService::class.java)
            .setAction(ProVpnService.ACTION_CONNECT)
        ContextCompat.startForegroundService(this, intent)
    }
}
