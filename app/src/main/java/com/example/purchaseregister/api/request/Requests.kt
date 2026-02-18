package com.example.purchaseregister.api.requests

data class ProductRequest(
    val description: String? = null,
    val quantity: Double? = null,
    val unitCost: Double? = null,
    val unitOfMeasure: String? = null
)

data class SaveProductsRequest(
    val products: List<ProductRequest>? = null
)

data class ScrapingCompletedRequest(
    val products: List<ProductRequest>? = null
)

data class InvoiceDetailRequest(
    val issuerRuc: String? = null,
    val series: String? = null,
    val number: String? = null,
    val ruc: String? = null,
    val solUser: String? = null,
    val solPassword: String? = null
)

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

data class RegisterInvoiceFromSunatRequest(
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
    val userId: Int = 1
)

data class ValidateCredentialsRequest(
    val ruc: String? = null,
    val user: String? = null,
    val solPassword: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null
)