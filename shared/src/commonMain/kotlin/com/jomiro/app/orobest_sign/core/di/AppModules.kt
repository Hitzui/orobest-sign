package com.jomiro.app.orobest_sign.core.di

import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createPlatformHttpClientEngine
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.network.platformApiConfig
import com.jomiro.app.orobest_sign.feature.auth.data.remote.AuthApi
import com.jomiro.app.orobest_sign.feature.auth.data.repository.DefaultAuthRepository
import com.jomiro.app.orobest_sign.feature.auth.domain.repository.AuthRepository
import com.jomiro.app.orobest_sign.feature.clients.di.clientsModule
import com.jomiro.app.orobest_sign.screen.login.di.loginModule
import com.jomiro.app.orobest_sign.screen.purchases.di.purchasesModule
import com.jomiro.app.orobest_sign.screen.signature.di.signatureModule
import org.koin.dsl.module
import org.koin.dsl.onClose

private val networkModule = module {
    single { platformApiConfig() }
    single { createWireJson() }
    single { createPlatformHttpClientEngine() } onClose { engine ->
        engine?.close()
    }
    single { createHttpClient(get(), get()) } onClose { client ->
        client?.close()
    }
}

private val authModule = module {
    single { AuthApi(get(), get()) }
    single<AuthRepository> { DefaultAuthRepository(get(), get()) }
}

val appModules = listOf(networkModule, platformModule, authModule, loginModule, purchasesModule, signatureModule, clientsModule)
