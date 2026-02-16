package com.example.purchaseregister.api.responses

data class SaveProductsResponse(
    val success: Boolean,
    val message: String,
    val savedProducts: Int,
    val invoiceId: Int?,
    val updatedStatus: Boolean?
)

data class InvoiceUIResponse(
    val success: Boolean,
    val message: String,
    val invoice: RegisteredInvoiceResponse,
    val note: String
)

data class InvoicesUIResponse(
    val success: Boolean,
    val message: String,
    val count: Int,
    val statusDistribution: Map<String, Int>,
    val invoices: List<RegisteredInvoiceResponse>,
    val note: String
)

data class InvoiceDetailXmlResponse(
    val id: String,
    val issueDate: String?,
    val issueTime: String?,
    val currency: String?,
    val issuer: IssuerResponse?,
    val receiver: ReceiverResponse?,
    val subtotal: Double?,
    val igv: Double?,
    val total: Double?,
    val items: List<ItemResponse>?,
    val xmlFile: String?
)

data class IssuerResponse(val ruc: String?, val name: String?)
data class ReceiverResponse(val ruc: String?, val name: String?)

data class ItemResponse(
    val quantity: Double,
    val unit: String,
    val code: String?,
    val description: String,
    val unitValue: Double
)

data class SunatResponse(
    val success: Boolean,
    val periodStart: String,
    val periodEnd: String,
    val results: List<SunatResult>
)

data class SunatResult(
    val period: String,
    val content: List<ContentItem>
)

data class ContentItem(
    val issuerRuc: String,
    val issuerBusinessName: String,
    val period: String,
    val sunatFile: String,
    val issueDate: String,
    val documentType: String,
    val series: String,
    val number: String,
    val receiverDocType: String,
    val receiverDocNumber: String,
    val receiverName: String,
    val taxableBase: Double,
    val igv: Double,
    val nonTaxedAmount: Double,
    val total: Double,
    val currency: String,
    val exchangeRate: Double?,
    val status: String
)

data class RegisterInvoicesResponse(
    val message: String,
    val results: List<RegistrationResult>
)

data class RegistrationResult(
    val success: Boolean,
    val id: Int,
    val documentNumber: String
)

data class RegisteredInvoiceResponse(
    val invoiceId: Int,
    val documentNumber: String,
    val issueDate: String,
    val status: String,
    val providerRuc: String,
    val totalCost: String,
    val igv: String,
    val totalAmount: String,
    val currency: String,
    val number: String,
    val series: String,
    val details: List<RegisteredDetail>?,
    val provider: RegisteredProvider?
)

data class RegisteredDetail(
    val description: String,
    val quantity: String,
    val unitCost: String,
    val unitOfMeasure: String
)

data class RegisteredProvider(
    val providerRuc: String,
    val businessName: String
)

data class ScrapingCompletedResponse(
    val message: String,
    val timestamp: String,
    val invoice: ScrapedInvoiceResponse?,
    val status: String?,
    val savedProducts: Int?,
    val warning: String?
)

data class ScrapedInvoiceResponse(
    val invoiceId: Int,
    val documentNumber: String,
    val status: String
)

data class RegisterInvoiceFromSunatResponse(
    val success: Boolean,
    val invoiceId: Int?,
    val documentNumber: String,
    val message: String
)

data class QueuedResponse(
    val success: Boolean,
    val jobId: String,
    val message: String
)

data class JobStatusResponse(
    val id: String,
    val state: String,
    val progress: Int,
    val result: JobResult?,
    val reason: String?
)

data class JobResult(
    val id: String,
    val issueDate: String?,
    val issueTime: String?,
    val currency: String?,
    val issuer: IssuerResponse?,
    val receiver: ReceiverResponse?,
    val subtotal: Double?,
    val igv: Double?,
    val total: Double?,
    val items: List<ItemResponse>?,
    val xmlFile: String?
)

data class ValidateCredentialsResponse(
    val valid: Boolean,
    val message: String? = null,
    val token: String? = null
)