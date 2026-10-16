package com.jomiro.app.orobest_sign.feature.purchases.data.remote

import com.jomiro.app.orobest_sign.core.network.ApiConfig
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.Purchase
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseException
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchasePage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class PurchasesEnvelope(val data: PurchasesDto)

@Serializable
private data class PurchasesDto(val items: List<PurchaseDto>, val pagination: PaginationDto)

@Serializable
private data class PaginationDto(val page: Int, val total: Int, @SerialName("total_pages") val totalPages: Int)

@Serializable
private data class CustomerDto(val nombre: String)

@Serializable
private data class SignatureDto(val version: Int)

@Serializable
private data class PurchaseDto(
    val idcompra: Int,
    val numcompra: String,
    val fecha: String,
    val total: String?,
    val cliente: CustomerDto? = null,
    val firma: SignatureDto? = null,
) {
    fun toDomain() = Purchase(idcompra, numcompra, fecha, total, cliente?.nombre, firma?.version)
}

internal class PurchasesApi(private val client: HttpClient, private val config: ApiConfig) {
    suspend fun list(token: String, filters: PurchaseFilters, page: Int): PurchasePage = try {
        val data = client.get(config.baseUrl + "compras") {
            bearerAuth(token)
            parameter("page", page)
            parameter("page_size", 20)
            filters.query.takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
            filters.from.takeIf { it.isNotEmpty() }?.let { parameter("desde", it) }
            filters.until.takeIf { it.isNotEmpty() }?.let { parameter("hasta", it) }
            filters.signature.apiValue?.let { parameter("firma", it) }
        }.body<PurchasesEnvelope>().data
        PurchasePage(data.items.map { it.toDomain() }, data.pagination.page, data.pagination.total, data.pagination.totalPages)
    } catch (error: ResponseException) {
        throw PurchaseException(error.response.status.value)
    }
}
