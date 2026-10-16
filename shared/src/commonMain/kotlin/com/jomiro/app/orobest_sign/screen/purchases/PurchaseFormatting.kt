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

internal fun formatDateForPicker(value: String): String {
    if (value.length != 10 || value[4] != '-' || value[7] != '-') return value
    return "${value.substring(8, 10)}/${value.substring(5, 7)}/${value.substring(0, 4)}"
}

/** Converts an ISO calendar date to the UTC-midnight value used by Compose Material 3 DatePicker. */
internal fun dateToUtcMillis(value: String): Long? {
    if (value.length != 10 || value[4] != '-' || value[7] != '-') return null
    val year = value.substring(0, 4).toIntOrNull() ?: return null
    val month = value.substring(5, 7).toIntOrNull() ?: return null
    val day = value.substring(8, 10).toIntOrNull() ?: return null
    if (year !in 1..9999 || month !in 1..12 || day !in 1..31) return null
    var adjustedYear = year
    if (month <= 2) adjustedYear--
    val era = adjustedYear / 400
    val yearOfEra = adjustedYear - era * 400
    val adjustedMonth = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    val epochDay = era * 146097L + dayOfEra - 719468L
    val millis = epochDay * 86_400_000L
    return millis.takeIf { utcMillisToDate(it) == value }
}

/** Reverses the UTC-midnight value from Compose Material 3 DatePicker into YYYY-MM-DD. */
internal fun utcMillisToDate(millis: Long): String {
    val epochDay = millis / 86_400_000L + 719468L
    val era = epochDay / 146097L
    val dayOfEra = (epochDay - era * 146097L).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    var year = yearOfEra + era.toInt() * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = monthPrime + if (monthPrime < 10) 3 else -9
    if (month <= 2) year++
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
