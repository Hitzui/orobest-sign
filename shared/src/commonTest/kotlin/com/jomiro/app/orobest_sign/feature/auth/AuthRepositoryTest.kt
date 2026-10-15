package com.jomiro.app.orobest_sign.feature.auth

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.core.network.createHttpClient
import com.jomiro.app.orobest_sign.core.network.createWireJson
import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.auth.data.remote.AuthApi
import com.jomiro.app.orobest_sign.feature.auth.data.repository.DefaultAuthRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthRepositoryTest {
    private class Store : SessionStore {
        var token: String? = null
        override suspend fun readToken() = token
        override suspend fun writeToken(token: String) { this.token = token }
        override suspend fun clear() { token = null }
    }

    private val sessionJson = """{"success":true,"data":{"access_token":"synthetic-token","expires_at":"2027-01-13T00:00:00Z","usuario":{"idusuario":1,"username":"test","nombre":"Usuario"},"agencia":{"idagencia":2,"codagencia":"B","descripcion":"Sucursal B"}}}"""

    @Test
    fun restoresStoredSessionInNewRepositoryAndUsesServerAgency() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/me")) {
                assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            }
            respond(sessionJson, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(engine, createWireJson())
        val store = Store()
        val api = AuthApi(client, ApiConfig("https://example.test/api/sign/v1/"))
        try {
            DefaultAuthRepository(api, store).login("test", "password")
            val recreated = DefaultAuthRepository(api, store)
            assertEquals(2, recreated.restoreSession()?.agency?.id)
            assertEquals("synthetic-token", store.token)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun networkFailureDoesNotErasePersistentToken() = runTest {
        val engine = MockEngine { throw IllegalStateException("synthetic network failure") }
        val client = createHttpClient(engine, createWireJson())
        val store = Store().apply { token = "saved-token" }
        try {
            val repository = DefaultAuthRepository(AuthApi(client, ApiConfig("https://example.test/")), store)
            assertFailsWith<IllegalStateException> { repository.restoreSession() }
            assertEquals("saved-token", store.token)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun serverRejectionClearsExpiredSession() = runTest {
        val engine = MockEngine {
            respond("""{"error":{"code":"invalid_session","message":"Sesión vencida"}}""", HttpStatusCode.Unauthorized,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(engine, createWireJson())
        val store = Store().apply { token = "expired-token" }
        try {
            val repository = DefaultAuthRepository(AuthApi(client, ApiConfig("https://example.test/")), store)
            assertNull(repository.restoreSession())
            assertNull(store.token)
        } finally {
            client.close()
            engine.close()
        }
    }
}
