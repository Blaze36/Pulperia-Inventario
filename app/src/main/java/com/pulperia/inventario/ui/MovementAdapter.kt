package com.pulperia.inventario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pulperia.inventario.R
import com.pulperia.inventario.data.Movement
import com.pulperia.inventario.data.MovementType
import com.pulperia.inventario.databinding.ItemMovementBinding

class MovementAdapter : ListAdapter<Movement, MovementAdapter.VH>(Diff) {

    class VH(val binding: ItemMovementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMovementBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        val ctx = holder.binding.root.context
        val isIn = m.type == MovementType.IN
        val color = ContextCompat.getColor(ctx, if (isIn) R.color.success else R.color.danger)
        with(holder.binding) {
            tvProduct.text = m.productName
            tvDate.text = m.date
            tvQty.text = if (isIn) "+${m.qty}" else "-${m.qty}"
            tvQty.setTextColor(color)
            tvType.text = ctx.getString(if (isIn) R.string.type_in else R.string.type_out)
            tvType.setTextColor(color)
        }
    }

    private object Diff : DiffUtil.ItemCallback<Movement>() {
        override fun areItemsTheSame(old: Movement, new: Movement) = old.id == new.id
        override fun areContentsTheSame(old: Movement, new: Movement) = old == new
    }
}
