package com.jomiro.app.orobest_sign.feature.purchases.domain.model

data class Purchase(
    val id: Int,
    val number: String,
    /** ISO 8601 con el offset de la agencia; no reinterpretar como fecha del dispositivo. */
    val date: String,
    val total: String?,
    val customerName: String?,
    val signatureVersion: Int?,
)

enum class SignatureFilter(val apiValue: String?) {
    All(null), Pending("pendiente"), Signed("firmada"),
}

data class PurchaseFilters(
    val query: String = "",
    val from: String = "",
    val until: String = "",
    val signature: SignatureFilter = SignatureFilter.All,
) {
    fun normalized() = copy(query = query.trim(), from = from.trim(), until = until.trim())

    fun validationError(): String? = when {
        query.length > 100 -> "El número de compra admite hasta 100 caracteres."
        from.isNotEmpty() && !validDate(from) -> "La fecha desde debe ser válida y usar AAAA-MM-DD."
        until.isNotEmpty() && !validDate(until) -> "La fecha hasta debe ser válida y usar AAAA-MM-DD."
        from.isNotEmpty() && until.isNotEmpty() && from > until -> "La fecha desde no puede ser posterior a la fecha hasta."
        else -> null
    }
}

private fun validDate(value: String): Boolean {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return false
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (year !in 1..9999 || month !in 1..12) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = when (month) { 2 -> if (leap) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
    return day in 1..days
}

data class PurchasePage(val items: List<Purchase>, val page: Int, val total: Int, val totalPages: Int)

class PurchaseException(val statusCode: Int) : Exception("Purchase request failed ($statusCode)")
