package com.jomiro.app.orobest_sign.core.di

import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createPlatformHttpClientEngine
import com.jomiro.app.orobest_sign.core.network.createWireJson
import org.koin.dsl.module
import org.koin.dsl.onClose

private val networkModule = module {
    single { createWireJson() }
    single { createPlatformHttpClientEngine() } onClose { engine ->
        engine?.close()
    }
    single { createHttpClient(get(), get()) } onClose { client ->
        client?.close()
    }
}

val appModules = listOf(networkModule)
