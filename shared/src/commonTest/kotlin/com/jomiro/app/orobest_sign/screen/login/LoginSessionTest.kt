package com.jomiro.app.orobest_sign.screen.login

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.auth.data.remote.AuthApi
import com.jomiro.app.orobest_sign.feature.auth.data.repository.DefaultAuthRepository
import com.jomiro.app.orobest_sign.screen.login.uistate.LoginPhase
import com.jomiro.app.orobest_sign.screen.login.viewmodel.LoginViewModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginSessionTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginPersistsSessionAndRecreatedScreenRestoresCurrentAgency() = runTest {
        val store = Store()
        val engine = mockEngine { request ->
            val restoring = request.url.encodedPath.endsWith("/me")
            if (restoring) assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            respond(sessionJson(if (restoring) 3 else 2), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            fun newViewModel() = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), store))
            val first = newViewModel()
            first.awaitIdle()
            assertEquals(LoginPhase.SignIn, first.state.value.phase)
            first.login(" test ", "synthetic-password")
            first.awaitIdle()
            assertEquals(LoginPhase.Authenticated, first.state.value.phase)
            assertEquals("synthetic-token", store.token)

            val recreated = newViewModel()
            assertEquals(LoginPhase.RestoringSession, recreated.state.value.phase)
            recreated.awaitIdle()
            assertEquals(LoginPhase.Authenticated, recreated.state.value.phase)
            assertEquals(3, recreated.state.value.profile?.agency?.id)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun offlineRestoreKeepsSessionAndRetryAuthenticates() = runTest {
        val store = Store().apply { token = "synthetic-token" }
        var online = false
        val engine = mockEngine {
            check(online) { "Synthetic network failure" }
            respond(sessionJson(), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), store))
            viewModel.awaitIdle()
            assertEquals(LoginPhase.RestoreFailed, viewModel.state.value.phase)
            assertNull(viewModel.state.value.profile)
            assertEquals("synthetic-token", store.token)
            online = true
            viewModel.restoreSession()
            viewModel.awaitIdle()
            assertEquals(LoginPhase.Authenticated, viewModel.state.value.phase)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun expiredSessionReturnsToLoginAndClearsToken() = runTest {
        val store = Store().apply { token = "expired-token" }
        val engine = mockEngine {
            respond("""{"error":{"code":"invalid_session","message":"Sesión vencida"}}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), store))
            viewModel.awaitIdle()
            assertEquals(LoginPhase.SignIn, viewModel.state.value.phase)
            assertNull(viewModel.state.value.profile)
            assertNull(store.token)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun emptyFieldsDoNotSendRequestAndRepeatedSubmitOnlyLogsInOnce() = runTest {
        var requests = 0
        val responseReady = CompletableDeferred<Unit>()
        val engine = mockEngine {
            requests++
            responseReady.await()
            respond(sessionJson(), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), Store()))
            viewModel.awaitIdle()
            viewModel.login(" ", "")
            assertNotNull(viewModel.state.value.usernameError)
            assertNotNull(viewModel.state.value.passwordError)
            assertEquals(0, requests)
            viewModel.login("test", "synthetic-password")
            runCurrent()
            viewModel.login("test", "synthetic-password")
            assertEquals(LoginPhase.SigningIn, viewModel.state.value.phase)
            responseReady.complete(Unit)
            viewModel.awaitIdle()
            assertEquals(1, requests)
            assertEquals(LoginPhase.Authenticated, viewModel.state.value.phase)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun logoutWithoutNetworkStillReturnsToLoginAndErasesLocalSession() = runTest {
        val store = Store().apply { token = "synthetic-token" }
        val engine = mockEngine { request ->
            check(!request.url.encodedPath.endsWith("/logout")) { "Synthetic network failure" }
            respond(sessionJson(), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), store))
            viewModel.awaitIdle()
            viewModel.logout()
            viewModel.awaitIdle()
            assertEquals(LoginPhase.SignIn, viewModel.state.value.phase)
            assertNotNull(viewModel.state.value.notice)
            assertNull(viewModel.state.value.profile)
            assertNull(store.token)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun expiredSessionWhileBrowsingReturnsToLoginOnRevalidation() = runTest {
        val store = Store().apply { token = "synthetic-token" }
        var expired = false
        val engine = mockEngine {
            if (expired) respond("""{"error":{"code":"invalid_session","message":"Sesión vencida"}}""", HttpStatusCode.Unauthorized, jsonHeaders)
            else respond(sessionJson(), headers = jsonHeaders)
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            val viewModel = LoginViewModel(DefaultAuthRepository(AuthApi(client, config), store))
            viewModel.awaitIdle()
            assertEquals(LoginPhase.Authenticated, viewModel.state.value.phase)
            expired = true
            viewModel.restoreSession()
            viewModel.awaitIdle()
            assertEquals(LoginPhase.SignIn, viewModel.state.value.phase)
            assertNull(store.token)
            assertNull(viewModel.state.value.profile)
        } finally {
            client.close()
            engine.close()
        }
    }

    private fun TestScope.mockEngine(handler: MockRequestHandler) = MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })

    private suspend fun LoginViewModel.awaitIdle() = withTimeout(5_000) {
        state.first { it.phase !in setOf(LoginPhase.RestoringSession, LoginPhase.SigningIn, LoginPhase.SigningOut) }
    }

    private class Store : SessionStore {
        var token: String? = null
        override suspend fun readToken() = token
        override suspend fun writeToken(token: String) { this.token = token }
        override suspend fun clear() { token = null }
    }

    private val config = ApiConfig("https://example.test/api/sign/v1/")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun sessionJson(agencyId: Int = 2) = """{"success":true,"data":{"access_token":"synthetic-token","expires_at":"2027-01-13T00:00:00Z","usuario":{"idusuario":1,"username":"test","nombre":"Usuario de prueba"},"agencia":{"idagencia":$agencyId,"codagencia":"B","descripcion":"Sucursal de prueba"}}}"""
}
