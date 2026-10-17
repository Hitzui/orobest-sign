package com.jomiro.app.orobest_sign.screen.clients.uistate

import com.jomiro.app.orobest_sign.feature.clients.domain.model.Client
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

data class ClientsUiState(
    val clients: List<Client> = emptyList(),
    val query: String = "",
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val selectedClient: Client? = null,
    val documents: List<ClientDocument> = emptyList(),
    val documentsLoading: Boolean = false,
    val documentsError: String? = null,
    val uploading: Boolean = false,
    val pendingUpload: ClientDocumentUpload? = null,
    val queuedUploads: List<ClientDocumentUpload> = emptyList(),
    val uploadedCount: Int = 0,
    val deleting: Boolean = false,
    val notice: String? = null,
    val requiresSessionCheck: Boolean = false,
    val previewDocument: ClientDocument? = null,
    val previewFile: ClientFile? = null,
    val previewLoading: Boolean = false,
    val previewError: String? = null,
)
