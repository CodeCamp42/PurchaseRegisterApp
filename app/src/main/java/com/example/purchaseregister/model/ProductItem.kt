package com.example.purchaseregister.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductItem(
    val description: String = "",
    val unitCost: String = "0",
    val quantity: String = "0",
    val unitOfMeasure: String = ""
)