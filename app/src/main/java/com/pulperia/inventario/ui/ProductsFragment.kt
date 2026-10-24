package com.pulperia.inventario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pulperia.inventario.MainActivity
import com.pulperia.inventario.R
import com.pulperia.inventario.data.DbHelper
import com.pulperia.inventario.data.InventoryException
import com.pulperia.inventario.data.Product
import com.pulperia.inventario.databinding.DialogProductBinding
import com.pulperia.inventario.databinding.FragmentProductsBinding

/** Pantalla 1: CRUD de productos (crear, leer, actualizar, eliminar). */
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DbHelper
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DbHelper.get(requireContext())

        adapter = ProductAdapter(
            onClick = { showForm(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.fabAdd.setOnClickListener { showForm(null) }
        binding.etSearch.doAfterTextChanged { load() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val items = db.getProducts(binding.etSearch.text?.toString())
        adapter.submitList(items)
        binding.tvEmpty.isVisible = items.isEmpty()
        (activity as? MainActivity)?.refreshAlertBadge()
    }

    /** Formulario para crear (product == null) o editar un producto. */
    private fun showForm(product: Product?) {
        val form = DialogProductBinding.inflate(layoutInflater)
        val editing = product != null

        if (product != null) {
            form.etName.setText(product.name)
            form.etPrice.setText(product.price.toString())
            form.etBarcode.setText(product.barcode.orEmpty())
            form.etMin.setText(product.minStock.toString())
            // El stock solo cambia con movimientos, para que el historial siempre cuadre.
            form.stockLayout.hint = getString(R.string.hint_stock_locked)
            form.stockLayout.helperText = getString(R.string.stock_locked_helper)
            form.etStock.setText(product.stock.toString())
            form.etStock.isEnabled = false
        } else {
            form.etMin.setText(Product.DEFAULT_MIN_STOCK.toString())
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editing) R.string.edit_product else R.string.new_product)
            .setView(form.root)
            .setPositiveButton(R.string.save, null) // se asigna abajo para no cerrar con errores
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (save(form, product)) dialog.dismiss()
            }
        }
        dialog.show()
    }

    /** Valida el formulario y guarda. Devuelve true si se guardó. */
    private fun save(form: DialogProductBinding, existing: Product?): Boolean {
        form.nameLayout.error = null
        form.priceLayout.error = null
        form.stockLayout.error = null
        form.barcodeLayout.error = null
        form.minLayout.error = null

        val name = form.etName.text?.toString()?.trim().orEmpty()
        val price = form.etPrice.text?.toString()?.trim()?.toDoubleOrNull()
        val stock = if (existing != null) existing.stock
        else form.etStock.text?.toString()?.trim()?.toIntOrNull()
        val barcode = form.etBarcode.text?.toString()?.trim().orEmpty().ifEmpty { null }
        val minText = form.etMin.text?.toString()?.trim().orEmpty()
        val minStock = if (minText.isEmpty()) Product.DEFAULT_MIN_STOCK else minText.toIntOrNull()

        var valid = true
        if (name.isEmpty()) {
            form.nameLayout.error = getString(R.string.err_name); valid = false
        }
        if (price == null || price < 0) {
            form.priceLayout.error = getString(R.string.err_price); valid = false
        }
        if (stock == null || stock < 0) {
            form.stockLayout.error = getString(R.string.err_stock); valid = false
        }
        if (minStock == null || minStock < 0) {
            form.minLayout.error = getString(R.string.err_min_stock); valid = false
        }
        if (!valid) return false

        val product = Product(
            id = existing?.id ?: 0,
            name = name,
            price = price!!,
            stock = stock!!,
            barcode = barcode,
            minStock = minStock!!
        )

        return try {
            if (existing == null) db.insertProduct(product) else db.updateProduct(product)
            Toast.makeText(requireContext(), R.string.product_saved, Toast.LENGTH_SHORT).show()
            load()
            true
        } catch (e: InventoryException) {
            form.barcodeLayout.error = e.message
            false
        }
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_product)
            .setMessage(getString(R.string.confirm_delete_msg, product.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                db.deleteProduct(product.id)
                Toast.makeText(requireContext(), R.string.product_deleted, Toast.LENGTH_SHORT).show()
                load()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
