package com.jomiro.app.orobest_sign.feature.signature.domain.model

/** Coordenadas normalizadas del lienzo horizontal, independientes de la rotación física. */
data class SignaturePoint(val x: Float, val y: Float)
typealias SignatureStroke = List<SignaturePoint>

data class SignaturePurchase(
    val id: Int,
    val number: String,
    val customer: String?,
    val revision: String,
    val baseVersion: Int,
    val purchaseModified: Boolean,
)

data class SignatureUpload(
    val purchaseId: Int,
    val operationId: String,
    val purchaseRevision: String,
    val baseVersion: Int,
    val png: ByteArray,
)

data class SavedSignature(val purchaseId: Int, val version: Int)
class SignatureException(val statusCode: Int, val code: String) : Exception("Signature request failed ($statusCode, $code)")

interface SignaturePngEncoder {
    fun encode(strokes: List<SignatureStroke>): ByteArray
}
