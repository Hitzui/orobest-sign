package com.jomiro.app.orobest_sign.feature.clients.data.repository

import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.clients.data.remote.ClientsApi
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientException
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientPage
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.repository.ClientsRepository

internal class DefaultClientsRepository(private val api: ClientsApi, private val sessionStore: SessionStore) : ClientsRepository {
    private suspend fun token(): String = sessionStore.readToken() ?: throw ClientException(401, "missing_session")
    override suspend fun list(query: String, page: Int): ClientPage = api.list(token(), query, page)
    override suspend fun documents(clientId: Int): List<ClientDocument> = api.documents(token(), clientId)
    override suspend fun content(clientId: Int, document: ClientDocument): ClientFile = api.content(token(), clientId, document)
    override suspend fun upload(upload: ClientDocumentUpload): ClientDocument = api.upload(token(), upload)
    override suspend fun delete(clientId: Int, documentId: Int) = api.delete(token(), clientId, documentId)
}
