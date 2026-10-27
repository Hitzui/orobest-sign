package com.jomiro.app.orobest_sign.screen.signature

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.signature.data.remote.SignatureApi
import com.jomiro.app.orobest_sign.feature.signature.data.repository.DefaultSignatureRepository
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SavedSignature
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureException
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePoint
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePurchase
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureStroke
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureUpload
import com.jomiro.app.orobest_sign.feature.signature.domain.repository.SignatureRepository
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureConfirmation
import com.jomiro.app.orobest_sign.screen.signature.viewmodel.SignatureViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SignatureFlowTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun standaloneFingerprintUsesHuellaEndpointWithoutSignatureFields() = runTest {
        val engine = MockEngine(MockEngineConfig().apply {
            addHandler { request ->
                assertEquals("POST", request.method.value)
                assertEquals("/api/sign/v1/compras/123/huella", request.url.encodedPath)
                assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
                val body = request.body.toByteArray()
                assertTrue(body.asList().windowed(png.size).any { it == png.asList() })
                val multipart = body.decodeToString()
                assertTrue(multipart.contains("name=\"huella\""))
                assertTrue(multipart.contains("filename=\"huella.png\""))
                assertFalse(multipart.contains("name=\"firma\""))
                respond("""{"success":true,"data":{"idcompra":123,"huella_guardada":true}}""", headers = jsonHeaders)
            }
        })
        val client = createHttpClient(engine, createWireJson())
        try {
            val api = SignatureApi(client, ApiConfig("https://example.test/api/sign/v1/"))
            api.saveFingerprint("synthetic-token", 123, png)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun editLoadsAuthorizedVersionAndRetrySendsTheSamePngOperationAndRevision() = runTest {
        val bodies = mutableListOf<String>()
        val encoder = Encoder()
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
                when {
                    request.method.value == "POST" -> {
                        assertEquals("/api/sign/v1/compras/123/firma", request.url.encodedPath)
                        val bytes = request.body.toByteArray()
                        assertTrue(bytes.asList().windowed(png.size).any { it == png.asList() })
                        val body = bytes.decodeToString()
                        assertTrue(body.contains("image/png"))
                        assertTrue(body.contains("filename=\"firma.png\""))
                        assertTrue(body.contains(revision))
                        val versionPart = Regex("name=\"?base_version\"?[^\\r]*\\r\\n(?:[^\\r]+\\r\\n)*\\r\\n3\\r\\n")
                        assertTrue(versionPart.containsMatchIn(body))
                        bodies += body
                        if (bodies.size == 1) error("Synthetic lost upload response")
                        respond("""{"success":true,"data":{"firma":{"idcompra":123,"version":4},"replayed":true}}""", headers = jsonHeaders)
                    }
                    request.url.encodedPath.endsWith("/firma") -> {
                        assertEquals("3", request.url.parameters["version"])
                        respond(png, headers = headersOf(HttpHeaders.ContentType, "image/png"))
                    }
                    else -> {
                        assertEquals("/api/sign/v1/compras/123", request.url.encodedPath)
                        respond("""{"success":true,"data":{"idcompra":123,"numcompra":"A-123","estado":1,"revision":"$revision","cliente":{"nombre":"Cliente de prueba"},"firma":{"idcompra":123,"version":3,"compra_modificada":true}}}""", headers = jsonHeaders)
                    }
                }
            }
        })
        val client = createHttpClient(engine, createWireJson())
        try {
            val repository = DefaultSignatureRepository(SignatureApi(client, ApiConfig("https://example.test/api/sign/v1/")), Store())
            val viewModel = SignatureViewModel(123, repository, encoder, StandardTestDispatcher(testScheduler))
            viewModel.awaitIdle()
            assertTrue(viewModel.state.value.showPrevious)
            assertTrue(viewModel.state.value.purchase!!.purchaseModified)
            assertContentEquals(png, viewModel.state.value.previousPng)
            assertFalse(viewModel.state.value.canSave)
            viewModel.togglePrevious()
            viewModel.draw()
            viewModel.save()
            viewModel.awaitIdle()
            assertNull(viewModel.state.value.savedVersion)
            assertTrue(viewModel.state.value.retryPending)
            assertFalse(viewModel.state.value.canDraw)
            assertFalse(viewModel.requestExit())
            viewModel.dismissConfirmation()
            viewModel.save()
            viewModel.awaitIdle()
            assertEquals(4, viewModel.state.value.savedVersion)
            assertEquals(1, encoder.calls)
            val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
            assertEquals(uuid.find(bodies[0])!!.value, uuid.find(bodies[1])!!.value)
            assertEquals(2, bodies.size)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun conflictRequiresExplicitReloadAndNeverSilentlyRebasesTheDrawing() = runTest {
        val repository = Repository()
        repository.failure = SignatureException(409, "signature_changed")
        val viewModel = SignatureViewModel(123, repository, Encoder(), StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        viewModel.draw()
        viewModel.save()
        viewModel.awaitIdle()
        assertTrue(viewModel.state.value.needsReload)
        assertTrue(viewModel.state.value.hasInk)
        assertFalse(viewModel.state.value.retryPending)
        assertFalse(viewModel.state.value.canSave)
        assertEquals(1, repository.detailCalls)
        viewModel.requestReload()
        assertEquals(SignatureConfirmation.Reload, viewModel.state.value.confirmation)
        repository.current = repository.current.copy(baseVersion = 5, revision = "b".repeat(64))
        repository.failure = null
        viewModel.reloadConfirmed()
        viewModel.awaitIdle()
        assertFalse(viewModel.state.value.hasInk)
        assertEquals(5, viewModel.state.value.purchase!!.baseVersion)
        viewModel.togglePrevious()
        viewModel.draw()
        viewModel.save()
        viewModel.awaitIdle()
        assertEquals(6, viewModel.state.value.savedVersion)
        assertEquals("b".repeat(64), repository.uploads.last().purchaseRevision)
    }

    @Test
    fun blocksEmptySaveDuplicateSubmitAndExitWhileUploading() = runTest {
        val repository = Repository()
        val responseReady = CompletableDeferred<Unit>()
        repository.responseReady = responseReady
        val encoder = Encoder()
        val viewModel = SignatureViewModel(123, repository, encoder, StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        viewModel.save()
        assertEquals(0, encoder.calls)
        assertEquals(0, repository.uploads.size)
        viewModel.beginStroke(SignaturePoint(0.5f, 0.5f))
        viewModel.appendPoint(SignaturePoint(0.5f, 0.5f))
        assertFalse(viewModel.state.value.canSave)
        viewModel.endStroke()
        assertEquals(1, viewModel.state.value.strokes.single().size)
        viewModel.save()
        runCurrent()
        viewModel.save()
        assertFalse(viewModel.requestExit())
        assertNull(viewModel.state.value.confirmation)
        responseReady.complete(Unit)
        viewModel.awaitIdle()
        assertEquals(1, repository.uploads.size)
        assertFalse(viewModel.state.value.canSave)
        assertEquals(1, viewModel.state.value.savedVersion)
    }

    @Test
    fun savesFingerprintWithoutCreatingASignatureVersion() = runTest {
        val repository = Repository()
        val viewModel = SignatureViewModel(123, repository, Encoder(), StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        viewModel.setFingerprint(png)

        assertTrue(viewModel.state.value.canSaveFingerprint)
        assertFalse(viewModel.state.value.canSave)
        viewModel.saveFingerprint()
        viewModel.awaitIdle()

        assertTrue(viewModel.state.value.fingerprintSaved)
        assertEquals(123, repository.savedFingerprints.single().first)
        assertContentEquals(png, repository.savedFingerprints.single().second)
        assertTrue(repository.uploads.isEmpty())
        assertNull(viewModel.state.value.savedVersion)
        assertTrue(viewModel.requestExit())
    }

    @Test
    fun fingerprintSaveShowsWhenTheServerDoesNotHaveTheDedicatedRoute() = runTest {
        val repository = Repository().apply { fingerprintFailure = SignatureException(404, "not_found") }
        val viewModel = SignatureViewModel(123, repository, Encoder(), StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        viewModel.setFingerprint(png)
        viewModel.saveFingerprint()
        viewModel.awaitIdle()

        assertTrue(viewModel.state.value.error.orEmpty().contains("404"))
        assertFalse(viewModel.state.value.fingerprintSaved)
        assertTrue(viewModel.state.value.canSaveFingerprint)
    }

    @Test
    fun backConfirmsDiscardButClearAllowsImmediateExit() = runTest {
        val viewModel = SignatureViewModel(123, Repository(), Encoder(), StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        assertTrue(viewModel.requestExit())
        viewModel.draw()
        assertFalse(viewModel.requestExit())
        assertEquals(SignatureConfirmation.Exit, viewModel.state.value.confirmation)
        viewModel.dismissConfirmation()
        viewModel.clear()
        assertTrue(viewModel.requestExit())
    }

    @Test
    fun accessRejectionRequestsSessionCheckWithoutReportingSaved() = runTest {
        val repository = Repository().apply { failure = SignatureException(403, "agency_unavailable") }
        val viewModel = SignatureViewModel(123, repository, Encoder(), StandardTestDispatcher(testScheduler))
        viewModel.awaitIdle()
        viewModel.draw()
        viewModel.save()
        viewModel.awaitIdle()
        assertTrue(viewModel.state.value.requiresSessionCheck)
        assertFalse(viewModel.state.value.canSave)
        assertNull(viewModel.state.value.savedVersion)
    }

    private suspend fun SignatureViewModel.awaitIdle() = withTimeout(5_000) { state.first { !it.loading && !it.saving && !it.savingFingerprint } }
    private fun SignatureViewModel.draw() {
        beginStroke(SignaturePoint(0.1f, 0.2f))
        appendPoint(SignaturePoint(0.5f, 0.3f))
        endStroke()
    }

    private class Encoder : SignaturePngEncoder {
        var calls = 0
        override fun encode(strokes: List<SignatureStroke>): ByteArray { calls++; return png }
    }

    private class Repository : SignatureRepository {
        var current = SignaturePurchase(123, "A-123", "Cliente de prueba", revision, 0, false)
        var detailCalls = 0
        var failure: Exception? = null
        var fingerprintFailure: Exception? = null
        var responseReady: CompletableDeferred<Unit>? = null
        val uploads = mutableListOf<SignatureUpload>()
        val savedFingerprints = mutableListOf<Pair<Int, ByteArray>>()
        override suspend fun purchase(id: Int): SignaturePurchase { detailCalls++; return current }
        override suspend fun image(id: Int, version: Int) = png
        override suspend fun saveFingerprint(id: Int, png: ByteArray) {
            fingerprintFailure?.let { throw it }
            savedFingerprints += id to png.copyOf()
        }
        override suspend fun save(upload: SignatureUpload): SavedSignature {
            uploads += upload
            responseReady?.await()
            failure?.let { throw it }
            return SavedSignature(upload.purchaseId, upload.baseVersion + 1)
        }
    }

    private class Store : SessionStore {
        override suspend fun readToken() = "synthetic-token"
        override suspend fun writeToken(token: String) = Unit
        override suspend fun clear() = Unit
    }

    private companion object {
        val revision = "a".repeat(64)
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        val png = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7x8AAAAASUVORK5CYII=")
    }
}
