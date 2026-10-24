package com.pulperia.inventario.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pulperia.inventario.MainActivity
import com.pulperia.inventario.R
import com.pulperia.inventario.data.DbHelper
import com.pulperia.inventario.data.InventoryException
import com.pulperia.inventario.data.MovementType
import com.pulperia.inventario.data.Product
import com.pulperia.inventario.databinding.FragmentMovementsBinding

/** Pantalla 2: registrar entradas y salidas, y ver el historial reciente. */
class MovementsFragment : Fragment() {

    private var _binding: FragmentMovementsBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DbHelper
    private lateinit var historyAdapter: MovementAdapter

    private var products: List<Product> = emptyList()
    private var selected: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DbHelper.get(requireContext())

        historyAdapter = MovementAdapter()
        binding.listHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.listHistory.adapter = historyAdapter

        binding.toggleType.check(R.id.btnIn)

        binding.dropdownProduct.setOnItemClickListener { _, _, position, _ ->
            selectProduct(products[position])
        }

        // Búsqueda por código de barras escrito a mano.
        binding.barcodeLayout.setEndIconOnClickListener { searchByBarcode() }
        binding.etBarcode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByBarcode()
                true
            } else {
                false
            }
        }
        binding.etBarcode.doAfterTextChanged { binding.barcodeLayout.error = null }
        binding.etQty.doAfterTextChanged { binding.qtyLayout.error = null }

        binding.btnRegister.setOnClickListener { register() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    /** Recarga productos, historial y vuelve a resolver el producto seleccionado. */
    private fun reload() {
        products = db.getProducts()

        binding.dropdownProduct.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                products.map { label(it) }
            )
        )

        selected = selected?.let { current -> products.find { it.id == current.id } }
        selected?.let { binding.dropdownProduct.setText(label(it), false) }
            ?: binding.dropdownProduct.setText("", false)
        updateStockInfo()

        val history = db.getMovements()
        historyAdapter.submitList(history)
        binding.tvEmptyHistory.isVisible = history.isEmpty()

        (activity as? MainActivity)?.refreshAlertBadge()
    }

    private fun label(p: Product): String =
        if (p.barcode.isNullOrBlank()) p.name else "${p.name} (${p.barcode})"

    private fun selectProduct(p: Product) {
        selected = p
        binding.dropdownProduct.setText(label(p), false)
        binding.productLayout.error = null
        updateStockInfo()
    }

    private fun updateStockInfo() {
        val p = selected
        val info = binding.tvStockInfo
        if (p == null) {
            info.text = getString(R.string.select_product_first)
            info.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary_container))
        } else if (p.isLow) {
            info.text = getString(R.string.stock_current_low, p.stock, p.minStock)
            info.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
        } else {
            info.text = getString(R.string.stock_current, p.stock)
            info.setTextColor(Color.DKGRAY)
        }
    }

    private fun searchByBarcode() {
        val code = binding.etBarcode.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) return

        val product = db.getProductByBarcode(code)
        if (product == null) {
            binding.barcodeLayout.error = getString(R.string.err_barcode_not_found)
        } else {
            binding.barcodeLayout.error = null
            selectProduct(product)
            binding.etBarcode.text?.clear()
            binding.etQty.requestFocus()
        }
    }

    private fun register() {
        val product = selected
        if (product == null) {
            binding.productLayout.error = getString(R.string.select_product_first)
            return
        }

        val qty = binding.etQty.text?.toString()?.trim()?.toIntOrNull()
        if (qty == null || qty <= 0) {
            binding.qtyLayout.error = getString(R.string.err_qty)
            return
        }

        val type = if (binding.toggleType.checkedButtonId == R.id.btnOut) {
            MovementType.OUT
        } else {
            MovementType.IN
        }

        try {
            val updated = db.registerMovement(product.id, qty, type)
            binding.etQty.text?.clear()
            reload()
            Toast.makeText(requireContext(), R.string.movement_saved, Toast.LENGTH_SHORT).show()
            // Alerta: una salida dejó el producto en su stock mínimo (o por debajo).
            if (type == MovementType.OUT && updated.isLow) showLowStockAlert(updated)
        } catch (e: InventoryException) {
            binding.qtyLayout.error = e.message
        }
    }

    private fun showLowStockAlert(p: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.low_stock_title)
            .setMessage(getString(R.string.low_stock_msg, p.name, p.stock, p.minStock))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
