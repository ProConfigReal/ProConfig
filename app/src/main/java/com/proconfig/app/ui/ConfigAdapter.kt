package com.proconfig.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.proconfig.app.R
import com.proconfig.app.core.ProxyConfig
import com.proconfig.app.databinding.ItemConfigBinding
import com.proconfig.app.util.Prefs

class ConfigAdapter(
    private var items: List<ProxyConfig>,
    private val onMenuClick: (config: ProxyConfig, view: View) -> Unit,
    private val onItemClick: (config: ProxyConfig) -> Unit
) : RecyclerView.Adapter<ConfigAdapter.VH>() {

    inner class VH(val b: ItemConfigBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        val ctx = holder.b.root.context
        holder.b.tvName.text = c.name
        holder.b.tvAddress.text = c.displayAddress()
        holder.b.tvProto.text = c.protoLabel()

        if (c.id == Prefs.selectedId) {
            holder.b.root.strokeWidth = dp(ctx, 2)
            holder.b.root.strokeColor = ctx.getColor(R.color.primary)
            holder.b.imgSelected.visibility = View.VISIBLE
        } else {
            holder.b.root.strokeWidth = dp(ctx, 1)
            holder.b.root.strokeColor = ctx.getColor(R.color.glass_stroke)
            holder.b.imgSelected.visibility = View.GONE
        }

        val latencyText: String
        val latencyColor: Int
        if (c.latency < 0) {
            latencyText = ctx.getString(R.string.latency_untested)
            latencyColor = ctx.getColor(R.color.text_secondary)
        } else if (c.latency == 0) {
            latencyText = ctx.getString(R.string.latency_timeout)
            latencyColor = ctx.getColor(R.color.latency_bad)
        } else if (c.latency < 300) {
            latencyText = c.latency.toString() + " ms"
            latencyColor = ctx.getColor(R.color.latency_good)
        } else {
            latencyText = c.latency.toString() + " ms"
            latencyColor = ctx.getColor(R.color.latency_mid)
        }
        holder.b.tvLatency.text = latencyText
        holder.b.tvLatency.setTextColor(latencyColor)

        holder.b.btnMenu.setOnClickListener { v -> onMenuClick(c, v) }
        holder.b.root.setOnClickListener { _ -> onItemClick(c) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<ProxyConfig>) {
        items = list
        notifyDataSetChanged()
    }

    private fun dp(ctx: android.content.Context, v: Int): Int =
        (v * ctx.resources.displayMetrics.density).toInt()
}
