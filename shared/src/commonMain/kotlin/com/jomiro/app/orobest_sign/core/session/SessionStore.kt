package com.jomiro.app.orobest_sign.core.session

/** Guarda únicamente el token opaco; nunca la contraseña del usuario. */
interface SessionStore {
    suspend fun readToken(): String?
    suspend fun writeToken(token: String)
    suspend fun clear()
}
