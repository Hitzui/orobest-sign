package com.jomiro.app.orobest_sign.core.network

internal actual fun platformApiConfig(): ApiConfig {
    val configured = System.getProperty("orobest.api.baseUrl") ?: System.getenv("OROBEST_API_BASE_URL")
    return configured?.takeIf(String::isNotBlank)?.let(::ApiConfig) ?: ApiConfig.Production
}
