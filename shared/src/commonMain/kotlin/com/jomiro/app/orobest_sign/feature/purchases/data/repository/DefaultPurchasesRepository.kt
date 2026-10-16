package com.jomiro.app.orobest_sign.feature.purchases.data.repository

import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.purchases.data.remote.PurchasesApi
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseException
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.repository.PurchasesRepository

internal class DefaultPurchasesRepository(private val api: PurchasesApi, private val store: SessionStore) : PurchasesRepository {
    override suspend fun list(filters: PurchaseFilters, page: Int) = api.list(
        token = store.readToken() ?: throw PurchaseException(401),
        filters = filters,
        page = page,
    )
}
