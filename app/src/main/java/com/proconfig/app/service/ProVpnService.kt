package com.proconfig.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.proconfig.app.MainActivity
import com.proconfig.app.R
import com.proconfig.app.core.ConfigRepository
import com.proconfig.app.core.XrayConfigBuilder
import com.proconfig.app.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

class ProVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.proconfig.app.action.CONNECT"
        const val ACTION_DISCONNECT = "com.proconfig.app.action.DISCONNECT"
        private const val CHANNEL_ID = "proconfig_vpn"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ProVpnService"

        @Volatile
        var pidXray: Int = 0

        @Volatile
        var pidTun: Int = 0

        @Volatile
        var tunInterface: android.os.ParcelFileDescriptor? = null

        @Volatile
        var lastConfigName: String = ""
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var monitorActive = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }
        scope.launch { connect() }
        return START_STICKY
    }

    private suspend fun connect() {
        if (StateBus.state.value != StateBus.DISCONNECTED) return
        StateBus.setState(StateBus.CONNECTING)

        val config = ConfigRepository.selected(this)
        if (config == null) {
            StateBus.setState(StateBus.DISCONNECTED)
            stopSelf()
            return
        }
        lastConfigName = config.name

        try {
            val socksPort = Prefs.socksPort
            val httpPort = socksPort + 1
            val restPort = socksPort + 2

            val coreDir = File(filesDir, "core")
            if (!coreDir.exists()) coreDir.mkdirs()
            val configFile = File(coreDir, "config.json")
            configFile.writeText(XrayConfigBuilder.buildFullConfig(config, socksPort, httpPort))

            val tun = Builder()
                .setSession("ProConfig")
                .setMtu(Prefs.mtu)
                .addAddress("26.26.26.1", 24)
                .addDnsServer("26.26.26.2")
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDisallowedApplication(packageName)
                .setBlocking(false)
                .establish()

            if (tun == null) {
                Log.e(TAG, "establish() returned null")
                fail()
                return
            }
            tunInterface = tun

            val xrayBin = File(applicationInfo.nativeLibraryDir, "libxray.so")
            pidXray = ProcessRunner.start(
                xrayBin.absolutePath,
                arrayOf("run", "-c", configFile.absolutePath)
            )
            if (!ProcessRunner.isRunning(pidXray)) {
                Log.e(TAG, "xray failed to start")
                fail()
                return
            }

            if (!awaitPort(socksPort, 8000)) {
                Log.e(TAG, "xray socks port not reachable")
                fail()
                return
            }

            val t2sBin = File(applicationInfo.nativeLibraryDir, "libtun2socks.so")
            val t2sArgs = arrayOf(
                "--device", "fd://${tun.fd}",
                "--proxy", "socks5://127.0.0.1:$socksPort",
                "--mtu", Prefs.mtu.toString(),
                "--restapi", "127.0.0.1:$restPort",
                "--loglevel", "none"
            )
            pidTun = ProcessRunner.start(t2sBin.absolutePath, t2sArgs, intArrayOf(tun.fd))
            if (!ProcessRunner.isRunning(pidTun)) {
                Log.e(TAG, "tun2socks failed to start")
                fail()
                return
            }

            startVpnForeground()

            StateBus.setState(StateBus.CONNECTED)
            TrafficMonitor.start(restPort)

            monitorActive = true
            scope.launch(Dispatchers.IO) {
                while (monitorActive) {
                    if (!ProcessRunner.isRunning(pidXray) || !ProcessRunner.isRunning(pidTun)) {
                        if (monitorActive) {
                            Log.w(TAG, "core process exited")
                            disconnect()
                        }
                        return@launch
                    }
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        return@launch
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect error", e)
            fail()
        }
    }

    private fun fail() {
        cleanupCore()
        StateBus.setState(StateBus.DISCONNECTED)
        stopSelf()
    }

    private fun disconnect() {
        StateBus.setState(StateBus.DISCONNECTING)
        monitorActive = false
        TrafficMonitor.stop()
        cleanupCore()
        StateBus.setState(StateBus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupCore() {
        ProcessRunner.kill(pidTun)
        ProcessRunner.kill(pidXray)
        pidTun = 0
        pidXray = 0
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
    }

    private fun awaitPort(port: Int, timeoutMs: Int): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!ProcessRunner.isRunning(pidXray)) return false
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress("127.0.0.1", port), 500)
                }
                return true
            } catch (_: Exception) {
                try {
                    Thread.sleep(200)
                } catch (_: InterruptedException) {
                    return false
                }
            }
        }
        return false
    }

    private fun startVpnForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ProVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_connected))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_notify, getString(R.string.disconnect), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    override fun onDestroy() {
        monitorActive = false
        TrafficMonitor.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        super.onRevoke()
        disconnect()
    }
}
