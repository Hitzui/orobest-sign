package com.jomiro.app.orobest_sign.screen.clients.viewmodel

import androidx.lifecycle.viewModelScope
import com.jomiro.app.orobest_sign.core.presentation.StateViewModel
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientException
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.repository.ClientsRepository
import com.jomiro.app.orobest_sign.screen.clients.uistate.ClientsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ClientsViewModel(private val repository: ClientsRepository) : StateViewModel<ClientsUiState>(ClientsUiState()) {
    private var request: Job? = null
    private var requestId = 0
    private var previewRequest: Job? = null
    private var previewRequestId = 0
    private var documentsRequestId = 0

    init { load(1, "") }

    fun search(query: String) = load(1, query.trim())
    fun refresh() = load(state.value.page, state.value.query)
    fun page(page: Int) {
        val current = state.value
        if (!current.loading && page in 1..maxOf(1, current.totalPages)) load(page, current.query)
    }

    fun select(client: com.jomiro.app.orobest_sign.feature.clients.domain.model.Client) {
        if (state.value.loading) return
        updateState { it.copy(selectedClient = client, documents = emptyList(), documentsLoading = true, documentsError = null, notice = null) }
        viewModelScope.launch { loadDocuments(client.id) }
    }

    fun backToClients() {
        if (state.value.uploading || state.value.deleting) return
        closePreview()
        updateState {
            it.copy(selectedClient = null, documents = emptyList(), documentsError = null, pendingUpload = null, queuedUploads = emptyList(), uploadedCount = 0, notice = null)
        }
    }

    fun openDocument(document: ClientDocument) {
        val client = state.value.selectedClient ?: return
        if (!document.available || state.value.uploading || state.value.deleting) return
        previewRequest?.cancel()
        val requestId = ++previewRequestId
        updateState { it.copy(previewDocument = document, previewFile = null, previewLoading = true, previewError = null) }
        previewRequest = viewModelScope.launch {
            try {
                val file = repository.content(client.id, document)
                updateState {
                    if (requestId == previewRequestId && it.selectedClient?.id == client.id && it.previewDocument?.id == document.id) {
                        it.copy(previewFile = file, previewLoading = false)
                    } else it
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState {
                    if (requestId == previewRequestId && it.selectedClient?.id == client.id && it.previewDocument?.id == document.id) {
                        it.copy(
                            previewLoading = false,
                            previewError = when {
                                error is ClientException && error.statusCode == 404 -> "El archivo ya no está disponible en el servidor."
                                error is ClientException && error.statusCode in setOf(401, 403) -> "Tu sesión o acceso cambió. Vuelve a iniciar sesión."
                                error is ClientException && error.statusCode == 422 -> "Este archivo no es una imagen JPG/PNG ni un PDF compatible."
                                else -> "No se pudo descargar el documento. Revisa tu conexión y vuelve a intentarlo."
                            },
                            requiresSessionCheck = error is ClientException && error.statusCode in setOf(401, 403),
                        )
                    } else it
                }
            }
        }
    }

    fun retryPreview() {
        state.value.previewDocument?.let(::openDocument)
    }

    fun closePreview() {
        previewRequestId++
        previewRequest?.cancel()
        previewRequest = null
        updateState { it.copy(previewDocument = null, previewFile = null, previewLoading = false, previewError = null) }
    }

    fun refreshDocuments() {
        val client = state.value.selectedClient ?: return
        viewModelScope.launch { loadDocuments(client.id) }
    }

    fun upload(category: ClientDocumentCategory, displayName: String, file: ClientFile) {
        uploadMany(category, displayName, listOf(file))
    }

    fun uploadMany(category: ClientDocumentCategory, displayName: String, files: List<ClientFile>) {
        if (state.value.uploading || state.value.deleting || state.value.pendingUpload != null) return
        val client = state.value.selectedClient ?: return
        if (files.isEmpty() || files.size > 10 || files.any { it.bytes.isEmpty() || it.bytes.size > 10 * 1024 * 1024 }) {
            updateState { it.copy(documentsError = "Selecciona de 1 a 10 archivos, de hasta 10 MiB cada uno.") }
            return
        }
        val operations = files.map { file ->
            val label = if (files.size == 1 && displayName.isNotBlank()) displayName else file.fileName
            ClientDocumentUpload(client.id, category, Uuid.random().toString(), label, file)
        }
        updateState { it.copy(pendingUpload = operations.first(), queuedUploads = operations.drop(1), uploadedCount = 0, notice = null, documentsError = null) }
        sendPending()
    }

    fun retryUpload() {
        if (state.value.pendingUpload != null && !state.value.uploading) sendPending()
    }

    fun dismissPendingUpload() {
        if (!state.value.uploading) updateState { it.copy(pendingUpload = null, queuedUploads = emptyList(), documentsError = null) }
    }

    private fun sendPending() {
        updateState { it.copy(uploading = true, documentsError = null) }
        viewModelScope.launch {
            try {
                val clientId = state.value.selectedClient?.id ?: return@launch
                while (true) {
                    val upload = state.value.pendingUpload ?: break
                    val saved = repository.upload(upload)
                    updateState { current ->
                        current.copy(
                            documents = listOf(saved) + current.documents.filterNot { it.id == saved.id },
                            pendingUpload = current.queuedUploads.firstOrNull(),
                            queuedUploads = current.queuedUploads.drop(1),
                            uploadedCount = current.uploadedCount + 1,
                        )
                    }
                }
                loadDocuments(clientId)
                updateState {
                    it.copy(uploading = false, notice = "${it.uploadedCount} documento(s) guardado(s) correctamente.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState {
                    it.copy(uploading = false, documentsError = when (error) {
                        is ClientException -> when (error.statusCode) {
                            401, 403 -> "Tu sesión o acceso cambió. Vuelve a iniciar sesión para continuar."
                            409 -> "Esta operación tuvo un conflicto. Actualiza los documentos antes de volver a intentar."
                            413 -> "El archivo supera el máximo de 10 MiB."
                            else -> "No se pudo subir el documento. Revisa el archivo y vuelve a intentarlo."
                        }
                        else -> "No pudimos subir el documento. Revisa tu conexión y vuelve a intentarlo."
                    }, requiresSessionCheck = error is ClientException && error.statusCode in setOf(401, 403))
                }
            }
        }
    }

    fun deleteDocument(document: ClientDocument) {
        val client = state.value.selectedClient ?: return
        if (state.value.uploading || state.value.deleting || state.value.pendingUpload != null) return
        updateState { it.copy(deleting = true, documentsError = null, notice = null) }
        viewModelScope.launch {
            try {
                repository.delete(client.id, document.id)
                // Una consulta iniciada antes del DELETE no debe restaurar el archivo eliminado.
                documentsRequestId++
                updateState { it.copy(deleting = false, documentsLoading = false, documents = it.documents.filterNot { item -> item.id == document.id }, notice = "Documento eliminado correctamente.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { it.copy(deleting = false, documentsError = "No se pudo confirmar la eliminación. Actualiza los documentos y vuelve a intentarlo.", requiresSessionCheck = error is ClientException && error.statusCode in setOf(401, 403)) }
            }
        }
    }

    private suspend fun loadDocuments(clientId: Int) {
        val requestId = ++documentsRequestId
        try {
            val documents = repository.documents(clientId)
            updateState { current ->
                if (requestId == documentsRequestId && current.selectedClient?.id == clientId) current.copy(documents = documents, documentsLoading = false, documentsError = null, requiresSessionCheck = false)
                else current
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            updateState { current ->
                if (requestId == documentsRequestId && current.selectedClient?.id == clientId) current.copy(
                    documentsLoading = false,
                    documentsError = "No se pudieron consultar los documentos. Inténtalo nuevamente.",
                    requiresSessionCheck = error is ClientException && error.statusCode in setOf(401, 403),
                )
                else current
            }
        }
    }

    private fun load(page: Int, query: String) {
        request?.cancel()
        val id = ++requestId
        updateState { it.copy(loading = true, error = null, page = page, query = query) }
        request = viewModelScope.launch {
            try {
                val result = repository.list(query, page)
                if (id == requestId) updateState {
                    it.copy(clients = result.items, page = result.page, total = result.total, totalPages = result.totalPages, loading = false, requiresSessionCheck = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (id == requestId) updateState {
                    it.copy(
                        loading = false,
                        error = "No se pudo cargar la lista de clientes. Revisa tu conexión e inténtalo de nuevo.",
                        requiresSessionCheck = error is ClientException && error.statusCode in setOf(401, 403),
                    )
                }
            }
        }
    }
}
