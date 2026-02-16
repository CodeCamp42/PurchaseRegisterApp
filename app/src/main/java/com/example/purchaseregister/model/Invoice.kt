package com.example.purchaseregister.model

import kotlinx.serialization.Serializable

@Serializable
data class Invoice(
    val id: Int,
    val ruc: String,
    val businessName: String,
    val series: String,
    val number: String,
    val issueDate: String,
    val documentType: String,
    val year: String = "",
    val currency: String = "",
    val totalCost: String = "",
    val igv: String = "",
    val exchangeRate: String = "",
    val totalAmount: String = "",
    var status: String = "CONSULTADO",
    var isSelected: Boolean = false,
    val products: List<ProductItem> = emptyList()
)