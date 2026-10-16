package com.jomiro.app.orobest_sign.screen.purchases.viewmodel

import androidx.lifecycle.viewModelScope
import com.jomiro.app.orobest_sign.core.presentation.StateViewModel
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.Purchase
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseException
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.repository.PurchasesRepository
import com.jomiro.app.orobest_sign.screen.purchases.uistate.PurchasesUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PurchasesViewModel(private val repository: PurchasesRepository) : StateViewModel<PurchasesUiState>(PurchasesUiState()) {
    private var request: Job? = null
    private var requestId = 0

    init { load(1, PurchaseFilters()) }

    fun applyFilters(filters: PurchaseFilters) {
        val normalized = filters.normalized()
        val error = normalized.validationError()
        updateState { it.copy(filterError = error) }
        if (error == null) load(1, normalized)
    }

    fun refresh() = load(state.value.page, state.value.filters)

    fun goToPage(page: Int) {
        val current = state.value
        if (!current.loading && page in 1..maxOf(1, current.totalPages) && page != current.page) load(page, current.filters)
    }

    fun selectForSignature(purchase: Purchase) {
        if (!state.value.loading && !state.value.requiresSessionCheck) updateState { it.copy(selectedPurchase = purchase) }
    }

    fun dismissSelection() = updateState { it.copy(selectedPurchase = null) }

    private fun load(page: Int, filters: PurchaseFilters) {
        request?.cancel()
        val id = ++requestId
        updateState {
            it.copy(items = emptyList(), page = page, filters = filters, total = 0, totalPages = 0,
                loading = true, error = null, filterError = null, requiresSessionCheck = false, selectedPurchase = null)
        }
        request = viewModelScope.launch {
            try {
                val result = repository.list(filters, page)
                if (id == requestId) updateState {
                    it.copy(items = result.items, page = result.page, total = result.total, totalPages = result.totalPages, loading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (id == requestId) updateState {
                    val sessionCheck = error is PurchaseException && error.statusCode in setOf(401, 403)
                    it.copy(loading = false, requiresSessionCheck = sessionCheck, error = when {
                        sessionCheck -> "Verificando el acceso a tu agencia…"
                        error is PurchaseException && error.statusCode == 422 -> "No fue posible aplicar los filtros. Revisa los valores e inténtalo de nuevo."
                        error is PurchaseException && error.statusCode >= 500 -> "El servicio de compras no está disponible. Inténtalo nuevamente."
                        else -> "No pudimos cargar las compras. Revisa tu conexión y vuelve a intentarlo."
                    })
                }
            }
        }
    }
}
