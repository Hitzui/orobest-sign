package com.jomiro.app.orobest_sign.screen.purchases.uistate

import com.jomiro.app.orobest_sign.feature.purchases.domain.model.Purchase
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters

data class PurchasesUiState(
    val items: List<Purchase> = emptyList(),
    val filters: PurchaseFilters = PurchaseFilters(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val filterError: String? = null,
    val requiresSessionCheck: Boolean = false,
    val selectedPurchase: Purchase? = null,
)
