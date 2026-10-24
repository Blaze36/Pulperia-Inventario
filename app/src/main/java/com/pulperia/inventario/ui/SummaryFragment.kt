package com.pulperia.inventario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pulperia.inventario.Fmt
import com.pulperia.inventario.MainActivity
import com.pulperia.inventario.R
import com.pulperia.inventario.data.DbHelper
import com.pulperia.inventario.databinding.FragmentSummaryBinding
import com.pulperia.inventario.databinding.ItemSummaryRowBinding

/** Pantalla 3: valor total del inventario, top 5 por valor y alertas de stock bajo. */
class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DbHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DbHelper.get(requireContext())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        // Valor total = suma de price * stock.
        binding.tvTotal.text = Fmt.money(db.getTotalValue())
        val (productCount, unitCount) = db.getStats()
        binding.tvStats.text = getString(R.string.stats_line, productCount, unitCount)

        // Top 5 productos con mayor valor.
        binding.listTop.removeAllViews()
        val top = db.getTopProducts(5)
        binding.tvEmptyTop.isVisible = top.isEmpty()
        top.forEachIndexed { index, p ->
            val row = ItemSummaryRowBinding.inflate(layoutInflater, binding.listTop, false)
            row.tvRank.text = (index + 1).toString()
            row.tvName.text = p.name
            row.tvDetail.text = getString(R.string.top_detail, Fmt.money(p.price), p.stock)
            row.tvValue.text = Fmt.money(p.value)
            binding.listTop.addView(row.root)
        }

        // Productos que llegaron a su stock mínimo.
        binding.listLow.removeAllViews()
        val low = db.getLowStockProducts()
        binding.cardLow.isVisible = low.isNotEmpty()
        val danger = ContextCompat.getColor(requireContext(), R.color.danger)
        low.forEach { p ->
            val row = ItemSummaryRowBinding.inflate(layoutInflater, binding.listLow, false)
            row.tvRank.text = "!"
            row.tvRank.setTextColor(danger)
            row.tvName.text = p.name
            row.tvDetail.text = getString(R.string.low_detail, p.stock, p.minStock)
            row.tvValue.text = p.stock.toString()
            row.tvValue.setTextColor(danger)
            binding.listLow.addView(row.root)
        }

        (activity as? MainActivity)?.refreshAlertBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
