package com.jomiro.app.orobest_sign.screen.purchases.di

import com.jomiro.app.orobest_sign.feature.purchases.data.remote.PurchasesApi
import com.jomiro.app.orobest_sign.feature.purchases.data.repository.DefaultPurchasesRepository
import com.jomiro.app.orobest_sign.feature.purchases.domain.repository.PurchasesRepository
import com.jomiro.app.orobest_sign.screen.purchases.viewmodel.PurchasesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val purchasesModule = module {
    single { PurchasesApi(get(), get()) }
    single<PurchasesRepository> { DefaultPurchasesRepository(get(), get()) }
    viewModelOf(::PurchasesViewModel)
}
