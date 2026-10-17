package com.jomiro.app.orobest_sign.screen.purchases

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.SignatureFilter
import com.jomiro.app.orobest_sign.screen.purchases.viewmodel.PurchasesViewModel
import com.jomiro.app.orobest_sign.screen.signature.SignatureRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PurchasesRoute(
    profile: SessionProfile,
    signingOut: Boolean,
    sessionError: String?,
    onLogout: () -> Unit,
    onSessionCheck: () -> Unit,
    owner: ViewModelStoreOwner,
    onFullscreenChange: (Boolean) -> Unit,
) {
    val viewModel = koinViewModel<PurchasesViewModel>(viewModelStoreOwner = owner)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query = rememberTextFieldState()
    val from = rememberTextFieldState()
    val until = rememberTextFieldState()
    var signatureName by rememberSaveable { mutableStateOf(SignatureFilter.All.name) }
    val signature = SignatureFilter.valueOf(signatureName)
    val listState = rememberLazyListState()
    val resultsKey = "${state.page}:${state.filters}"
    var previousResultsKey by rememberSaveable { mutableStateOf(resultsKey) }
    LaunchedEffect(resultsKey) {
        if (previousResultsKey != resultsKey) {
            listState.scrollToItem(0)
            previousResultsKey = resultsKey
        }
    }

    LaunchedEffect(state.requiresSessionCheck) {
        if (state.requiresSessionCheck) onSessionCheck()
    }

    val selected = state.selectedPurchase
    LaunchedEffect(selected != null) { onFullscreenChange(selected != null) }
    if (selected != null) {
        SignatureRoute(
            purchaseId = selected.id,
            onClose = viewModel::dismissSelection,
            onSaved = viewModel::signatureSaved,
            onSessionCheck = onSessionCheck,
        )
    } else PurchasesScreen(
        profile = profile,
        state = state,
        query = query,
        from = from,
        until = until,
        signature = signature,
        onSignatureFilter = {
            signatureName = it.name
            viewModel.applyFilters(PurchaseFilters(query.text.toString(), from.text.toString(), until.text.toString(), it))
        },
        onApply = { viewModel.applyFilters(PurchaseFilters(query.text.toString(), from.text.toString(), until.text.toString(), signature)) },
        onClear = {
            query.setTextAndPlaceCursorAtEnd("")
            from.setTextAndPlaceCursorAtEnd("")
            until.setTextAndPlaceCursorAtEnd("")
            signatureName = SignatureFilter.All.name
            viewModel.applyFilters(PurchaseFilters())
        },
        onRefresh = viewModel::refresh,
        onPage = viewModel::goToPage,
        onSignature = viewModel::selectForSignature,
        listState = listState,
        signingOut = signingOut,
        sessionError = sessionError,
        onLogout = onLogout,
    )
}
