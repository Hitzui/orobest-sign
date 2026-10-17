package com.jomiro.app.orobest_sign.screen.clients

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.feature.clients.data.remote.ClientsApi
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
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDocumentDeletionTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun sendsAuthenticatedDeleteAndRequiresServerConfirmation() = runTest {
        var confirmed = false
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/api/sign/v1/clientes/7/documentos/12", request.url.encodedPath)
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            respond("""{"success":true,"data":{"deleted":$confirmed}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createHttpClient(engine, createWireJson())
        try {
            val api = ClientsApi(http, ApiConfig("https://example.test/api/sign/v1/"))
            assertEquals("delete_not_confirmed", assertFailsWith<ClientException> { api.delete("synthetic-token", 7, 12) }.code)
            confirmed = true
            api.delete("synthetic-token", 7, 12)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun retainsFileUntilConfirmedAndIgnoresPreDeleteListingResponse() = runTest {
        val deletion = CompletableDeferred<Unit>()
        val staleListing = CompletableDeferred<List<ClientDocument>>()
        var listingCalls = 0
        val repository = FakeRepository(onList = {
            if (++listingCalls == 1) listOf(document, otherDocument) else staleListing.await()
        }, onDelete = { deletion.await() })
        val vm = ClientsViewModel(repository)
        advanceUntilIdle()
        vm.select(client)
        advanceUntilIdle()
        vm.refreshDocuments()
        runCurrent()
        vm.deleteDocument(document)
        runCurrent()
        assertTrue(vm.state.value.deleting)
        assertEquals(2, vm.state.value.documents.size)
        assertNull(vm.state.value.notice)
        deletion.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.deleting)
        assertEquals(listOf(otherDocument), vm.state.value.documents)
        assertNotNull(vm.state.value.notice)
        staleListing.complete(listOf(document, otherDocument))
        advanceUntilIdle()
        assertEquals(listOf(otherDocument), vm.state.value.documents)
    }

    @Test
    fun failedDeletionKeepsDocumentAndChecksRevokedSession() = runTest {
        val vm = ClientsViewModel(FakeRepository(onDelete = { throw ClientException(401, "invalid_session") }))
        advanceUntilIdle()
        vm.select(client)
        advanceUntilIdle()
        vm.deleteDocument(document)
        advanceUntilIdle()
        assertFalse(vm.state.value.deleting)
        assertEquals(listOf(document), vm.state.value.documents)
        assertNotNull(vm.state.value.documentsError)
        assertNull(vm.state.value.notice)
        assertTrue(vm.state.value.requiresSessionCheck)
    }

    private inner class FakeRepository(
        private val onList: suspend () -> List<ClientDocument> = { listOf(document) },
        private val onDelete: suspend () -> Unit,
    ) : ClientsRepository {
        override suspend fun list(query: String, page: Int) = ClientPage(emptyList(), page, 0, 0)
        override suspend fun documents(clientId: Int) = onList()
        override suspend fun content(clientId: Int, document: ClientDocument): ClientFile = error("Not used")
        override suspend fun upload(upload: ClientDocumentUpload): ClientDocument = error("Not used")
        override suspend fun delete(clientId: Int, documentId: Int) {
            assertEquals(client.id, clientId)
            assertEquals(document.id, documentId)
            onDelete()
        }
    }

    private val client = Client(7, "Cliente", "Sintético", null, null, null)
    private val document = ClientDocument(12, ClientDocumentCategory.Other, "prueba.pdf", "application/pdf", 100, null, true)
    private val otherDocument = document.copy(id = 13, name = "otro.pdf")
}
