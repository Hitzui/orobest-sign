package com.jomiro.app.orobest_sign.screen.login.uistate

import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile

enum class LoginPhase {
    RestoringSession,
    SignIn,
    SigningIn,
    RestoreFailed,
    Authenticated,
    SigningOut,
}

/** Estado de presentación. No contiene contraseñas ni tokens. */
data class LoginUiState(
    val phase: LoginPhase = LoginPhase.RestoringSession,
    val profile: SessionProfile? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val notice: String? = null,
)
