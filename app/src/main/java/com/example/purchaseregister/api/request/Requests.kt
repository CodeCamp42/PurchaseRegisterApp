package com.example.purchaseregister.api.request

import com.google.gson.annotations.SerializedName

data class ProductToRegister(
    val description: String? = null,
    val quantity: String? = null,
    val unitCost: String? = null,
    val unitOfMeasure: String? = null
)

data class InvoiceToRegister(
    val id: Int? = null,
    val issuerRuc: String? = null,
    val series: String? = null,
    val number: String? = null,
    val issueDate: String? = null,
    val businessName: String? = null,
    val documentType: String? = null,
    val currency: String? = null,
    val totalCost: String? = null,
    val igv: String? = null,
    val totalAmount: String? = null,
    val products: List<ProductToRegister>? = null
)

data class RegisterInvoicesRequest(
    val invoices: List<InvoiceToRegister>? = null
)

data class SaveSunatCredentialsRequest(
    val ruc: String? = null,
    val solUser: String? = null,
    val solPassword: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class UpdateSunatCredentialsRequest(
    val ruc: String? = null,
    @SerializedName("solUser") val solUser: String? = null,
    @SerializedName("solPassword") val solPassword: String? = null,
    @SerializedName("clientId") val clientId: String? = null,
    @SerializedName("clientSecret") val clientSecret: String? = null
)