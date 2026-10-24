package com.pulperia.inventario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pulperia.inventario.Fmt
import com.pulperia.inventario.R
import com.pulperia.inventario.data.Product
import com.pulperia.inventario.databinding.ItemProductBinding

class ProductAdapter(
    private val onClick: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(Diff) {

    class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val ctx = holder.binding.root.context
        with(holder.binding) {
            tvName.text = p.name
            tvPrice.text = ctx.getString(R.string.price_line, Fmt.money(p.price))
            tvBarcode.text = if (p.barcode.isNullOrBlank()) {
                ctx.getString(R.string.no_barcode)
            } else {
                ctx.getString(R.string.barcode_line, p.barcode)
            }
            tvStock.text = p.stock.toString()
            tvStock.setTextColor(
                if (p.isLow) ContextCompat.getColor(ctx, R.color.danger)
                else ContextCompat.getColor(ctx, R.color.primary)
            )
            tvLow.isVisible = p.isLow
            tvLow.text = ctx.getString(R.string.low_stock_badge, p.minStock)

            root.setOnClickListener { onClick(p) }
            btnDelete.setOnClickListener { onDelete(p) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
        override fun areContentsTheSame(old: Product, new: Product) = old == new
    }
}
