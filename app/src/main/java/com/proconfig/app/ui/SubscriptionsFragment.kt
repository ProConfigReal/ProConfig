package com.proconfig.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.proconfig.app.R
import com.proconfig.app.core.ConfigRepository
import com.proconfig.app.core.ConfigParser
import com.proconfig.app.core.ProxyConfig
import com.proconfig.app.core.Subscription
import com.proconfig.app.core.SubscriptionManager
import com.proconfig.app.databinding.DialogSubAddBinding
import com.proconfig.app.databinding.FragmentSubsBinding
import kotlinx.coroutines.launch

class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubsBinding? = null
    private val b get() = _binding!!
    private lateinit var adapter: SubsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SubsAdapter(
            emptyList(),
            onUpdateClick = { sub -> updateSub(sub) },
            onDeleteClick = { sub -> confirmDelete(sub) }
        )
        b.recycler.layoutManager = LinearLayoutManager(context)
        b.recycler.adapter = adapter

        b.btnAdd.setOnClickListener { showAddDialog() }
        refresh()
    }

    private fun refresh() {
        val subs = context?.let { ConfigRepository.loadSubscriptions(it) } ?: emptyList()
        adapter.submit(subs)
        b.tvEmpty.visibility = if (subs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val dialogBinding = DialogSubAddBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sub_add_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.etName.text?.toString()?.trim() ?: ""
                val url = dialogBinding.etUrl.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val sub = Subscription(
                        id = ProxyConfig.newId(),
                        name = name,
                        url = url
                    )
                    context?.let {
                        val subs = ConfigRepository.loadSubscriptions(it)
                        subs.add(sub)
                        ConfigRepository.saveSubscriptions(it, subs)
                    }
                    refresh()
                    updateSub(sub)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateSub(sub: Subscription) {
        b.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = SubscriptionManager.fetch(sub.url)
            b.progress.visibility = View.GONE
            result.onSuccess { configs ->
                val withSub = configs.map { it.subId = sub.id; it }
                val count = context?.let { ConfigRepository.replaceSubConfigs(it, sub, withSub) } ?: 0
                Toast.makeText(context, getString(R.string.sub_updated_fmt, count), Toast.LENGTH_SHORT).show()
                refresh()
            }.onFailure { e ->
                Toast.makeText(
                    context,
                    getString(R.string.sub_update_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmDelete(sub: Subscription) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sub_delete_title)
            .setMessage(getString(R.string.sub_delete_msg, sub.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                context?.let { ConfigRepository.deleteSubscription(it, sub) }
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
