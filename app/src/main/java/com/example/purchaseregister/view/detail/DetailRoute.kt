package com.example.purchaseregister.navigation

import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(
    val id: Int,
    val isPurchase: Boolean = true
)