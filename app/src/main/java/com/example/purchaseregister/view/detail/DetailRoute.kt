package com.example.purchaseregister.view.detail

import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(
    val id: Int,
    val isPurchase: Boolean = true
)