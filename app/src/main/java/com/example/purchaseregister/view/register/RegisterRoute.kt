package com.example.purchaseregister.view.register

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object RegisterRoute

fun NavGraphBuilder.registerPurchaseRoute(
    onBack: () -> Unit,
    viewModel: PurchaseRegistrationViewModel
) {
    composable<RegisterRoute> {
        RegisterPurchaseScreen(
            onBack = onBack,
            viewModel = viewModel
        )
    }
}