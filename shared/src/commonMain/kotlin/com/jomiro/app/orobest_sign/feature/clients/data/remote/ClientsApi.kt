package com.jomiro.app.orobest_sign.feature.clients.data.remote

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.feature.clients.domain.model.Client
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentUpload
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientException
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientPage
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable private data class Envelope<T>(val data: T)
@Serializable private data class PaginationDto(val page: Int, val total: Int, @SerialName("total_pages") val totalPages: Int)
@Serializable private data class ClientDto(
    val idcliente: Int, val nombres: String, val apellidos: String,
    val cedula: String? = null, val telefono: String? = null, val celular: String? = null,
) {
    fun toDomain() = Client(idcliente, nombres, apellidos, cedula, telefono, celular)
}
@Serializable private data class ClientListDto(val items: List<ClientDto>, val pagination: PaginationDto)
@Serializable private data class DocumentDto(
    val idarchivo: Int,
    @SerialName("tipo_documento") val documentType: String,
    val nombre: String,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val disponible: Boolean = true,
) {
    fun toDomain() = ClientDocument(
        idarchivo,
        ClientDocumentCategory.entries.firstOrNull { it.apiValue == documentType } ?: ClientDocumentCategory.Other,
        nombre, mimeType, sizeBytes, createdAt, disponible,
    )
}
@Serializable private data class DocumentsDto(val items: List<DocumentDto>)
@Serializable private data class SavedDocumentDto(val documento: DocumentDto)
@Serializable private data class DeletedDocumentDto(val deleted: Boolean)
@Serializable private data class ErrorDto(val code: String)
@Serializable private data class ErrorEnvelope(val error: ErrorDto)

internal class ClientsApi(private val client: HttpClient, private val config: ApiConfig) {
    suspend fun list(token: String, query: String, page: Int): ClientPage = translateErrors {
        val data = client.get(config.baseUrl + "clientes") {
            bearerAuth(token)
            parameter("page", page)
            parameter("page_size", 20)
            query.trim().takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
        }.body<Envelope<ClientListDto>>().data
        ClientPage(data.items.map(ClientDto::toDomain), data.pagination.page, data.pagination.total, data.pagination.totalPages)
    }

    suspend fun documents(token: String, clientId: Int): List<ClientDocument> = translateErrors {
        client.get(config.baseUrl + "clientes/$clientId/documentos") { bearerAuth(token) }
            .body<Envelope<DocumentsDto>>().data.items.map(DocumentDto::toDomain)
    }

    suspend fun content(token: String, clientId: Int, document: ClientDocument): ClientFile = translateErrors {
        val response = client.get(config.baseUrl + "clientes/$clientId/documentos/${document.id}/contenido") {
            bearerAuth(token)
            header(HttpHeaders.Accept, "*/*")
        }
        // Los documentos históricos pueden guardar "jpg" o "pdf" en lugar de MIME.
        // Usar el tipo real entregado por el endpoint, no el metadato histórico.
        val mime = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
        if (mime !in setOf("image/jpeg", "image/png", "application/pdf", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw ClientException(422, "unsupported_document")
        }
        val bytes = response.body<ByteArray>()
        if (bytes.isEmpty()) throw ClientException(422, "empty_document")
        ClientFile(bytes, document.name, requireNotNull(mime))
    }

    suspend fun upload(token: String, upload: ClientDocumentUpload): ClientDocument = translateErrors {
        val data = client.post(config.baseUrl + "clientes/${upload.clientId}/documentos") {
            bearerAuth(token)
            setBody(MultiPartFormDataContent(formData {
                append("operation_id", upload.operationId)
                append("tipo_documento", upload.category.apiValue)
                append("nombre", upload.displayName)
                append("archivo", upload.file.bytes, Headers.build {
                    append(HttpHeaders.ContentType, upload.file.mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${upload.file.fileName.replace('"', '_')}\"")
                })
            }))
        }.body<Envelope<SavedDocumentDto>>().data.documento
        data.toDomain()
    }

    suspend fun delete(token: String, clientId: Int, documentId: Int): Unit = translateErrors {
        val result = client.delete(config.baseUrl + "clientes/$clientId/documentos/$documentId") { bearerAuth(token) }
            .body<Envelope<DeletedDocumentDto>>().data
        if (!result.deleted) throw ClientException(422, "delete_not_confirmed")
    }

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (error: ResponseException) {
        val code = try {
            error.response.body<ErrorEnvelope>().error.code
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        throw ClientException(error.response.status.value, code)
    }
}
