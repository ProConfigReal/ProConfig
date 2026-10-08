package com.proconfig.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.proconfig.app.MainActivity
import com.proconfig.app.R
import com.proconfig.app.core.ConfigRepository
import com.proconfig.app.databinding.FragmentHomeBinding
import com.proconfig.app.service.ProVpnService
import com.proconfig.app.service.StateBus
import com.proconfig.app.util.Prefs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val b get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateDuration()
            handler.postDelayed(this, 1000)
        }
    }

    private val stateObserver = Observer<Int> { updateStateUi(it) }
    private val speedUpObserver = Observer<Long> { b.tvSpeedUp.text = formatSpeed(it) }
    private val speedDownObserver = Observer<Long> { b.tvSpeedDown.text = formatSpeed(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnConnect.setOnClickListener {
            (activity as? MainActivity)?.requestConnect()
        }

        b.cardSelected.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_configs)
        }

        StateBus.state.observe(viewLifecycleOwner, stateObserver)
        StateBus.speedUp.observe(viewLifecycleOwner, speedUpObserver)
        StateBus.speedDown.observe(viewLifecycleOwner, speedDownObserver)

        updateStateUi(StateBus.state.value ?: StateBus.DISCONNECTED)
        refreshSelected()
    }

    override fun onResume() {
        super.onResume()
        refreshSelected()
        updateStateUi(StateBus.state.value ?: StateBus.DISCONNECTED)
    }

    private fun refreshSelected() {
        val cfg = context?.let { ConfigRepository.selected(it) }
        if (cfg == null) {
            b.tvConfigName.text = getString(R.string.home_no_selection)
            b.tvProto.text = "-"
            b.tvServer.text = getString(R.string.home_pick_hint)
        } else {
            b.tvConfigName.text = cfg.name
            b.tvProto.text = cfg.protoLabel()
            b.tvServer.text = cfg.displayAddress()
        }
    }

    private fun updateStateUi(state: Int) {
        val ctx = context ?: return
        when (state) {
            StateBus.CONNECTED -> {
                b.tvStatus.text = getString(R.string.state_connected)
                b.dotStatus.setBackgroundResource(R.drawable.dot_connected)
                b.tvStatusHint.text = getString(R.string.home_connected_hint)
                b.btnConnect.isEnabled = true
                b.btnConnect.alpha = 1f
                b.btnConnect.setImageResource(R.drawable.ic_stop)
                b.trafficGroup.visibility = View.VISIBLE
                handler.removeCallbacks(timerRunnable)
                handler.post(timerRunnable)
                ProVpnService.lastConfigName.let {
                    if (it.isNotEmpty()) b.tvStatusHint.text = it
                }
            }
            StateBus.CONNECTING -> {
                b.tvStatus.text = getString(R.string.state_connecting)
                b.dotStatus.setBackgroundResource(R.drawable.dot_connecting)
                b.tvStatusHint.text = getString(R.string.home_connecting_hint)
                b.btnConnect.isEnabled = false
                b.btnConnect.alpha = 0.6f
                b.btnConnect.setImageResource(R.drawable.ic_stop)
                b.trafficGroup.visibility = View.GONE
                handler.removeCallbacks(timerRunnable)
            }
            StateBus.DISCONNECTING -> {
                b.tvStatus.text = getString(R.string.state_disconnecting)
                b.dotStatus.setBackgroundResource(R.drawable.dot_disconnected)
                b.btnConnect.isEnabled = false
                b.btnConnect.alpha = 0.6f
                b.trafficGroup.visibility = View.GONE
                handler.removeCallbacks(timerRunnable)
            }
            else -> {
                b.tvStatus.text = getString(R.string.state_disconnected)
                b.dotStatus.setBackgroundResource(R.drawable.dot_disconnected)
                b.tvStatusHint.text = getString(R.string.home_disconnected_hint)
                b.btnConnect.isEnabled = true
                b.btnConnect.alpha = 1f
                b.btnConnect.setImageResource(R.drawable.ic_power)
                b.trafficGroup.visibility = View.GONE
                b.tvDuration.text = "00:00:00"
                b.tvSpeedUp.text = formatSpeed(0)
                b.tvSpeedDown.text = formatSpeed(0)
                b.tvTotalUp.text = formatTotal(0)
                b.tvTotalDown.text = formatTotal(0)
                handler.removeCallbacks(timerRunnable)
            }
        }
    }

    private fun updateDuration() {
        val start = StateBus.connectedAt.get()
        if (start <= 0) return
        val sec = ((System.currentTimeMillis() - start) / 1000).toInt()
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        b.tvDuration.text = String.format("%02d:%02d:%02d", h, m, s)
        b.tvTotalUp.text = formatTotal(StateBus.totalUp.get())
        b.tvTotalDown.text = formatTotal(StateBus.totalDown.get())
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        val kb = bytesPerSec / 1024.0
        if (kb < 1024) return String.format("%.1f KB/s", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.2f MB/s", mb)
        return String.format("%.2f GB/s", mb / 1024.0)
    }

    private fun formatTotal(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
        StateBus.state.removeObserver(stateObserver)
        StateBus.speedUp.removeObserver(speedUpObserver)
        StateBus.speedDown.removeObserver(speedDownObserver)
        _binding = null
    }
}
