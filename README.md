# Pulpería Inventario

App de Android para llevar el inventario de una pulpería. Hecha con **Kotlin** y **SQLite**.

## Qué hace

- Guarda productos con **nombre, precio y stock**.
- Registra **entradas** (cuando llega mercadería) y **salidas** (cuando se vende).
- Muestra el **valor total** del inventario y los 5 productos que más valen.
- Cada producto puede tener un **código de barras** (se escribe a mano).
- Avisa cuando un producto tiene **poco stock**.

## Cómo abrirla

1. Abre **Android Studio** y elige *Open*, luego selecciona esta carpeta.
2. Espera a que termine el *Gradle Sync* (necesita internet la primera vez).
3. Ejecútala en un emulador o en un celular con Android 8.0 o más nuevo.

## Las 3 pantallas

| Pantalla | Para qué sirve |
|---|---|
| **Productos** | Ver la lista y buscar por nombre o código. El botón **+** crea un producto, tocar un producto lo edita y el ícono rojo lo elimina. |
| **Movimientos** | Elegir un producto, marcar *Entrada* o *Salida*, escribir la cantidad y registrar. No deja sacar más de lo que hay. |
| **Resumen** | Ver el valor total del inventario, el top 5 por valor y los productos con stock bajo. |

## Alerta de stock bajo

Cada producto tiene un **stock mínimo** (5 por defecto). Cuando el stock es igual o menor al mínimo:

1. Sale un aviso al registrar una salida.
2. El producto muestra una etiqueta roja "⚠ Stock bajo" en la lista.
3. La pestaña *Resumen* muestra un globo con la cantidad de alertas.

## Base de datos

Son dos tablas:

| Tabla | Columnas |
|---|---|
| `products` | `id`, `name`, `price`, `stock`, `barcode`, `min_stock` |
| `movements` | `id`, `product_id`, `qty`, `type` (`IN` o `OUT`), `date` |

El stock solo cambia con movimientos. Por eso, al editar un producto el campo de stock está bloqueado.

## Archivos importantes

```
app/src/main/java/com/pulperia/inventario/
├── MainActivity.kt     menú de abajo y globo de alertas
├── Fmt.kt              formato de dinero (lempiras)
├── data/
│   ├── Models.kt       Product y Movement
│   └── DbHelper.kt     la base de datos
└── ui/                 una pantalla por archivo (Productos, Movimientos, Resumen)
```

## Ideas para seguir

- Escanear el código de barras con la cámara.
- Exportar el inventario a un archivo CSV.
- Guardar precio de compra y precio de venta.
