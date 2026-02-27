package com.example.purchaseregister.api.responses
data class SunatResponse(
    val id: Int,
    val issuerRuc: String,
    val issuerName: String,
    val period: String,
    val sunatCar: String,
    val issueDate: String,
    val docType: String,
    val series: String,
    val number: String,
    val receiverDocType: String,
    val receiverDocNumber: String,
    val receiverName: String,
    val taxableAmount: Double,
    val igv: Double,
    val nonTaxableAmount: Double,
    val totalAmount: Double,
    val currency: String,
    val exchangeRate: Double,
    val status: String,
    val invoiceStatus: String
)

data class RegisterInvoicesResponse(
    val message: String? = null,
    val results: List<RegistrationResult>? = null
)

data class RegistrationResult(
    val success: Boolean? = null,
    val id: Int? = null,
    val documentNumber: String? = null
)

data class RegisteredInvoiceResponse(
    val invoiceId: Int? = null,
    val documentNumber: String? = null,
    val issueDate: String? = null,
    val status: String? = null,
    val providerRuc: String? = null,
    val totalCost: String? = null,
    val igv: String? = null,
    val totalAmount: String? = null,
    val currency: String? = null,
    val number: String? = null,
    val series: String? = null,
    val details: List<RegisteredDetail>? = null,
    val provider: RegisteredProvider? = null
)

data class RegisteredDetail(
    val description: String? = null,
    val quantity: String? = null,
    val unitCost: String? = null,
    val unitOfMeasure: String? = null
)

data class RegisteredProvider(
    val providerRuc: String? = null,
    val businessName: String? = null
)

data class SaveSunatCredentialsResponse(
    val success: Boolean? = null,
    val message: String? = null
)