package com.jomiro.app.orobest_sign.core.di

import com.jomiro.app.orobest_sign.core.session.SessionStore
import org.koin.core.module.Module
import org.koin.dsl.module

/** JVM es auxiliar de desarrollo: no escribe credenciales en preferencias sin cifrar. */
internal actual val platformModule: Module = module {
    single<SessionStore> {
        object : SessionStore {
            private var token: String? = null
            override suspend fun readToken() = token
            override suspend fun writeToken(token: String) { this.token = token }
            override suspend fun clear() { token = null }
        }
    }
}
