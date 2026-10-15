package com.jomiro.app.orobest_sign.feature.auth.data.repository

import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.auth.data.remote.AuthApi
import com.jomiro.app.orobest_sign.feature.auth.domain.model.AuthException
import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import com.jomiro.app.orobest_sign.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DefaultAuthRepository(private val api: AuthApi, private val store: SessionStore) : AuthRepository {
    private val mutex = Mutex()
    private val mutableProfile = MutableStateFlow<SessionProfile?>(null)
    override val profile = mutableProfile.asStateFlow()

    override suspend fun login(username: String, password: String): SessionProfile = mutex.withLock {
        val response = api.login(username.trim(), password)
        val token = requireNotNull(response.accessToken) { "El servidor no devolvió una sesión" }
        store.writeToken(token)
        response.toProfile().also { mutableProfile.value = it }
    }

    override suspend fun restoreSession(): SessionProfile? = mutex.withLock {
        val token = store.readToken() ?: return@withLock null.also { mutableProfile.value = null }
        try {
            api.me(token).toProfile().also { mutableProfile.value = it }
        } catch (e: AuthException) {
            mutableProfile.value = null
            if (e.statusCode != 401) throw e
            store.clear()
            null
        }
    }

    override suspend fun logout(): Unit = mutex.withLock {
        val token = store.readToken()
        try {
            if (token != null) {
                try {
                    api.logout(token)
                } catch (e: AuthException) {
                    if (e.statusCode != 401) throw e
                }
            }
        } finally {
            withContext(NonCancellable) {
                store.clear()
                mutableProfile.value = null
            }
        }
    }
}
