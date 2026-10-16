package com.jomiro.app.orobest_sign.feature.signature.domain.repository

import com.jomiro.app.orobest_sign.feature.signature.domain.model.SavedSignature
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePurchase
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureUpload

interface SignatureRepository {
    suspend fun purchase(id: Int): SignaturePurchase
    suspend fun image(id: Int, version: Int): ByteArray
    suspend fun save(upload: SignatureUpload): SavedSignature
}
