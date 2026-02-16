package com.example.purchaseregister.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductItem(
    val description: String,
    val unitCost: String,
    val quantity: String,
    val unitOfMeasure: String = ""
)