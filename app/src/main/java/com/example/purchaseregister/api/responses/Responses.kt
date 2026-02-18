package com.example.purchaseregister.api.responses

data class SaveProductsResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val savedProducts: Int? = null,
    val invoiceId: Int? = null,
    val updatedStatus: Boolean? = null
)

data class InvoiceUIResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val invoice: RegisteredInvoiceResponse? = null,
    val note: String? = null
)

data class InvoicesUIResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val count: Int? = null,
    val statusDistribution: Map<String, Int>? = null,
    val invoices: List<RegisteredInvoiceResponse>? = null,
    val note: String? = null
)

data class InvoiceDetailXmlResponse(
    val id: String? = null,
    val issueDate: String? = null,
    val issueTime: String? = null,
    val currency: String? = null,
    val issuer: IssuerResponse? = null,
    val receiver: ReceiverResponse? = null,
    val subtotal: Double? = null,
    val igv: Double? = null,
    val total: Double? = null,
    val items: List<ItemResponse>? = null,
    val xmlFile: String? = null
)

data class IssuerResponse(
    val ruc: String? = null,
    val name: String? = null
)

data class ReceiverResponse(
    val ruc: String? = null,
    val name: String? = null
)

data class ItemResponse(
    val quantity: Double? = null,
    val unit: String? = null,
    val code: String? = null,
    val description: String? = null,
    val unitValue: Double? = null
)

data class SunatResponse(
    val success: Boolean? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val results: List<SunatResult>? = null
)

data class SunatResult(
    val period: String? = null,
    val content: List<ContentItem>? = null
)

data class ContentItem(
    val issuerRuc: String? = null,
    val issuerBusinessName: String? = null,
    val period: String? = null,
    val sunatFile: String? = null,
    val issueDate: String? = null,
    val documentType: String? = null,
    val series: String? = null,
    val number: String? = null,
    val receiverDocType: String? = null,
    val receiverDocNumber: String? = null,
    val receiverName: String? = null,
    val taxableBase: Double? = null,
    val igv: Double? = null,
    val nonTaxedAmount: Double? = null,
    val total: Double? = null,
    val currency: String? = null,
    val exchangeRate: Double? = null,
    val status: String? = null
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

data class ScrapingCompletedResponse(
    val message: String? = null,
    val timestamp: String? = null,
    val invoice: ScrapedInvoiceResponse? = null,
    val status: String? = null,
    val savedProducts: Int? = null,
    val warning: String? = null
)

data class ScrapedInvoiceResponse(
    val invoiceId: Int? = null,
    val documentNumber: String? = null,
    val status: String? = null
)

data class RegisterInvoiceFromSunatResponse(
    val success: Boolean? = null,
    val invoiceId: Int? = null,
    val documentNumber: String? = null,
    val message: String? = null
)

data class QueuedResponse(
    val success: Boolean? = null,
    val jobId: String? = null,
    val message: String? = null
)

data class JobStatusResponse(
    val id: String? = null,
    val state: String? = null,
    val progress: Int? = null,
    val result: JobResult? = null,
    val reason: String? = null
)

data class JobResult(
    val id: String? = null,
    val issueDate: String? = null,
    val issueTime: String? = null,
    val currency: String? = null,
    val issuer: IssuerResponse? = null,
    val receiver: ReceiverResponse? = null,
    val subtotal: Double? = null,
    val igv: Double? = null,
    val total: Double? = null,
    val items: List<ItemResponse>? = null,
    val xmlFile: String? = null
)

data class ValidateCredentialsResponse(
    val valid: Boolean? = null,
    val message: String? = null,
    val token: String? = null
)