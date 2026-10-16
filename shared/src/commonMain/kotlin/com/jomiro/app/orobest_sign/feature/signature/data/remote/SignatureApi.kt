package com.jomiro.app.orobest_sign.feature.signature.data.remote

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SavedSignature
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureException
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePurchase
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureUpload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable private data class Envelope<T>(val data: T)
@Serializable private data class CustomerDto(val nombre: String)
@Serializable private data class SignatureDto(
    val idcompra: Int,
    val version: Int,
    @SerialName("compra_modificada") val modified: Boolean = false,
)
@Serializable private data class PurchaseDto(
    val idcompra: Int, val numcompra: String, val estado: Int, val revision: String,
    val cliente: CustomerDto? = null, val firma: SignatureDto? = null,
)
@Serializable private data class SavedDto(val firma: SignatureDto)
@Serializable private data class ErrorDto(val code: String)
@Serializable private data class ErrorEnvelope(val error: ErrorDto)

internal class SignatureApi(private val client: HttpClient, private val config: ApiConfig) {
    suspend fun purchase(token: String, id: Int): SignaturePurchase = translateErrors {
        val data = client.get(config.baseUrl + "compras/$id") { bearerAuth(token) }.body<Envelope<PurchaseDto>>().data
        if (data.estado != 1) throw SignatureException(409, "purchase_not_active")
        check(data.idcompra == id && Regex("[0-9a-f]{64}").matches(data.revision))
        SignaturePurchase(data.idcompra, data.numcompra, data.cliente?.nombre, data.revision, data.firma?.version ?: 0, data.firma?.modified ?: false)
    }

    suspend fun image(token: String, id: Int, version: Int): ByteArray = translateErrors {
        client.get(config.baseUrl + "compras/$id/firma") {
            bearerAuth(token)
            parameter("version", version)
        }.body<ByteArray>()
    }

    suspend fun save(token: String, upload: SignatureUpload): SavedSignature = translateErrors {
        val data = client.post(config.baseUrl + "compras/${upload.purchaseId}/firma") {
            bearerAuth(token)
            setBody(MultiPartFormDataContent(formData {
                append("operation_id", upload.operationId)
                append("compra_revision", upload.purchaseRevision)
                append("base_version", upload.baseVersion.toString())
                append("firma", upload.png, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"firma.png\"")
                })
            }))
        }.body<Envelope<SavedDto>>().data.firma
        check(data.idcompra == upload.purchaseId && data.version == upload.baseVersion + 1)
        SavedSignature(data.idcompra, data.version)
    }

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (error: ResponseException) {
        val code = try {
            error.response.body<ErrorEnvelope>().error.code
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "http_${error.response.status.value}"
        }
        throw SignatureException(error.response.status.value, code)
    }
}
