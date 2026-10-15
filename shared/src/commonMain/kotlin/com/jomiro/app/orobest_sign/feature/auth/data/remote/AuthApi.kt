package com.jomiro.app.orobest_sign.feature.auth.data.remote

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.feature.agency.domain.model.Agency
import com.jomiro.app.orobest_sign.feature.auth.domain.model.AuthException
import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiEnvelope<T>(val data: T)

@Serializable
internal data class LoginRequest(val username: String, val password: String)

@Serializable
internal data class UserDto(val idusuario: Int, val username: String, val nombre: String?)

@Serializable
internal data class AgencyDto(val idagencia: Int, val codagencia: String?, val descripcion: String)

@Serializable
internal data class SessionDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    val usuario: UserDto,
    val agencia: AgencyDto,
) {
    fun toProfile() = SessionProfile(
        userId = usuario.idusuario,
        username = usuario.username,
        name = usuario.nombre,
        agency = Agency(agencia.idagencia, agencia.codagencia, agencia.descripcion),
        expiresAt = expiresAt,
    )
}

@Serializable
private data class ErrorDto(val code: String, val message: String)

@Serializable
private data class ErrorEnvelope(val error: ErrorDto)

internal class AuthApi(private val client: HttpClient, private val config: ApiConfig) {
    suspend fun login(username: String, password: String): SessionDto = translateErrors {
        client.post(config.baseUrl + "auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body<ApiEnvelope<SessionDto>>().data
    }

    suspend fun me(token: String): SessionDto = translateErrors {
        client.get(config.baseUrl + "auth/me") { bearerAuth(token) }.body<ApiEnvelope<SessionDto>>().data
    }

    suspend fun logout(token: String) = translateErrors {
        client.post(config.baseUrl + "auth/logout") { bearerAuth(token) }
        Unit
    }

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val error = try {
            e.response.body<ErrorEnvelope>().error
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ErrorDto("http_${e.response.status.value}", "No fue posible completar la solicitud.")
        }
        throw AuthException(e.response.status.value, error.code, error.message)
    }
}
