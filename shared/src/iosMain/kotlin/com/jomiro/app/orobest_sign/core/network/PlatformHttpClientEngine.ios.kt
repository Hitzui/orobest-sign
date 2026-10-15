package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create()
