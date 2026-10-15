package com.jomiro.app.orobest_sign.feature.auth.domain.model

import com.jomiro.app.orobest_sign.feature.agency.domain.model.Agency

data class SessionProfile(
    val userId: Int,
    val username: String,
    val name: String?,
    val agency: Agency,
    val expiresAt: String,
)

class AuthException(val statusCode: Int, val errorCode: String, message: String) : Exception(message)
