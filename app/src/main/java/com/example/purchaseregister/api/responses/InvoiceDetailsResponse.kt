package com.example.purchaseregister.api.responses

import com.google.gson.annotations.SerializedName

data class InvoiceDetailsResponse(
    val status: String,
    val message: String? = null,
    val invoice: InvoiceData? = null
)

data class InvoiceData(
    val id: Int,
    @SerializedName("issuerRuc") val issuerRuc: String? = null,
    @SerializedName("issuerName") val issuerName: String? = null,
    val series: String? = null,
    val number: String? = null,
    @SerializedName("issueDate") val issueDate: String? = null,
    @SerializedName("docType") val documentType: String? = null,
    val currency: String? = null,
    @SerializedName("totalAmount") val totalAmount: String? = null,
    @SerializedName("igv") val igv: String? = null,
    @SerializedName("taxableAmount") val taxableAmount: String? = null,
    val details: List<DetailData>? = null
)

data class DetailData(
    val description: String? = null,
    val quantity: String? = null,
    @SerializedName("unitCost") val unitCost: String? = null,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String? = null
)