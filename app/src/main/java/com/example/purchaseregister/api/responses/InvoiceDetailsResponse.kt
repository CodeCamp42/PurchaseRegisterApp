package com.example.purchaseregister.api.responses

data class InvoiceDetailsResponse(
    val id: String,
    val userId: String,
    val issuerRuc: String,
    val issuerName: String,
    val period: String,
    val sireCar: String,
    val issueDate: String,
    val docType: String,
    val series: String,
    val number: String,
    val receiverDocType: String,
    val receiverDocNumber: String,
    val receiverName: String,
    val taxableAmount: String,
    val igv: String,
    val nonTaxableAmount: String,
    val totalAmount: String,
    val currency: String,
    val exchangeRate: String,
    val status: String,
    val invoiceStatus: String,
    val createdAt: String,
    val updatedAt: String,
    val invoiceDetails: List<InvoiceDetailItem>,
    val invoiceFiles: List<InvoiceFileItem>,
    val detraction: DetractionItem? = null,
    val paymentMethod: PaymentMethodItem? = null
)

data class InvoiceDetailItem(
    val id: String,
    val invoiceId: String,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val unitOfMeasure: String,
    val lineExtensionAmount: String,
    val referencePriceAmount: String? = null,
    val referencePriceTypeCode: String? = null,
    val isFree: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val invoiceDetailTaxes: List<InvoiceDetailTax> = emptyList()
)

data class InvoiceFileItem(
    val id: String,
    val sunatFileType: String,
    val fileName: String,
    val fileSize: Int?,
    val uploadedAt: String
)

data class InvoiceDetailTax(
    val id: String,
    val invoiceDetailId: String,
    val taxableAmount: String,
    val taxAmount: String,
    val taxCategoryId: String,
    val taxPercent: String,
    val taxExemptionReasonCode: String,
    val taxId: String,
    val taxName: String,
    val taxTypeCode: String
)

data class DetractionItem(
    val id: String,
    val invoiceId: String,
    val bankAccount: String,
    val code: String,
    val percentage: String,
    val amount: String
)

data class PaymentMethodItem(
    val id: String,
    val invoiceId: String,
    val type: String,
    val amount: String? = null
)