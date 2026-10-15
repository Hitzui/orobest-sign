package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.engine.HttpClientEngine

internal expect fun createPlatformHttpClientEngine(): HttpClientEngine
