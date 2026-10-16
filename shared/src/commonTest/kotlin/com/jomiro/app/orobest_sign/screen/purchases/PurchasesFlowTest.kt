package com.jomiro.app.orobest_sign.screen.purchases

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.purchases.data.remote.PurchasesApi
import com.jomiro.app.orobest_sign.feature.purchases.data.repository.DefaultPurchasesRepository
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.Purchase
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchasePage
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.SignatureFilter
import com.jomiro.app.orobest_sign.feature.purchases.domain.repository.PurchasesRepository
import com.jomiro.app.orobest_sign.screen.purchases.viewmodel.PurchasesViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PurchasesFlowTest {
    @BeforeTest
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun sendsOnlySupportedFiltersWithBearerAndResetsPaginationWhenFiltering() = runTest {
        val queries = mutableListOf<Map<String, String>>()
        val engine = mockEngine { request ->
            assertEquals("/api/sign/v1/compras", request.url.encodedPath)
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            queries += request.url.parameters.names().associateWith { request.url.parameters[it].orEmpty() }
            respond(pageJson(request.url.parameters["page"]!!.toInt()), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = PurchasesViewModel(DefaultPurchasesRepository(PurchasesApi(client, config), Store()))
            viewModel.awaitIdle()
            assertEquals(mapOf("page" to "1", "page_size" to "20"), queries.last())
            assertEquals("123456789.123", viewModel.state.value.items[0].total)
            assertEquals(2, viewModel.state.value.items[0].signatureVersion)
            assertNull(viewModel.state.value.items[1].signatureVersion)
            assertNull(viewModel.state.value.items[1].customerName)
            assertNull(viewModel.state.value.items[1].total)

            viewModel.goToPage(2)
            viewModel.awaitIdle()
            assertEquals(2, viewModel.state.value.page)
            viewModel.applyFilters(PurchaseFilters(" A-12 ", "2026-10-01", "2026-10-15", SignatureFilter.Pending))
            viewModel.awaitIdle()
            assertEquals(1, viewModel.state.value.page)
            assertEquals(mapOf("page" to "1", "page_size" to "20", "q" to "A-12", "desde" to "2026-10-01", "hasta" to "2026-10-15", "firma" to "pendiente"), queries.last())
            assertFalse(queries.last().keys.any { it in setOf("idagencia", "idcaja", "idusuario") })

            viewModel.applyFilters(PurchaseFilters(signature = SignatureFilter.Signed))
            viewModel.awaitIdle()
            assertEquals("firmada", queries.last()["firma"])
            viewModel.applyFilters(PurchaseFilters())
            viewModel.awaitIdle()
            assertEquals(mapOf("page" to "1", "page_size" to "20"), queries.last())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun invalidDatesDoNotSendRequestOrReplaceCurrentResults() = runTest {
        var requests = 0
        val repository = object : PurchasesRepository {
            override suspend fun list(filters: PurchaseFilters, page: Int): PurchasePage {
                requests++
                return PurchasePage(listOf(purchase(1)), page, 1, 1)
            }
        }
        val viewModel = PurchasesViewModel(repository)
        viewModel.awaitIdle()
        listOf(
            PurchaseFilters(from = "2026-02-29"),
            PurchaseFilters(until = "2026-04-31"),
            PurchaseFilters(from = "15/10/2026"),
            PurchaseFilters(from = "2026-10-16", until = "2026-10-15"),
        ).forEach { filters ->
            viewModel.applyFilters(filters)
            assertNotNull(viewModel.state.value.filterError)
            assertEquals(1, requests)
            assertEquals(1, viewModel.state.value.items.single().id)
        }
        assertNull(PurchaseFilters(from = "2024-02-29", until = "2024-02-29").validationError())
    }

    @Test
    fun offlineRetryRetainsFiltersAndAccessRejectionRequestsSessionValidation() = runTest {
        var fail = false
        var forbidden = false
        val engine = mockEngine {
            check(!fail) { "Synthetic offline response" }
            if (forbidden) respond("{}", HttpStatusCode.Forbidden, jsonHeaders)
            else respond(pageJson(), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        val store = Store()
        try {
            val viewModel = PurchasesViewModel(DefaultPurchasesRepository(PurchasesApi(client, config), store))
            viewModel.awaitIdle()
            fail = true
            val filters = PurchaseFilters(query = "A-12", signature = SignatureFilter.Pending)
            viewModel.applyFilters(filters)
            viewModel.awaitIdle()
            assertNotNull(viewModel.state.value.error)
            assertTrue(viewModel.state.value.items.isEmpty())
            assertFalse(viewModel.state.value.requiresSessionCheck)
            assertEquals("synthetic-token", store.readToken())
            fail = false
            viewModel.refresh()
            viewModel.awaitIdle()
            assertNull(viewModel.state.value.error)
            assertEquals(filters, viewModel.state.value.filters)
            forbidden = true
            viewModel.refresh()
            viewModel.awaitIdle()
            assertTrue(viewModel.state.value.requiresSessionCheck)
            assertTrue(viewModel.state.value.items.isEmpty())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun lateCancelledResponseCannotOverwriteNewFilters() = runTest {
        val oldResponse = CompletableDeferred<Unit>()
        val repository = object : PurchasesRepository {
            override suspend fun list(filters: PurchaseFilters, page: Int): PurchasePage {
                if (filters.query.isEmpty()) withContext(NonCancellable) { oldResponse.await() }
                return PurchasePage(listOf(purchase(if (filters.query.isEmpty()) 1 else 2)), 1, 1, 1)
            }
        }
        val viewModel = PurchasesViewModel(repository)
        runCurrent()
        viewModel.applyFilters(PurchaseFilters(query = "new"))
        viewModel.awaitIdle()
        assertEquals(2, viewModel.state.value.items.single().id)
        oldResponse.complete(Unit)
        advanceUntilIdle()
        assertEquals("new", viewModel.state.value.filters.query)
        assertEquals(2, viewModel.state.value.items.single().id)
    }

    @Test
    fun presentationPreservesDecimalPrecisionAndAgencyCalendarDate() {
        assertEquals("123,456,789.123", formatPurchaseTotal("123456789.123"))
        assertEquals("0.001", formatPurchaseTotal("0.001"))
        assertEquals("—", formatPurchaseTotal(null))
        assertEquals("15/10/2026 · 23:30", formatPurchaseDate("2026-10-15T23:30:00-06:00"))
    }

    private fun TestScope.mockEngine(handler: MockRequestHandler) = MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })

    private suspend fun PurchasesViewModel.awaitIdle() = withTimeout(5_000) { state.first { !it.loading } }

    private class Store : SessionStore {
        private var token: String? = "synthetic-token"
        override suspend fun readToken() = token
        override suspend fun writeToken(token: String) { this.token = token }
        override suspend fun clear() { token = null }
    }

    private fun purchase(id: Int) = Purchase(id, "A-$id", "2026-10-15T10:30:00-06:00", "1.000", null, null)
    private val config = ApiConfig("https://example.test/api/sign/v1/")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun pageJson(page: Int = 1) = """{"success":true,"data":{"items":[{"idcompra":1,"numcompra":"A-12","fecha":"2026-10-15T23:30:00-06:00","estado":1,"total":"123456789.123","cliente":{"idcliente":1,"nombre":"Cliente de prueba"},"firma":{"idfirma":4,"version":2}},{"idcompra":2,"numcompra":"A-11","fecha":"2026-10-14T10:00:00-06:00","estado":1,"total":null,"cliente":null,"firma":null}],"pagination":{"page":$page,"page_size":20,"total":22,"total_pages":2}}}"""
}
