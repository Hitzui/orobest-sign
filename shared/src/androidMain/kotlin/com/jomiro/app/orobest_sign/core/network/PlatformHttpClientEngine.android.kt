package com.jomiro.app.orobest_sign.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create()
