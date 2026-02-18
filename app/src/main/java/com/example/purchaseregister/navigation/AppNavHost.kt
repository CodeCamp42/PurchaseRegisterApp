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
import com.example.purchaseregister.view.detail.DetailViewModel
import com.example.purchaseregister.view.purchase.PurchaseDetailScreen
import com.example.purchaseregister.view.register.PurchaseRegistrationViewModel
import com.example.purchaseregister.view.register.RegisterPurchaseScreen
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import com.example.purchaseregister.view.register.DetailScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // Instanciar ViewModels a nivel de NavHost para que vivan mientras el host exista
    val invoiceListViewModel: InvoiceListViewModel = viewModel()
    val purchaseRegistrationViewModel: PurchaseRegistrationViewModel = viewModel()
    // DetailViewModel se instancia en la pantalla de detalle porque es específico de esa pantalla

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

        // Ruta de detalle de factura, recibe parámetros de la ruta
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()
            val detailViewModel: DetailViewModel = viewModel() // ViewModel específico de la pantalla

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
                    id = invoice.id,
                    onBack = { navController.popBackStack() },
                    providerRuc = invoice.ruc,
                    series = invoice.series,
                    number = invoice.number,
                    date = invoice.issueDate,
                    businessName = invoice.businessName,
                    documentType = invoice.documentType,
                    year = invoice.year,
                    currency = invoice.currency,
                    totalCost = invoice.totalCost,
                    igv = invoice.igv,
                    exchangeRate = invoice.exchangeRate,
                    totalAmount = invoice.totalAmount,
                    isPurchase = args.isPurchase,
                    products = invoice.products,
                    viewModel = detailViewModel
                )
            } else {
                // Manejar caso de factura no encontrada
                Text("Factura no encontrada")
            }
        }
    }
}