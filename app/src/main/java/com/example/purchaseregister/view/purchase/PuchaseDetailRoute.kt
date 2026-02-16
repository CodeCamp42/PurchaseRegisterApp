package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.purchase.PurchaseDetailScreen
import com.example.purchaseregister.view.purchase.PurchaseViewModel
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.serialization.Serializable

@Serializable
object PurchaseDetailRoute

fun NavGraphBuilder.purchaseDetailRoute(
    purchaseViewModel: PurchaseViewModel,
    invoiceViewModel: InvoiceViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (DetailRoute) -> Unit,
    onPurchasesClick: () -> Unit = {},
    onSalesClick: () -> Unit = {}
) {
    composable<PurchaseDetailRoute> {
        PurchaseDetailScreen(
            purchaseViewModel = purchaseViewModel,
            invoiceViewModel = invoiceViewModel,
            onPurchasesClick = onPurchasesClick,
            onSalesClick = onSalesClick,
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToDetail = onNavigateToDetail
        )
    }
}