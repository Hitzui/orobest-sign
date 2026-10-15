package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpClientFactoryTest {
    @Serializable
    private data class Probe(val value: String)

    @Test
    fun readsJsonWithAdditionalServerFields() = runTest {
        val engine = MockEngine { request ->
            assertEquals(ContentType.Application.Json.toString(), request.headers[HttpHeaders.Accept])
            respond(
                content = """{"value":"ok","future_field":true}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = createHttpClient(engine, createWireJson())
        try {
            assertEquals("ok", client.get("https://example.test/probe").body<Probe>().value)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun unauthorizedResponseIsNotTreatedAsSuccess() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Unauthorized) }
        val client = createHttpClient(engine, createWireJson())
        try {
            assertFailsWith<ClientRequestException> {
                client.get("https://example.test/probe")
            }
        } finally {
            client.close()
            engine.close()
        }
    }
}
