package com.example.purchaseregister.view.purchase

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.detail.DetailRoute
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import kotlinx.serialization.Serializable

@Serializable
object PurchaseDetailRoute

fun NavGraphBuilder.purchaseDetailRoute(
    viewModel: InvoiceListViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (DetailRoute) -> Unit
) {
    composable<PurchaseDetailRoute> {
        PurchaseDetailScreen(
            viewModel = viewModel,
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToDetail = onNavigateToDetail
        )
    }
}