package com.jomiro.app.orobest_sign.feature.purchases.domain.repository

import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchasePage

interface PurchasesRepository {
    suspend fun list(filters: PurchaseFilters, page: Int): PurchasePage
}
