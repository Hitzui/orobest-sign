package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun createWireJson(): Json = Json {
    ignoreUnknownKeys = true
}

internal fun createHttpClient(engine: HttpClientEngine, wireJson: Json): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        // La autenticación y la URL se definirán al aprobar el contrato de la API.
        followRedirects = false
        install(ContentNegotiation) {
            json(wireJson)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        defaultRequest {
            accept(ContentType.Application.Json)
        }
    }
