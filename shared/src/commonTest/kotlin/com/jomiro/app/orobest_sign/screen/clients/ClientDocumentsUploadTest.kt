package com.jomiro.app.orobest_sign.screen.clients

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.feature.clients.data.remote.ClientsApi
import com.jomiro.app.orobest_sign.feature.clients.domain.model.*
import com.jomiro.app.orobest_sign.feature.clients.domain.repository.ClientsRepository
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.ClientsViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDocumentsUploadTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun uncertainBatchRetriesOnlyPendingFilesWithSameOperationAndBytes() = runTest {
        val repository = Repository()
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        viewModel.select(client)
        advanceUntilIdle()
        val files = (1..3).map { ClientFile(byteArrayOf(it.toByte()), "mismo-nombre.png", "image/png") }
        repository.failOnCall = 2
        viewModel.uploadMany(ClientDocumentCategory.Other, "", files)
        advanceUntilIdle()
        assertEquals(2, repository.calls.size)
        assertEquals(1, viewModel.state.value.uploadedCount)
        assertEquals(1, viewModel.state.value.queuedUploads.size)
        val uncertain = repository.calls.last()
        assertEquals(uncertain.operationId, viewModel.state.value.pendingUpload?.operationId)
        assertFalse(viewModel.state.value.uploading)
        viewModel.retryUpload()
        advanceUntilIdle()
        assertEquals(4, repository.calls.size)
        assertEquals(uncertain.operationId, repository.calls[2].operationId)
        assertContentEquals(uncertain.file.bytes, repository.calls[2].file.bytes)
        assertEquals(3, repository.saved.size)
        assertEquals(3, repository.calls.map { it.operationId }.distinct().size)
        assertNull(viewModel.state.value.pendingUpload)
        assertTrue(viewModel.state.value.queuedUploads.isEmpty())
        assertEquals(3, viewModel.state.value.documents.size)
    }

    @Test
    fun deleteOnlyRemovesConfirmedDocumentAndPreservesOthers() = runTest {
        val repository = Repository()
        repository.saved += listOf(document(1), document(2))
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        viewModel.select(client)
        advanceUntilIdle()
        repository.failDelete = true
        viewModel.deleteDocument(document(1))
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.documents.size)
        assertNotNull(viewModel.state.value.documentsError)
        repository.failDelete = false
        viewModel.deleteDocument(document(1))
        advanceUntilIdle()
        assertEquals(listOf(2), viewModel.state.value.documents.map { it.id })
        assertFalse(viewModel.state.value.deleting)
    }

    @Test
    fun deleteUsesAuthenticatedEndpointAndRequiresConfirmation() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/api/sign/v1/clientes/7/documentos/2", request.url.encodedPath)
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            respond("""{"success":true,"data":{"deleted":true}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createHttpClient(engine, createWireJson())
        try {
            ClientsApi(http, ApiConfig("https://example.test/api/sign/v1/")).delete("synthetic-token", 7, 2)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun invalidBatchIsRejectedBeforeUploadingAnything() = runTest {
        val repository = Repository()
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        viewModel.select(client)
        advanceUntilIdle()
        viewModel.uploadMany(ClientDocumentCategory.Other, "", List(11) { ClientFile(byteArrayOf(1), "foto.png", "image/png") })
        advanceUntilIdle()
        assertTrue(repository.calls.isEmpty())
        assertNotNull(viewModel.state.value.documentsError)
    }

    private class Repository : ClientsRepository {
        val saved = mutableListOf<ClientDocument>()
        val calls = mutableListOf<ClientDocumentUpload>()
        var failOnCall = -1
        var failDelete = false
        override suspend fun list(query: String, page: Int) = ClientPage(emptyList(), page, 0, 0)
        override suspend fun documents(clientId: Int) = saved.toList()
        override suspend fun content(clientId: Int, document: ClientDocument): ClientFile = error("Not used")
        override suspend fun upload(upload: ClientDocumentUpload): ClientDocument {
            calls += upload
            if (calls.size == failOnCall) error("Connection lost")
            return ClientDocument(saved.size + 1, upload.category, upload.displayName, upload.file.mimeType, upload.file.bytes.size.toLong(), null, true).also { saved += it }
        }
        override suspend fun delete(clientId: Int, documentId: Int) {
            if (failDelete) error("Connection lost")
            saved.removeAll { it.id == documentId }
        }
    }

    private val client = Client(7, "Cliente", "Sintético", null, null, null)
    private fun document(id: Int) = ClientDocument(id, ClientDocumentCategory.Other, "documento.png", "image/png", 100, null, true)
}
