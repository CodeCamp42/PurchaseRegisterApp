package com.example.purchaseregister.api.requests

data class ProductRequest(
    val description: String,
    val quantity: Double,
    val unitCost: Double,
    val unitOfMeasure: String
)

data class SaveProductsRequest(
    val products: List<ProductRequest>
)

data class ScrapingCompletedRequest(
    val products: List<ProductRequest>? = null
)

data class InvoiceDetailRequest(
    val issuerRuc: String,
    val series: String,
    val number: String,
    val ruc: String,
    val solUser: String,
    val solPassword: String
)

data class ProductToRegister(
    val description: String,
    val quantity: String,
    val unitCost: String,
    val unitOfMeasure: String
)

data class InvoiceToRegister(
    val id: Int,
    val issuerRuc: String,
    val series: String,
    val number: String,
    val issueDate: String,
    val businessName: String,
    val documentType: String,
    val currency: String,
    val totalCost: String,
    val igv: String,
    val totalAmount: String,
    val products: List<ProductToRegister>
)

data class RegisterInvoicesRequest(
    val invoices: List<InvoiceToRegister>
)

data class RegisterInvoiceFromSunatRequest(
    val issuerRuc: String,
    val series: String,
    val number: String,
    val issueDate: String,
    val businessName: String,
    val documentType: String,
    val currency: String,
    val totalCost: String,
    val igv: String,
    val totalAmount: String,
    val userId: Int = 1
)

data class ValidateCredentialsRequest(
    val ruc: String,
    val user: String,
    val solPassword: String
)