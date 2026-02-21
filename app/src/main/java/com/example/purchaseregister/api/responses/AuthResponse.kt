package com.example.purchaseregister.api.responses

data class AuthResponse(
    val token: String? = null,
    val user: UserData? = null,
    val message: String? = null,
    val code: String? = null
)

data class UserData(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
)