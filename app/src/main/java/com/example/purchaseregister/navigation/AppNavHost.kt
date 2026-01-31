package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.purchaseregister.view.register.DetailScreen
import com.example.purchaseregister.viewmodel.InvoiceViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val viewModel: InvoiceViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute // Punto de entrada
    ) {
        // 1. Pantalla Principal
        purchaseDetailRoute(
            viewModel = viewModel,
            onNavigateToRegistrar = {
                navController.navigate(RegisterRoute)
            },
            onNavigateToDetalle = { routeData ->
                navController.navigate(routeData)
            }
        )

        // 2. Pantalla de Registro
        registerPurchaseRoute(
            onBack = {
                navController.popBackStack() // Regresa a la pantalla anterior
            },
            viewModel = viewModel
        )

        // 3. Pantalla de Detalle de Factura
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            println("🎯 [AppNavHost] Recibiendo DetailRoute:")
            println("🎯 [AppNavHost] - ID: ${args.id}")
            println("🎯 [AppNavHost] - esCompra: ${args.esCompra}")

            val facturasCompras = viewModel.facturasCompras.collectAsState()
            val facturasVentas = viewModel.facturasVentas.collectAsState()

            // Buscar la factura completa en el ViewModel usando el ID
            val factura = if (args.esCompra) {
                facturasCompras.value.firstOrNull { it.id == args.id }
            } else {
                facturasVentas.value.firstOrNull { it.id == args.id }
            }

            LaunchedEffect(args.id, args.esCompra) {
                println("🔄 [AppNavHost] Actualizando estado al ENTRAR al detalle")
                viewModel.actualizarEstadoFactura(args.id, "CON DETALLE", args.esCompra)
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
                    productos = factura.productos, // ← PASAR LISTA REAL DE PRODUCTOS
                    onAceptar = {
                        println("✅ [AppNavHost] Botón ACEPTAR presionado")
                    }
                )
            } else {
                // Mostrar mensaje de error
                Text("Factura no encontrada")
            }
        }
    }
}