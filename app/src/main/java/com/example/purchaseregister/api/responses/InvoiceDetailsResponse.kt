package com.example.purchaseregister.api.responses

import com.google.gson.annotations.SerializedName

data class InvoiceDetailsResponse(
    val id: Int,
    val userId: String,
    @SerializedName("issuerRuc") val issuerRuc: String,
    @SerializedName("issuerName") val issuerName: String,
    val period: String,
    @SerializedName("sireCar") val sireCar: String,
    @SerializedName("issueDate") val issueDate: String,
    @SerializedName("docType") val docType: String,
    val series: String,
    val number: String,
    @SerializedName("receiverDocType") val receiverDocType: String,
    @SerializedName("receiverDocNumber") val receiverDocNumber: String,
    @SerializedName("receiverName") val receiverName: String,
    @SerializedName("taxableAmount") val taxableAmount: String,
    val igv: String,
    @SerializedName("nonTaxableAmount") val nonTaxableAmount: String,
    @SerializedName("totalAmount") val totalAmount: String,
    val currency: String,
    @SerializedName("exchangeRate") val exchangeRate: String,
    val status: String,
    @SerializedName("invoiceStatus") val invoiceStatus: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("invoiceDetails") val invoiceDetails: List<InvoiceDetailItem>,
    @SerializedName("invoiceFiles") val invoiceFiles: List<InvoiceFileItem>
)

data class InvoiceDetailItem(
    val id: Int,
    @SerializedName("invoiceId") val invoiceId: Int,
    @SerializedName("productDescription") val description: String,
    @SerializedName("productCode") val productCode: String? = null,
    @SerializedName("quantity") val quantity: String,
    @SerializedName("unitCost") val unitCost: String,
    @SerializedName("unitOfMeasureCode") val unitOfMeasureCode: String,
    @SerializedName("unitOfMeasureDescription") val unitOfMeasureDescription: String? = null,
    @SerializedName("taxAmount") val taxAmount: String,
    @SerializedName("taxRate") val taxRate: String,
    @SerializedName("unitPriceWithTax") val unitPriceWithTax: String,
    @SerializedName("totalAmount") val totalAmount: String,
    @SerializedName("discountAmount") val discountAmount: String,
    @SerializedName("icbper") val icbper: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class InvoiceFileItem(
    val id: Int,
    @SerializedName("sunatFileType") val sunatFileType: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileSize") val fileSize: Int?,
    @SerializedName("uploadedAt") val uploadedAt: String
)