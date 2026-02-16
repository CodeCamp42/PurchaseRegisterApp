package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.purchaseregister.view.register.DetailScreen
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import com.example.purchaseregister.view.purchase.PurchaseViewModel
import com.example.purchaseregister.view.register.PurchaseRegistrationViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val purchaseViewModel: PurchaseViewModel = viewModel()
    val invoiceViewModel: InvoiceViewModel = viewModel()
    val purchaseRegistrationViewModel: PurchaseRegistrationViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute
    ) {
        purchaseDetailRoute(
            purchaseViewModel = purchaseViewModel,
            invoiceViewModel = invoiceViewModel,
            onNavigateToRegister = {
                navController.navigate(RegisterRoute)
            },
            onNavigateToDetail = { routeData ->
                navController.navigate(routeData)
            }
        )

        registerPurchaseRoute(
            onBack = {
                navController.popBackStack()
            },
            viewModel = purchaseRegistrationViewModel
        )

        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            println("🎯 [AppNavHost] Recibiendo DetailRoute: ID=${args.id}, isPurchase=${args.isPurchase}")

            val purchaseInvoices by invoiceViewModel.purchaseInvoices.collectAsState()
            val salesInvoices by invoiceViewModel.salesInvoices.collectAsState()

            val invoice = remember(purchaseInvoices, salesInvoices, args.id, args.isPurchase) {
                if (args.isPurchase) {
                    purchaseInvoices.firstOrNull { it.id == args.id }
                } else {
                    salesInvoices.firstOrNull { it.id == args.id }
                }
            }

            if (invoice != null) {
                DetailScreen(
                    id = invoice.id,
                    onBack = {
                        println("🔙 [AppNavHost] Navegando BACK desde DetailScreen")
                        navController.popBackStack()
                    },
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
                    onAccept = {
                        println("✅ [AppNavHost] Botón ACEPTAR presionado")
                    }
                )
            } else {
                Text("Factura no encontrada")
            }
        }
    }
}