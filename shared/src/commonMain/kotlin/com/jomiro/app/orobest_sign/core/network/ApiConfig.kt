package com.jomiro.app.orobest_sign.core.network

import io.ktor.http.URLProtocol
import io.ktor.http.Url

data class ApiConfig(val baseUrl: String) {
    init {
        val url = Url(baseUrl)
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank()) { "La API requiere HTTPS" }
        require(baseUrl.endsWith('/') && url.parameters.isEmpty() && url.fragment.isEmpty())
        require(url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
    }

    companion object {
        val Production = ApiConfig("https://orobestni.com/api/sign/v1/")
    }
}
