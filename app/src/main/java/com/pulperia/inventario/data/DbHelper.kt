package com.pulperia.inventario.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Acceso a la base de datos SQLite local.
 *
 * Tablas:
 *  - products(id, name, price, stock, barcode, min_stock)
 *  - movements(id, product_id, qty, type, date)
 */
class DbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Necesario para que ON DELETE CASCADE funcione.
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK (price >= 0),
                stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
                barcode TEXT,
                min_stock INTEGER NOT NULL DEFAULT ${Product.DEFAULT_MIN_STOCK} CHECK (min_stock >= 0)
            )
            """.trimIndent()
        )
        // Un código de barras no puede repetirse (los productos sin código quedan en NULL).
        db.execSQL("CREATE UNIQUE INDEX idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL")

        db.execSQL(
            """
            CREATE TABLE movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                qty INTEGER NOT NULL CHECK (qty > 0),
                type TEXT NOT NULL CHECK (type IN ('IN', 'OUT')),
                date TEXT NOT NULL,
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_movements_product ON movements(product_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión 1: no hay migraciones todavía. Cuando cambie el esquema, migrar aquí
        // con ALTER TABLE en lugar de borrar los datos del negocio.
    }

    // ---------------------------------------------------------------------
    // Productos (CRUD)
    // ---------------------------------------------------------------------

    /**
     * Crea un producto. Si trae stock inicial, registra además una entrada
     * en `movements` para que el historial siempre cuadre con el stock.
     */
    fun insertProduct(p: Product): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (barcodeInUse(db, p.barcode, excludeId = null)) throw duplicateBarcode()
            val id = try {
                db.insertOrThrow("products", null, p.toValues(includeStock = true))
            } catch (e: SQLiteConstraintException) {
                throw InventoryException("No se pudo guardar: datos inválidos o código repetido")
            }
            if (p.stock > 0) insertMovement(db, id, p.stock, MovementType.IN)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    /** Actualiza nombre, precio, código y mínimo. El stock solo cambia con movimientos. */
    fun updateProduct(p: Product) {
        val db = writableDatabase
        if (barcodeInUse(db, p.barcode, excludeId = p.id)) throw duplicateBarcode()
        try {
            db.update("products", p.toValues(includeStock = false), "id = ?", arrayOf(p.id.toString()))
        } catch (e: SQLiteConstraintException) {
            throw InventoryException("No se pudo guardar: datos inválidos o código repetido")
        }
    }

    /** Elimina el producto y, por la llave foránea, también sus movimientos. */
    fun deleteProduct(id: Long) {
        writableDatabase.delete("products", "id = ?", arrayOf(id.toString()))
    }

    fun getProducts(query: String? = null): List<Product> {
        val sql = StringBuilder("SELECT $PRODUCT_COLUMNS FROM products")
        val args = ArrayList<String>()
        if (!query.isNullOrBlank()) {
            sql.append(" WHERE name LIKE ? OR barcode LIKE ?")
            val like = "%${query.trim()}%"
            args.add(like)
            args.add(like)
        }
        sql.append(" ORDER BY name COLLATE NOCASE")
        return readableDatabase.rawQuery(sql.toString(), args.toTypedArray()).use { it.toProducts() }
    }

    fun getProductByBarcode(barcode: String): Product? =
        readableDatabase.rawQuery(
            "SELECT $PRODUCT_COLUMNS FROM products WHERE barcode = ?",
            arrayOf(barcode.trim())
        ).use { it.toProducts().firstOrNull() }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    /**
     * Registra una entrada (IN) o salida (OUT) y actualiza el stock en la misma
     * transacción. Devuelve el producto con su stock ya actualizado.
     */
    fun registerMovement(productId: Long, qty: Int, type: String): Product {
        if (qty <= 0) throw InventoryException("La cantidad debe ser mayor a 0")
        val db = writableDatabase
        db.beginTransaction()
        try {
            val product = db.rawQuery(
                "SELECT $PRODUCT_COLUMNS FROM products WHERE id = ?",
                arrayOf(productId.toString())
            ).use { it.toProducts().firstOrNull() }
                ?: throw InventoryException("El producto ya no existe")

            val newStock = if (type == MovementType.IN) product.stock + qty else product.stock - qty
            if (newStock < 0) {
                throw InventoryException("Stock insuficiente. Disponible: ${product.stock}")
            }

            db.execSQL("UPDATE products SET stock = ? WHERE id = ?", arrayOf<Any>(newStock, productId))
            insertMovement(db, productId, qty, type)
            db.setTransactionSuccessful()
            return product.copy(stock = newStock)
        } finally {
            db.endTransaction()
        }
    }

    fun getMovements(limit: Int = 50): List<Movement> {
        val sql = """
            SELECT m.id, m.product_id, p.name, m.qty, m.type, m.date
            FROM movements m
            JOIN products p ON p.id = m.product_id
            ORDER BY m.id DESC
            LIMIT $limit
        """.trimIndent()
        return readableDatabase.rawQuery(sql, null).use { c ->
            val list = ArrayList<Movement>()
            while (c.moveToNext()) {
                list.add(Movement(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getString(4), c.getString(5)))
            }
            list
        }
    }

    // ---------------------------------------------------------------------
    // Resumen
    // ---------------------------------------------------------------------

    /** Valor total del inventario: suma de price * stock de todos los productos. */
    fun getTotalValue(): Double =
        readableDatabase.rawQuery("SELECT COALESCE(SUM(price * stock), 0) FROM products", null)
            .use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }

    /** Los productos de mayor valor (price * stock). */
    fun getTopProducts(limit: Int = 5): List<Product> =
        readableDatabase.rawQuery(
            "SELECT $PRODUCT_COLUMNS FROM products ORDER BY price * stock DESC, name COLLATE NOCASE LIMIT $limit",
            null
        ).use { it.toProducts() }

    /** Productos cuyo stock llegó al mínimo configurado o por debajo. */
    fun getLowStockProducts(): List<Product> =
        readableDatabase.rawQuery(
            "SELECT $PRODUCT_COLUMNS FROM products WHERE stock <= min_stock ORDER BY stock ASC, name COLLATE NOCASE",
            null
        ).use { it.toProducts() }

    /** (cantidad de productos, unidades totales en stock). */
    fun getStats(): Pair<Int, Int> =
        readableDatabase.rawQuery("SELECT COUNT(*), COALESCE(SUM(stock), 0) FROM products", null)
            .use { c -> if (c.moveToFirst()) Pair(c.getInt(0), c.getInt(1)) else Pair(0, 0) }

    // ---------------------------------------------------------------------
    // Utilidades privadas
    // ---------------------------------------------------------------------

    private fun insertMovement(db: SQLiteDatabase, productId: Long, qty: Int, type: String) {
        val values = ContentValues().apply {
            put("product_id", productId)
            put("qty", qty)
            put("type", type)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        db.insertOrThrow("movements", null, values)
    }

    private fun barcodeInUse(db: SQLiteDatabase, barcode: String?, excludeId: Long?): Boolean {
        if (barcode.isNullOrBlank()) return false
        return db.rawQuery(
            "SELECT 1 FROM products WHERE barcode = ? AND id != ?",
            arrayOf(barcode.trim(), (excludeId ?: -1L).toString())
        ).use { it.moveToFirst() }
    }

    private fun duplicateBarcode() = InventoryException("Ya existe un producto con ese código de barras")

    private fun Product.toValues(includeStock: Boolean) = ContentValues().apply {
        put("name", name.trim())
        put("price", price)
        if (includeStock) put("stock", stock)
        if (barcode.isNullOrBlank()) putNull("barcode") else put("barcode", barcode.trim())
        put("min_stock", minStock)
    }

    private fun Cursor.toProducts(): List<Product> {
        val list = ArrayList<Product>()
        while (moveToNext()) {
            list.add(
                Product(
                    id = getLong(0),
                    name = getString(1),
                    price = getDouble(2),
                    stock = getInt(3),
                    barcode = getString(4),
                    minStock = getInt(5)
                )
            )
        }
        return list
    }

    companion object {
        private const val DB_NAME = "pulperia.db"
        private const val DB_VERSION = 1
        private const val PRODUCT_COLUMNS = "id, name, price, stock, barcode, min_stock"

        @Volatile
        private var instance: DbHelper? = null

        fun get(context: Context): DbHelper =
            instance ?: synchronized(this) {
                instance ?: DbHelper(context.applicationContext).also { instance = it }
            }
    }
}
