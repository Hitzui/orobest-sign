package com.jomiro.app.orobest_sign.screen.purchases

/** Mantiene todos los decimales del servidor, sin conversión a Double ni moneda supuesta. */
internal fun formatPurchaseTotal(value: String?): String {
    if (value == null) return "—"
    if (!Regex("-?[0-9]+(?:\\.[0-9]+)?").matches(value)) return value
    val unsigned = value.removePrefix("-")
    val integer = unsigned.substringBefore('.').reversed().chunked(3).joinToString(",").reversed()
    val fraction = if ('.' in unsigned) "." + unsigned.substringAfter('.') else ""
    return (if (value.startsWith('-')) "-" else "") + integer + fraction
}

internal fun formatPurchaseDate(value: String): String {
    if (value.length < 16 || value.getOrNull(4) != '-' || value.getOrNull(7) != '-') return value
    return "${value.substring(8, 10)}/${value.substring(5, 7)}/${value.substring(0, 4)} · ${value.substring(11, 16)}"
}
