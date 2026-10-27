package com.jomiro.app.orobest_sign.screen.signature.viewmodel

import androidx.lifecycle.viewModelScope
import com.jomiro.app.orobest_sign.core.presentation.StateViewModel
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureException
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePoint
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureUpload
import com.jomiro.app.orobest_sign.feature.signature.domain.repository.SignatureRepository
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureConfirmation
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class SignatureViewModel(
    private val purchaseId: Int,
    private val repository: SignatureRepository,
    private val encoder: SignaturePngEncoder,
    private val renderingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StateViewModel<SignatureUiState>(SignatureUiState()) {
    private var pendingUpload: SignatureUpload? = null

    init { load() }

    private fun load() {
        pendingUpload = null
        updateState { SignatureUiState() }
        viewModelScope.launch {
            try {
                val purchase = repository.purchase(purchaseId)
                var image: ByteArray? = null
                var imageError: String? = null
                if (purchase.baseVersion > 0) {
                    try {
                        image = repository.image(purchaseId, purchase.baseVersion)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (error.needsSessionCheck()) throw error
                        imageError = "No se pudo cargar la firma anterior. Puedes volver a consultar la compra o capturar una nueva."
                    }
                }
                updateState { SignatureUiState(loading = false, purchase = purchase, previousPng = image, previousError = imageError, showPrevious = image != null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { SignatureUiState(loading = false, error = when {
                    error is SignatureException && error.code == "purchase_not_active" -> "Esta compra está anulada y no admite firmas."
                    error is SignatureException && error.statusCode == 404 -> "La compra ya no está disponible en tu agencia."
                    else -> "No pudimos consultar la compra. Revisa tu conexión y vuelve a intentarlo."
                }, requiresSessionCheck = error.needsSessionCheck()) }
            }
        }
    }

    fun beginStroke(point: SignaturePoint) {
        if (state.value.canDraw && !state.value.strokeActive) updateState { it.copy(strokes = it.strokes + listOf(listOf(point.clamped())), strokeActive = true, error = null) }
    }

    fun appendPoint(point: SignaturePoint) {
        if (!state.value.canDraw || !state.value.strokeActive) return
        val clamped = point.clamped()
        if (state.value.strokes.last().last() != clamped) {
            updateState { it.copy(strokes = it.strokes.dropLast(1) + listOf(it.strokes.last() + clamped)) }
        }
    }

    fun endStroke() = updateState { it.copy(strokeActive = false) }

    fun clear() {
        if (state.value.canDraw && !state.value.strokeActive) updateState { it.copy(strokes = emptyList(), error = null) }
    }

    fun setFingerprint(png: ByteArray) {
        if (!state.value.saving && !state.value.retryPending && !state.value.loading && !state.value.showPrevious) {
            updateState { it.copy(fingerprintPng = png, error = null) }
        }
    }

    fun clearFingerprint() {
        if (!state.value.saving && !state.value.retryPending) updateState { it.copy(fingerprintPng = null) }
    }

    fun showError(message: String) = updateState { it.copy(error = message) }

    fun togglePrevious() {
        if (!state.value.saving && !state.value.strokeActive && !state.value.retryPending && state.value.previousPng != null) {
            updateState { it.copy(showPrevious = !it.showPrevious, strokeActive = false) }
        }
    }

    /** true permite salir inmediatamente; los trazos o un envío incierto requieren confirmación. */
    fun requestExit(): Boolean {
        if (state.value.saving) return false
        if (!state.value.hasInk && state.value.fingerprintPng == null && !state.value.retryPending) return true
        updateState { it.copy(confirmation = SignatureConfirmation.Exit, strokeActive = false) }
        return false
    }

    fun requestReload() {
        if (state.value.loading || state.value.saving || state.value.retryPending) return
        if (state.value.hasInk || state.value.fingerprintPng != null) updateState { it.copy(confirmation = SignatureConfirmation.Reload) } else load()
    }

    fun reloadConfirmed() = load()
    fun dismissConfirmation() = updateState { it.copy(confirmation = null) }

    fun save() {
        val current = state.value
        if (!current.canSave) return
        val purchase = current.purchase ?: return
        updateState { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val upload = pendingUpload ?: SignatureUpload(
                    purchaseId = purchaseId,
                    operationId = Uuid.random().toString(),
                    purchaseRevision = purchase.revision,
                    baseVersion = purchase.baseVersion,
                    png = withContext(renderingDispatcher) { encoder.encode(current.strokes) },
                    fingerprintPng = current.fingerprintPng,
                ).also { pendingUpload = it }
                val saved = repository.save(upload)
                pendingUpload = null
                updateState { it.copy(saving = false, retryPending = false, savedVersion = saved.version) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val status = (error as? SignatureException)?.statusCode
                val conflict = status == 409 || status == 404
                val rejected = status in listOf(401, 403, 404, 409, 413, 422)
                if (rejected) pendingUpload = null
                updateState { it.copy(
                    saving = false, retryPending = pendingUpload != null, needsReload = conflict,
                    requiresSessionCheck = error.needsSessionCheck(),
                    error = when {
                        error is SignatureException && error.code == "purchase_changed" -> "La compra cambió mientras firmabas. Recárgala y captura una firma sobre los datos actuales."
                        error is SignatureException && error.code == "signature_changed" -> "La firma fue actualizada en otro dispositivo. Recarga la compra antes de volver a firmar."
                        error is SignatureException && error.code == "purchase_not_active" -> "La compra fue anulada y ya no admite firmas."
                        conflict -> "No se pudo guardar por un cambio en la compra o firma. Consulta nuevamente la compra."
                        status == 413 || status == 422 -> "El servidor rechazó la imagen. Limpia el lienzo y captura nuevamente la firma."
                        pendingUpload != null -> "No pudimos confirmar el guardado. Reintenta el mismo envío; no se generará una versión duplicada."
                        else -> "No pudimos generar la imagen PNG. Inténtalo de nuevo."
                    },
                ) }
            }
        }
    }
}

private fun SignaturePoint.clamped() = SignaturePoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
private fun Exception.needsSessionCheck() = this is SignatureException && statusCode in listOf(401, 403)
