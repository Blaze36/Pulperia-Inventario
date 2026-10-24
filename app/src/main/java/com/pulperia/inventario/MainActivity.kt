package com.pulperia.inventario

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pulperia.inventario.data.DbHelper
import com.pulperia.inventario.databinding.ActivityMainBinding
import com.pulperia.inventario.ui.MovementsFragment
import com.pulperia.inventario.ui.ProductsFragment
import com.pulperia.inventario.ui.SummaryFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_products -> ProductsFragment()
                R.id.nav_movements -> MovementsFragment()
                R.id.nav_summary -> SummaryFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_products
        }
    }

    /**
     * Muestra en la pestaña "Resumen" un globo con la cantidad de productos
     * en stock mínimo. Los fragments lo llaman cada vez que cambian los datos.
     */
    fun refreshAlertBadge() {
        val lowCount = DbHelper.get(this).getLowStockProducts().size
        if (lowCount > 0) {
            binding.bottomNav.getOrCreateBadge(R.id.nav_summary).number = lowCount
        } else {
            binding.bottomNav.removeBadge(R.id.nav_summary)
        }
    }
}
