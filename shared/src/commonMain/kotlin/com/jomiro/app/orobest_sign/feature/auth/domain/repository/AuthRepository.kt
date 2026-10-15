package com.jomiro.app.orobest_sign.feature.auth.domain.repository

import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val profile: StateFlow<SessionProfile?>
    suspend fun login(username: String, password: String): SessionProfile
    /** null solo si no hay sesión o el servidor confirma 401. Un fallo de red conserva el token. */
    suspend fun restoreSession(): SessionProfile?
    suspend fun logout()
}
