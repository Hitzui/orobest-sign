package com.jomiro.app.orobest_sign.feature.clients.domain.repository

import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientPage
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

interface ClientsRepository {
    suspend fun list(query: String, page: Int): ClientPage
    suspend fun documents(clientId: Int): List<ClientDocument>
    suspend fun content(clientId: Int, document: ClientDocument): ClientFile
    suspend fun upload(upload: ClientDocumentUpload): ClientDocument
    suspend fun delete(clientId: Int, documentId: Int)
}
