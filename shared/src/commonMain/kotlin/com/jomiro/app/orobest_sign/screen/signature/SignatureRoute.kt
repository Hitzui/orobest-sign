package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureConfirmation
import com.jomiro.app.orobest_sign.screen.signature.viewmodel.SignatureViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal expect fun SignaturePlatformEffect(onBack: () -> Unit)

@Composable
fun SignatureRoute(purchaseId: Int, onClose: () -> Unit, onSaved: (Int) -> Unit, onSessionCheck: () -> Unit) {
    val owner = remember(purchaseId) { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val viewModel = koinViewModel<SignatureViewModel>(viewModelStoreOwner = owner) { parametersOf(purchaseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exit = { if (viewModel.requestExit()) onClose() }
    SignaturePlatformEffect(onBack = exit)
    LaunchedEffect(state.savedVersion) { state.savedVersion?.let { onSaved(it) } }
    LaunchedEffect(state.requiresSessionCheck) { if (state.requiresSessionCheck) onSessionCheck() }

    SignatureScreen(
        state = state,
        onBegin = viewModel::beginStroke,
        onPoint = viewModel::appendPoint,
        onEnd = viewModel::endStroke,
        onClear = viewModel::clear,
        onTogglePrevious = viewModel::togglePrevious,
        onSave = viewModel::save,
        onExit = exit,
        onReload = viewModel::requestReload,
        onDismissConfirmation = viewModel::dismissConfirmation,
        onConfirmDiscard = {
            if (state.confirmation == SignatureConfirmation.Reload) viewModel.reloadConfirmed() else onClose()
        },
    )
}
