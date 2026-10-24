# Pulpería Inventario (Android · Kotlin · SQLite)

App para llevar el inventario de una pulpería: productos con precio y stock, entradas y salidas, resumen del valor del inventario, código de barras manual y alertas de stock mínimo.

## Cómo abrirla

1. Abre **Android Studio** (Koala o más reciente) → *Open* → selecciona esta carpeta.
2. Espera a que termine el *Gradle Sync* (descarga las dependencias; necesita internet la primera vez).
3. Ejecuta en un emulador o celular con **Android 8.0 (API 26)** o superior.

> La carpeta no incluye `gradle-wrapper.jar`; Android Studio usa su propio Gradle y lo regenera al sincronizar. Si prefieres la terminal: `gradle wrapper` y luego `./gradlew assembleDebug`.

## Base de datos (SQLite, `DbHelper.kt`)

| Tabla | Columnas |
|-------|----------|
| `products` | `id`, `name`, `price`, `stock`, `barcode` (único, opcional), `min_stock` |
| `movements` | `id`, `product_id` (FK → products, `ON DELETE CASCADE`), `qty`, `type` (`IN`/`OUT`), `date` |

`barcode` y `min_stock` son las dos columnas extra que piden los retos adicionales.

Reglas que garantiza la base de datos: precio y stock nunca negativos, cantidad de movimiento mayor a 0, tipo solo `IN` u `OUT`, y código de barras sin repetir.

## Pantallas

- **Productos** – lista con búsqueda por nombre o código. `+` crea, tocar una fila edita, el ícono rojo elimina (con confirmación; también borra su historial).
- **Movimientos** – eliges el producto (lista o escribiendo su código de barras), marcas Entrada/Salida e ingresas la cantidad. El stock se actualiza y el movimiento se guarda en **una sola transacción**. Una salida mayor al stock disponible se rechaza.
- **Resumen** – valor total del inventario (`SUM(price * stock)`), top 5 productos por valor y lista de productos con stock bajo.

## Retos adicionales

- **Código de barras manual:** campo opcional al crear/editar un producto; en Movimientos se puede buscar el producto escribiendo el código. No permite códigos repetidos.
- **Alerta de stock mínimo:** cada producto tiene su `min_stock` (5 por defecto). Se avisa de tres formas cuando `stock <= min_stock`:
  1. Diálogo emergente al registrar una salida que deja el producto en el mínimo.
  2. Etiqueta roja "⚠ Stock bajo" en la lista de productos.
  3. Globo con la cantidad de alertas en la pestaña Resumen, más una tarjeta roja con el detalle.

## Decisiones de diseño

- **El stock solo cambia con movimientos.** Al editar un producto el campo de stock está bloqueado. Al crear un producto con stock inicial se registra automáticamente una entrada, así el historial siempre cuadra con el stock actual.
- Los montos se muestran en lempiras (`L 1,250.00`); cambia el formato en `Fmt.kt` si usas otra moneda.
- Sin librerías externas más allá de AndroidX y Material: `SQLiteOpenHelper` directo, View Binding, `RecyclerView` y tres Fragments con navegación inferior.

## Estructura

```
app/src/main/java/com/pulperia/inventario/
├── MainActivity.kt          navegación inferior + globo de alertas
├── Fmt.kt                   formato de dinero
├── data/
│   ├── Models.kt            Product, Movement, MovementType
│   └── DbHelper.kt          esquema, CRUD, movimientos, totales y top 5
└── ui/
    ├── ProductsFragment.kt  + ProductAdapter.kt
    ├── MovementsFragment.kt + MovementAdapter.kt
    └── SummaryFragment.kt
```

## Ideas para seguir

Escaneo con cámara (ML Kit / CameraX), exportar el inventario a CSV, precio de costo vs. precio de venta, pantalla de ajustes para el mínimo global, pruebas instrumentadas de `DbHelper`.
