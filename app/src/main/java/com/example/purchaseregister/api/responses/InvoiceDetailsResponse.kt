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
    @SerializedName("invoiceDetails") val invoiceDetails: List<InvoiceDetailItem>
)

data class InvoiceDetailItem(
    val id: Int,
    @SerializedName("invoiceId") val invoiceId: Int,
    val description: String,
    val quantity: String,
    @SerializedName("unitCost") val unitCost: String,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)