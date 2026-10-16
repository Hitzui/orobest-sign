package com.jomiro.app.orobest_sign.screen.signature.uistate

import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePurchase
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureStroke

enum class SignatureConfirmation { Exit, Reload }

data class SignatureUiState(
    val loading: Boolean = true,
    val purchase: SignaturePurchase? = null,
    val previousPng: ByteArray? = null,
    val previousError: String? = null,
    val showPrevious: Boolean = false,
    val strokes: List<SignatureStroke> = emptyList(),
    val strokeActive: Boolean = false,
    val saving: Boolean = false,
    val retryPending: Boolean = false,
    val needsReload: Boolean = false,
    val error: String? = null,
    val savedVersion: Int? = null,
    val requiresSessionCheck: Boolean = false,
    val confirmation: SignatureConfirmation? = null,
) {
    val hasInk get() = strokes.any { it.isNotEmpty() }
    val canDraw get() = purchase != null && !loading && !saving && !retryPending && !needsReload && !showPrevious && !requiresSessionCheck && savedVersion == null
    val canSave get() = purchase != null && !loading && !saving && !needsReload && !strokeActive && !showPrevious && !requiresSessionCheck && savedVersion == null && (hasInk || retryPending)
}
