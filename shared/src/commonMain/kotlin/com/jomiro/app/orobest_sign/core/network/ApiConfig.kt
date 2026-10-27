package com.jomiro.app.orobest_sign.core.network

import io.ktor.http.URLProtocol
import io.ktor.http.Url

data class ApiConfig(val baseUrl: String) {
    init {
        val url = Url(baseUrl)
        val isLoopbackHttp = url.protocol == URLProtocol.HTTP &&
            url.host.lowercase() in setOf("localhost", "127.0.0.1", "::1")
        require((url.protocol == URLProtocol.HTTPS || isLoopbackHttp) && url.host.isNotBlank()) {
            "La API requiere HTTPS, excepto HTTP local en localhost"
        }
        require(baseUrl.endsWith('/') && url.parameters.isEmpty() && url.fragment.isEmpty())
        require(url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
    }

    companion object {
        val Production = ApiConfig("https://orobestni.com/api/sign/v1/")
        val Localhost = ApiConfig("http://localhost/orobest/api/sign/v1/")
    }
}
