package com.jomiro.app.orobest_sign.screen.clients

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.clients.data.remote.ClientsApi
import com.jomiro.app.orobest_sign.feature.clients.data.repository.DefaultClientsRepository
import com.jomiro.app.orobest_sign.feature.clients.domain.model.Client
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientException
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientPage
import com.jomiro.app.orobest_sign.feature.clients.domain.repository.ClientsRepository
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.ClientsViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDocumentPreviewTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun downloadsWithBearerAndUsesActualMimeForLegacyDocuments() = runTest {
        val expected = "%PDF-1.4\nsynthetic".encodeToByteArray()
        val engine = MockEngine { request ->
            assertEquals("/api/sign/v1/clientes/7/documentos/12/contenido", request.url.encodedPath)
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            respond(expected, headers = headersOf(HttpHeaders.ContentType, "application/pdf"))
        }
        val http = createHttpClient(engine, createWireJson())
        try {
            val repository = DefaultClientsRepository(ClientsApi(http, config), Store())
            val result = repository.content(7, document.copy(mimeType = "pdf"))
            assertEquals("application/pdf", result.mimeType)
            assertEquals(document.name, result.fileName)
            assertContentEquals(expected, result.bytes)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun rejectsHtmlInsteadOfTreatingLoginPageAsDocument() = runTest {
        val engine = MockEngine {
            respond("<html>Login</html>", headers = headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val http = createHttpClient(engine, createWireJson())
        try {
            val api = ClientsApi(http, config)
            assertEquals("unsupported_document", assertFailsWith<ClientException> {
                api.content("synthetic-token", 7, document)
            }.code)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun downloadRejectionPreservesApiErrorForSessionCheck() = runTest {
        val engine = MockEngine {
            respond("""{"error":{"code":"invalid_session"}}""", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createHttpClient(engine, createWireJson())
        try {
            val api = ClientsApi(http, config)
            val error = assertFailsWith<ClientException> { api.content("synthetic-token", 7, document) }
            assertEquals(401, error.statusCode)
            assertEquals("invalid_session", error.code)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun lateDownloadCannotReplaceReopenedPreviewOfSameFile() = runTest {
        val oldResponse = CompletableDeferred<ClientFile>()
        var calls = 0
        val repository = FakeRepository {
            calls++
            if (calls == 1) withContext(NonCancellable) { oldResponse.await() }
            else ClientFile(byteArrayOf(2), "new.png", "image/png")
        }
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        viewModel.select(client)
        advanceUntilIdle()
        viewModel.openDocument(document)
        runCurrent()
        viewModel.closePreview()
        assertNull(viewModel.state.value.previewFile)
        viewModel.openDocument(document)
        runCurrent()
        assertContentEquals(byteArrayOf(2), viewModel.state.value.previewFile?.bytes)
        oldResponse.complete(ClientFile(byteArrayOf(1), "old.png", "image/png"))
        advanceUntilIdle()
        assertContentEquals(byteArrayOf(2), viewModel.state.value.previewFile?.bytes)
        viewModel.closePreview()
        assertNull(viewModel.state.value.previewDocument)
        assertNull(viewModel.state.value.previewFile)
    }

    @Test
    fun missingFileCanRetryAndAccessRejectionRequestsRevalidation() = runTest {
        var status = 404
        val viewModel = ClientsViewModel(FakeRepository { throw ClientException(status) })
        advanceUntilIdle()
        viewModel.select(client)
        advanceUntilIdle()
        viewModel.openDocument(document)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.previewLoading)
        assertNotNull(viewModel.state.value.previewError)
        assertFalse(viewModel.state.value.requiresSessionCheck)
        status = 401
        viewModel.retryPreview()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.requiresSessionCheck)
        assertNull(viewModel.state.value.previewFile)
    }

    private class FakeRepository(private val download: suspend () -> ClientFile) : ClientsRepository {
        override suspend fun list(query: String, page: Int) = ClientPage(emptyList(), page, 0, 0)
        override suspend fun documents(clientId: Int) = emptyList<ClientDocument>()
        override suspend fun content(clientId: Int, document: ClientDocument) = download()
        override suspend fun delete(clientId: Int, documentId: Int) {
            error("Deletion is not part of preview tests")
        }
        override suspend fun upload(upload: ClientDocumentUpload): ClientDocument = error("Not used")
    }

    private class Store : SessionStore {
        override suspend fun readToken() = "synthetic-token"
        override suspend fun writeToken(token: String) = Unit
        override suspend fun clear() = Unit
    }

    private val config = ApiConfig("https://example.test/api/sign/v1/")
    private val client = Client(7, "Cliente", "Sintético", null, null, null)
    private val document = ClientDocument(12, ClientDocumentCategory.Other, "documento.pdf", "pdf", 100, null, true)
}
