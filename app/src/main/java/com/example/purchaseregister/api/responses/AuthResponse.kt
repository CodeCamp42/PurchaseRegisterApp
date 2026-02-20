package com.example.purchaseregister.api.responses

data class AuthResponse(
    val user: UserData? = null,
    val session: SessionData? = null,
    val error: String? = null,
    val code: String? = null
)

data class UserData(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
)

data class SessionData(
    val token: String? = null,
)