package com.jomiro.app.orobest_sign.core.di

import com.jomiro.app.orobest_sign.core.session.AndroidSessionStore
import com.jomiro.app.orobest_sign.core.session.SessionStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<SessionStore> { AndroidSessionStore(get()) }
}
