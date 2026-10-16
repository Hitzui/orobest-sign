package com.jomiro.app.orobest_sign.screen.login.viewmodel

import androidx.lifecycle.viewModelScope
import com.jomiro.app.orobest_sign.core.presentation.StateViewModel
import com.jomiro.app.orobest_sign.feature.auth.domain.model.AuthException
import com.jomiro.app.orobest_sign.feature.auth.domain.repository.AuthRepository
import com.jomiro.app.orobest_sign.screen.login.uistate.LoginPhase
import com.jomiro.app.orobest_sign.screen.login.uistate.LoginUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : StateViewModel<LoginUiState>(LoginUiState()) {
    private var operation: Job? = null

    init {
        restoreSession()
    }

    fun restoreSession() {
        if (operation?.isActive == true || state.value.phase !in setOf(LoginPhase.RestoringSession, LoginPhase.RestoreFailed)) return
        updateState { LoginUiState(phase = LoginPhase.RestoringSession) }
        operation = viewModelScope.launch {
            try {
                val profile = authRepository.restoreSession()
                updateState {
                    LoginUiState(
                        phase = if (profile == null) LoginPhase.SignIn else LoginPhase.Authenticated,
                        profile = profile,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { LoginUiState(phase = LoginPhase.RestoreFailed, errorMessage = error.messageForLogin(restoring = true)) }
            }
        }
    }

    fun useAnotherAccount() {
        if (state.value.phase == LoginPhase.RestoreFailed && operation?.isActive != true) {
            // Conserva el token anterior hasta confirmar el acceso con la otra cuenta.
            updateState { LoginUiState(phase = LoginPhase.SignIn) }
        }
    }

    fun onUsernameEdited() {
        updateState { it.copy(usernameError = null, errorMessage = null, notice = null) }
    }

    fun onPasswordEdited() {
        updateState { it.copy(passwordError = null, errorMessage = null, notice = null) }
    }

    fun login(username: String, password: String) {
        if (state.value.phase != LoginPhase.SignIn || operation?.isActive == true) return
        val userError = if (username.isBlank()) "Escribe tu usuario." else null
        val passwordError = if (password.isEmpty()) "Escribe tu contraseña." else null
        if (userError != null || passwordError != null) {
            updateState { it.copy(usernameError = userError, passwordError = passwordError, errorMessage = null, notice = null) }
            return
        }
        updateState { LoginUiState(phase = LoginPhase.SigningIn) }
        operation = viewModelScope.launch {
            try {
                val profile = authRepository.login(username.trim(), password)
                updateState { LoginUiState(phase = LoginPhase.Authenticated, profile = profile) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { LoginUiState(phase = LoginPhase.SignIn, errorMessage = error.messageForLogin()) }
            }
        }
    }

    fun logout() {
        if (state.value.phase != LoginPhase.Authenticated || operation?.isActive == true) return
        updateState { it.copy(phase = LoginPhase.SigningOut, errorMessage = null) }
        operation = viewModelScope.launch {
            try {
                authRepository.logout()
                updateState { LoginUiState(phase = LoginPhase.SignIn, notice = "Sesión cerrada correctamente.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val profile = authRepository.profile.value
                updateState {
                    if (profile == null) {
                        LoginUiState(phase = LoginPhase.SignIn, notice = "Sesión cerrada en este dispositivo.")
                    } else {
                        LoginUiState(phase = LoginPhase.Authenticated, profile = profile, errorMessage = "No pudimos cerrar la sesión. Inténtalo de nuevo.")
                    }
                }
            }
        }
    }
}

private fun Exception.messageForLogin(restoring: Boolean = false): String = when {
    this is AuthException && statusCode == 401 -> "El usuario o la contraseña no son correctos."
    this is AuthException && statusCode == 403 -> "Tu cuenta no tiene una agencia activa asignada. Consulta con administración."
    this is AuthException && statusCode == 429 -> "Has realizado varios intentos. Espera un momento y vuelve a intentarlo."
    this is AuthException && statusCode >= 500 -> "El servicio no está disponible en este momento. Inténtalo nuevamente."
    this is AuthException && statusCode in 300..499 -> "No pudimos acceder al servicio de inicio de sesión. Inténtalo nuevamente."
    restoring -> "No pudimos verificar tu sesión. Revisa tu conexión y vuelve a intentarlo; tu sesión guardada se conserva."
    else -> "No pudimos completar el acceso. Revisa tu conexión e inténtalo de nuevo."
}
