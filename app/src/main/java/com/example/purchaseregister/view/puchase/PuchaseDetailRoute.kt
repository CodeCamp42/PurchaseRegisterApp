package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.puchase.PurchaseDetailScreen
import kotlinx.serialization.Serializable

// 1. Definimos la ruta como un object porque no pasamos parámetros (como IDs) aún.
@Serializable
object PurchaseDetailRoute

// 2. Función de extensión para configurar la pantalla en el mapa de navegación.
fun NavGraphBuilder.purchaseDetailRoute(
    onNavigateToRegistrar: () -> Unit,
    onNavigateToDetalle: (DetailRoute) -> Unit,
    onComprasClick: () -> Unit = {},
    onVentasClick: () -> Unit = {}
) {
    composable<PurchaseDetailRoute> {
        PurchaseDetailScreen(
            onComprasClick = onComprasClick,
            onVentasClick = onVentasClick,
            onNavigateToRegistrar = onNavigateToRegistrar,
            onNavigateToDetalle = onNavigateToDetalle
        )
    }
}