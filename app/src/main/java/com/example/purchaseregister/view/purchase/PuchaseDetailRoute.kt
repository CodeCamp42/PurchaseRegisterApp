package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.purchase.PurchaseDetailScreen
import com.example.purchaseregister.view.purchase.PurchaseViewModel
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.serialization.Serializable

// 1. Definimos la ruta como un object porque no pasamos parámetros (como IDs) aún.
@Serializable
object PurchaseDetailRoute

// 2. Función de extensión para configurar la pantalla en el mapa de navegación.
fun NavGraphBuilder.purchaseDetailRoute(
    purchaseViewModel: PurchaseViewModel,
    invoiceViewModel: InvoiceViewModel,
    onNavigateToRegistrar: () -> Unit,
    onNavigateToDetalle: (DetailRoute) -> Unit,
    onComprasClick: () -> Unit = {},
    onVentasClick: () -> Unit = {}
) {
    composable<PurchaseDetailRoute> {
        PurchaseDetailScreen(
            purchaseViewModel = purchaseViewModel,
            invoiceViewModel = invoiceViewModel,
            onComprasClick = onComprasClick,
            onVentasClick = onVentasClick,
            onNavigateToRegistrar = onNavigateToRegistrar,
            onNavigateToDetalle = onNavigateToDetalle
        )
    }
}