package com.jomiro.app.orobest_sign.screen.signature.di

import com.jomiro.app.orobest_sign.feature.signature.data.image.DefaultSignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.data.remote.SignatureApi
import com.jomiro.app.orobest_sign.feature.signature.data.repository.DefaultSignatureRepository
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.domain.repository.SignatureRepository
import com.jomiro.app.orobest_sign.screen.signature.viewmodel.SignatureViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val signatureModule = module {
    single { SignatureApi(get(), get()) }
    single<SignatureRepository> { DefaultSignatureRepository(get(), get()) }
    single<SignaturePngEncoder> { DefaultSignaturePngEncoder() }
    viewModel { (purchaseId: Int) -> SignatureViewModel(purchaseId, get(), get()) }
}
