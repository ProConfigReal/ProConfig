package com.proconfig.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.proconfig.app.R
import com.proconfig.app.core.Subscription
import com.proconfig.app.databinding.ItemSubscriptionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubsAdapter(
    private var items: List<Subscription>,
    private val onUpdateClick: (Subscription) -> Unit,
    private val onDeleteClick: (Subscription) -> Unit
) : RecyclerView.Adapter<SubsAdapter.VH>() {

    inner class VH(val b: ItemSubscriptionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        val ctx = holder.b.root.context
        holder.b.tvName.text = s.name
        holder.b.tvUrl.text = s.url
        holder.b.tvCount.text = ctx.getString(R.string.sub_count_fmt, s.count)
        if (s.lastUpdate > 0) {
            val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            holder.b.tvUpdated.text = fmt.format(Date(s.lastUpdate))
        } else {
            holder.b.tvUpdated.text = ctx.getString(R.string.sub_never_updated)
        }
        holder.b.btnUpdate.setOnClickListener { onUpdateClick(s) }
        holder.b.btnDelete.setOnClickListener { onDeleteClick(s) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<Subscription>) {
        items = list
        notifyDataSetChanged()
    }
}
