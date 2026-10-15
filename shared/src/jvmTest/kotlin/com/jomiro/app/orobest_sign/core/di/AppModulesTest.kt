package com.jomiro.app.orobest_sign.core.di

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppModulesTest {
    @Test
    fun resolvesSharedNetworkDependencies() {
        val application = koinApplication { modules(appModules) }
        try {
            val client = application.koin.get<HttpClient>()
            assertSame(client, application.koin.get<HttpClient>())
            assertTrue(application.koin.get<Json>().configuration.ignoreUnknownKeys)
        } finally {
            application.close()
        }
    }
}
