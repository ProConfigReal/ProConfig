package com.proconfig.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.proconfig.app.R
import com.proconfig.app.core.ConfigParser
import com.proconfig.app.core.ConfigRepository
import com.proconfig.app.core.ProxyConfig
import com.proconfig.app.databinding.FragmentConfigsBinding
import com.proconfig.app.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class ConfigsFragment : Fragment() {

    private var _binding: FragmentConfigsBinding? = null
    private val b get() = _binding!!
    private lateinit var adapter: ConfigAdapter
    private var configs: List<ProxyConfig> = emptyList()
    private var testAllJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ConfigAdapter(
            emptyList(),
            onMenuClick = { cfg, anchor -> showMenu(cfg, anchor) },
            onItemClick = { cfg ->
                Prefs.selectedId = cfg.id
                refresh()
                Toast.makeText(context, R.string.toast_selected, Toast.LENGTH_SHORT).show()
            }
        )
        b.recycler.layoutManager = LinearLayoutManager(context)
        b.recycler.adapter = adapter

        b.swipe.setOnRefreshListener {
            refresh()
            b.swipe.isRefreshing = false
        }

        b.btnAdd.setOnClickListener {
            ConfigEditDialogFragment(null) { refresh() }
                .show(parentFragmentManager, "edit")
        }

        b.btnTestAll.setOnClickListener { testAll() }

        b.search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterList()
            }
        })

        refresh()
    }

    private fun refresh() {
        configs = context?.let { ConfigRepository.loadConfigs(it) } ?: emptyList()
        filterList()
    }

    private fun filterList() {
        val q = b.search.text?.toString()?.trim() ?: ""
        val filtered = if (q.isEmpty()) configs
        else configs.filter {
            it.name.contains(q, true) || it.address.contains(q, true)
        }
        adapter.submit(filtered)
        b.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        b.recycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showMenu(cfg: ProxyConfig, anchor: View) {
        val pm = PopupMenu(requireContext(), anchor)
        pm.menuInflater.inflate(R.menu.menu_config_item, pm.menu)
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_test -> { testOne(cfg); true }
                R.id.action_edit -> {
                    ConfigEditDialogFragment(cfg) { refresh() }
                        .show(parentFragmentManager, "edit")
                    true
                }
                R.id.action_share -> {
                    share(ConfigParser.exportLink(cfg))
                    true
                }
                R.id.action_copy -> {
                    copyToClipboard(ConfigParser.exportLink(cfg))
                    true
                }
                R.id.action_delete -> {
                    context?.let { ConfigRepository.deleteConfig(it, cfg.id) }
                    refresh()
                    true
                }
                else -> false
            }
        }
        pm.show()
    }

    private fun share(link: String) {
        if (link.isEmpty()) return
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(i, getString(R.string.share_via)))
    }

    private fun copyToClipboard(text: String) {
        if (text.isEmpty()) {
            Toast.makeText(context, R.string.toast_error, Toast.LENGTH_SHORT).show()
            return
        }
        val cm = requireContext().getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("config", text))
        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun testOne(cfg: ProxyConfig) {
        viewLifecycleOwner.lifecycleScope.launch {
            val latency = withContext(Dispatchers.IO) { tcpPing(cfg) }
            cfg.latency = latency
            context?.let { ConfigRepository.updateConfig(it, cfg) }
            refresh()
        }
    }

    private fun testAll() {
        if (testAllJob?.isActive == true) return
        b.progressTest.visibility = View.VISIBLE
        testAllJob = viewLifecycleOwner.lifecycleScope.launch {
            val list = configs
            withContext(Dispatchers.IO) {
                list.map { cfg ->
                    async {
                        val l = tcpPing(cfg)
                        cfg.latency = l
                    }
                }.awaitAll()
            }
            context?.let { ctx ->
                val repo = ConfigRepository.loadConfigs(ctx)
                for (c in repo) {
                    val updated = list.firstOrNull { it.id == c.id }
                    if (updated != null) c.latency = updated.latency
                }
                ConfigRepository.saveConfigs(ctx, repo)
            }
            refresh()
            b.progressTest.visibility = View.GONE
        }
    }

    private fun tcpPing(cfg: ProxyConfig): Int {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { s ->
                s.connect(InetSocketAddress(cfg.address, cfg.port), 3000)
            }
            (System.currentTimeMillis() - start).toInt()
        } catch (e: Exception) {
            0
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testAllJob?.cancel()
        _binding = null
    }
}
