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

    // ✅ ViewModels específicos para cada pantalla
    val purchaseViewModel: PurchaseViewModel = viewModel()
    val invoiceViewModel: InvoiceViewModel = viewModel()
    val purchaseRegistrationViewModel: PurchaseRegistrationViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute
    ) {
        // 1. Pantalla Principal - AHORA USA AMBOS VIEWMODELS
        purchaseDetailRoute(
            purchaseViewModel = purchaseViewModel,
            invoiceViewModel = invoiceViewModel,
            onNavigateToRegistrar = {
                navController.navigate(RegisterRoute)
            },
            onNavigateToDetalle = { routeData ->
                navController.navigate(routeData)
            }
        )

        // 2. Pantalla de Registro - USA SU PROPIO VIEWMODEL
        registerPurchaseRoute(
            onBack = {
                navController.popBackStack()
            },
            viewModel = purchaseRegistrationViewModel
        )

        // 3. Pantalla de Detalle de Factura - USA INVOICEVIEWMODEL PARA DATOS
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            println("🎯 [AppNavHost] Recibiendo DetailRoute: ID=${args.id}, esCompra=${args.esCompra}")

            // ✅ OBSERVAR REACTIVAMENTE LOS CAMBIOS DESDE InvoiceViewModel
            val facturasCompras by invoiceViewModel.facturasCompras.collectAsState()
            val facturasVentas by invoiceViewModel.facturasVentas.collectAsState()

            // ✅ BUSCAR LA FACTURA REACTIVAMENTE (se actualiza cuando cambia el StateFlow)
            val factura = remember(facturasCompras, facturasVentas, args.id, args.esCompra) {
                if (args.esCompra) {
                    facturasCompras.firstOrNull { it.id == args.id }
                } else {
                    facturasVentas.firstOrNull { it.id == args.id }
                }
            }

            if (factura != null) {
                DetailScreen(
                    id = factura.id,
                    onBack = {
                        println("🔙 [AppNavHost] Navegando BACK desde DetailScreen")
                        navController.popBackStack()
                    },
                    rucProveedor = factura.ruc,
                    serie = factura.serie,
                    numero = factura.numero,
                    fecha = factura.fechaEmision,
                    razonSocial = factura.razonSocial,
                    tipoDocumento = factura.tipoDocumento,
                    anio = factura.anio,
                    moneda = factura.moneda,
                    costoTotal = factura.costoTotal,
                    igv = factura.igv,
                    tipoCambio = factura.tipoCambio,
                    importeTotal = factura.importeTotal,
                    esCompra = args.esCompra,
                    productos = factura.productos,
                    onAceptar = {
                        println("✅ [AppNavHost] Botón ACEPTAR presionado")
                    }
                )
            } else {
                Text("Factura no encontrada")
            }
        }
    }
}