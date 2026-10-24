package com.pulperia.inventario.data

/** Producto del inventario. Fila de la tabla `products`. */
data class Product(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val stock: Int,
    val barcode: String? = null,
    val minStock: Int = DEFAULT_MIN_STOCK
) {
    /** Valor del producto en inventario: price * stock. */
    val value: Double get() = price * stock

    /** true cuando el stock alcanzó (o bajó de) el mínimo configurado. */
    val isLow: Boolean get() = stock <= minStock

    companion object {
        const val DEFAULT_MIN_STOCK = 5
    }
}

/** Movimiento de inventario. Fila de la tabla `movements` (con el nombre del producto). */
data class Movement(
    val id: Long,
    val productId: Long,
    val productName: String,
    val qty: Int,
    val type: String,
    val date: String
)

object MovementType {
    const val IN = "IN"
    const val OUT = "OUT"
}

/** Error de negocio con un mensaje apto para mostrarse al usuario. */
class InventoryException(message: String) : Exception(message)
