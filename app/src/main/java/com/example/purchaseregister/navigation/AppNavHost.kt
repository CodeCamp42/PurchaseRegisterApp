package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute // Punto de entrada
    ) {
        // 1. Usamos la extensión de PurchaseDetailRoute.kt
        purchaseDetailRoute(
            onNavigateToRegistrar = {
                navController.navigate(RegisterRoute)
            },
            onNavigateToDetalle = { routeData ->
                navController.navigate(routeData)
            }
        )

        // 2. Usamos la extensión de RegisterPurchaseRoute.kt
        registerPurchaseRoute(
            onBack = {
                navController.popBackStack() // Regresa a la pantalla anterior
            }
        )

        // 3. Pantalla de Detalle de Factura
        detailPurchaseRoute(
            onBack = {
                navController.popBackStack()
            }
        )
    }
}