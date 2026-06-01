package com.example.purchaseregister.api.responses

data class SunatCredentialsResponse(
    val id: String? = null,
    val userId: String? = null,
    val ruc: String,
    val solUser: String,
    val solPassword: String,
    val clientId: String,
    val clientSecret: String,
    val businessName: String? = null,
    val address: String? = null,
    val status: String? = null,
    val condition: String? = null,
    val taxingDistrict: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)