package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.register.RegisterPurchaseScreen
import com.example.purchaseregister.view.register.PurchaseRegistrationViewModel
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