package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.purchaseregister.view.detail.DetailRoute
import com.example.purchaseregister.view.detail.DetailViewModel
import com.example.purchaseregister.view.purchase.PurchaseDetailScreen
import com.example.purchaseregister.view.register.PurchaseRegistrationViewModel
import com.example.purchaseregister.view.register.RegisterPurchaseScreen
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import com.example.purchaseregister.view.detail.DetailScreen
import com.example.purchaseregister.view.purchase.PurchaseDetailRoute
import com.example.purchaseregister.view.register.RegisterRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val invoiceListViewModel: InvoiceListViewModel = viewModel()
    val purchaseRegistrationViewModel: PurchaseRegistrationViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute
    ) {
        // Ruta principal de la lista de facturas
        composable<PurchaseDetailRoute> {
            PurchaseDetailScreen(
                viewModel = invoiceListViewModel,
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
                onNavigateToDetail = { detailRoute ->
                    navController.navigate(detailRoute)
                }
            )
        }

        // Ruta de registro de factura
        composable<RegisterRoute> {
            RegisterPurchaseScreen(
                onBack = {
                    navController.popBackStack()
                },
                viewModel = purchaseRegistrationViewModel
            )
        }

        // Ruta de detalle de factura
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()
            val detailViewModel: DetailViewModel = viewModel()

            // Obtener la factura de los flows del ViewModel de lista
            val purchaseInvoices by invoiceListViewModel.purchaseInvoices.collectAsState()
            val salesInvoices by invoiceListViewModel.salesInvoices.collectAsState()

            val invoice = if (args.isPurchase) {
                purchaseInvoices.firstOrNull { it.id == args.id }
            } else {
                salesInvoices.firstOrNull { it.id == args.id }
            }

            if (invoice != null) {
                DetailScreen(
                    invoiceId = args.id,
                    isPurchase = args.isPurchase,
                    onBack = { navController.popBackStack() },
                    viewModel = detailViewModel
                )
            } else {
                // Manejar caso de factura no encontrada
                Text("Factura no encontrada")
            }
        }
    }
}