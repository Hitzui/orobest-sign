package com.jomiro.app.orobest_sign.feature.clients.domain.model

data class Client(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val identityNumber: String?,
    val phone: String?,
    val mobile: String?,
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

enum class ClientDocumentCategory(val apiValue: String, val displayName: String) {
    IdentityFront("cedula_frente", "Cédula · Frente"),
    IdentityBack("cedula_reverso", "Cédula · Reverso"),
    Other("otro", "Otro documento"),
}

data class ClientDocument(
    val id: Int,
    val category: ClientDocumentCategory,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val createdAt: String?,
    val available: Boolean,
)

data class ClientPage(val items: List<Client>, val page: Int, val total: Int, val totalPages: Int)

data class ClientFile(val bytes: ByteArray, val fileName: String, val mimeType: String)

data class ClientDocumentUpload(
    val clientId: Int,
    val category: ClientDocumentCategory,
    val operationId: String,
    val displayName: String,
    val file: ClientFile,
)

class ClientException(val statusCode: Int, val code: String? = null) : Exception("Client API failed ($statusCode)")
