package com.example.purchaseregister.view.detail

import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(
    val id: String,
    val isPurchase: Boolean = true
)