package com.pulperia.inventario

import java.util.Locale

/** Utilidades de formato. */
object Fmt {
    /** Formato de lempiras, p. ej. "L 1,250.50". */
    fun money(value: Double): String = String.format(Locale.US, "L %,.2f", value)
}
