package com.jomiro.app.orobest_sign.feature.signature.data.repository

import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.signature.data.remote.SignatureApi
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureException
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureUpload
import com.jomiro.app.orobest_sign.feature.signature.domain.repository.SignatureRepository

internal class DefaultSignatureRepository(private val api: SignatureApi, private val store: SessionStore) : SignatureRepository {
    private suspend fun token() = store.readToken() ?: throw SignatureException(401, "invalid_session")
    override suspend fun purchase(id: Int) = api.purchase(token(), id)
    override suspend fun image(id: Int, version: Int) = api.image(token(), id, version)
    override suspend fun save(upload: SignatureUpload) = api.save(token(), upload)
    override suspend fun saveFingerprint(id: Int, png: ByteArray) = api.saveFingerprint(token(), id, png)
}
