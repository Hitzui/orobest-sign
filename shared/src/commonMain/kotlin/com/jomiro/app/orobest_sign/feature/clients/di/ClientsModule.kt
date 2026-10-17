package com.jomiro.app.orobest_sign.feature.clients.di

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.clients.data.remote.ClientsApi
import com.jomiro.app.orobest_sign.feature.clients.data.repository.DefaultClientsRepository
import com.jomiro.app.orobest_sign.feature.clients.domain.repository.ClientsRepository
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.ClientsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val clientsModule = module {
    single { ClientsApi(get(), get<ApiConfig>()) }
    single<ClientsRepository> { DefaultClientsRepository(get(), get<SessionStore>()) }
    viewModelOf(::ClientsViewModel)
}
