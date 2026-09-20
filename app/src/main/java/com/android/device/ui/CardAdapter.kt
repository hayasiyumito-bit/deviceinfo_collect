package com.android.device.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.device.R
import com.google.android.material.card.MaterialCardView

/** 卡片列表项：风险总览头卡，或普通信息卡（标题 + 多行键值）。 */
sealed class CardItem {
    data class Header(val verdict: String, val sub: String, val danger: Boolean) : CardItem()
    data class Body(val title: String, val rows: List<Row>) : CardItem()

    data class Row(val label: String, val value: String, val fullValue: String, val key: String = "")
}

/**
 * 统一卡片适配器：风控页与各通用信息页共用。
 * 普通卡内的行动态 inflate 到卡片的 LinearLayout 中，点击行回调 [onRowClick] 看全文。
 */
class CardAdapter(
    private val onRowClick: (CardItem.Row) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<CardItem>()

    fun submit(newItems: List<CardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is CardItem.Header) TYPE_HEADER else TYPE_BODY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_card_header, parent, false))
        } else {
            BodyVH(inflater.inflate(R.layout.item_info_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CardItem.Header -> (holder as HeaderVH).bind(item)
            is CardItem.Body -> (holder as BodyVH).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val icon: TextView = view.findViewById(R.id.header_icon)
        val verdict: TextView = view.findViewById(R.id.header_verdict)
        val sub: TextView = view.findViewById(R.id.header_sub)

        fun bind(item: CardItem.Header) {
            val ctx = itemView.context
            val strong = ContextCompat.getColor(ctx, if (item.danger) R.color.risk_danger else R.color.risk_safe)
            val container = ContextCompat.getColor(ctx, if (item.danger) R.color.risk_danger_container else R.color.risk_safe_container)
            card.setCardBackgroundColor(container)
            icon.backgroundTintList = ColorStateList.valueOf(strong)
            icon.text = if (item.danger) "!" else "✓"
            icon.setTextColor(0xFFFFFFFF.toInt())
            verdict.text = item.verdict
            verdict.setTextColor(strong)
            sub.text = item.sub
            sub.setTextColor(strong)
        }
    }

    private inner class BodyVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.card_title)
        val rows: LinearLayout = view.findViewById(R.id.card_rows)

        fun bind(item: CardItem.Body) {
            title.text = item.title
            rows.removeAllViews()
            val inflater = LayoutInflater.from(rows.context)
            for (row in item.rows) {
                val rowView = inflater.inflate(R.layout.item_info_row, rows, false)
                rowView.findViewById<TextView>(R.id.row_label).text = row.label
                rowView.findViewById<TextView>(R.id.row_value).text = row.value
                rowView.setOnClickListener { onRowClick(row) }
                rows.addView(rowView)
            }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_BODY = 1
    }
}
