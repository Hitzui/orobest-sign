package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = CIO.create()
