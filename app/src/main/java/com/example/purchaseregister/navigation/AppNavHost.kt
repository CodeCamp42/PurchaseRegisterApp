package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.purchaseregister.view.register.DetailScreen
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import com.example.purchaseregister.utils.SunatPrefs
import java.util.Base64

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val viewModel: InvoiceViewModel = viewModel()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute
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
                navController.popBackStack()
            },
            viewModel = viewModel
        )

        // 3. Pantalla de Detalle de Factura
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            println("🎯 [AppNavHost] Recibiendo DetailRoute: ID=${args.id}, esCompra=${args.esCompra}")

            // ✅ OBSERVAR REACTIVAMENTE LOS CAMBIOS
            val facturasCompras by viewModel.facturasCompras.collectAsState()
            val facturasVentas by viewModel.facturasVentas.collectAsState()

            // ✅ BUSCAR LA FACTURA REACTIVAMENTE (se actualiza cuando cambia el StateFlow)
            val factura = remember(facturasCompras, facturasVentas, args.id, args.esCompra) {
                if (args.esCompra) {
                    facturasCompras.firstOrNull { it.id == args.id }
                } else {
                    facturasVentas.firstOrNull { it.id == args.id }
                }
            }

            // Estado para el diálogo de clave SOL
            var showClaveSolDialog by remember { mutableStateOf(false) }
            var claveSolInput by remember { mutableStateOf("") }

            // CARGAR DETALLE XML SI NO TIENE PRODUCTOS Y TENEMOS CREDENCIALES
            LaunchedEffect(args.id, args.esCompra, factura?.productos?.isEmpty()) {
                if (factura != null && factura.productos.isEmpty()) {
                    val ruc = SunatPrefs.getRuc(context)
                    val usuario = SunatPrefs.getUser(context)
                    val claveSol = SunatPrefs.getClaveSol(context)

                    println("🔍 [AppNavHost] Credenciales: RUC=$ruc, Usuario=$usuario, ClaveSOL=${if (claveSol != null) "***" else "NULL"}")

                    if (ruc != null && usuario != null && claveSol != null) {
                        viewModel.cargarDetalleFacturaXmlConUsuario(
                            facturaId = args.id,
                            esCompra = args.esCompra,
                            rucEmisor = factura.ruc,
                            context = context
                        )
                    } else if (ruc != null && usuario != null) {
                        println("⚠️ [AppNavHost] Faltan credenciales completas")
                        showClaveSolDialog = true
                    } else {
                        println("❌ [AppNavHost] No hay credenciales SUNAT guardadas")
                    }
                }
            }

            // Diálogo para pedir clave SOL
            if (showClaveSolDialog) {
                AlertDialog(
                    onDismissRequest = { showClaveSolDialog = false },
                    title = { Text("Clave SOL requerida") },
                    text = {
                        Column {
                            Text("Para obtener los detalles del producto, necesitamos su Clave SOL.")
                            Text("Esta clave SOL se guardará de forma segura en su dispositivo.")
                            Text("No se enviará a ningún servidor externo.", color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = claveSolInput,
                                onValueChange = { claveSolInput = it },
                                label = { Text("Clave SOL") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (claveSolInput.isNotEmpty()) {
                                    // Guardar clave SOL (cifrada)
                                    SunatPrefs.saveClaveSol(context, claveSolInput)
                                    println("🔐 [AppNavHost] Clave SOL guardada")

                                    if (factura != null) {
                                        viewModel.cargarDetalleFacturaXmlConUsuario(
                                            facturaId = args.id,
                                            esCompra = args.esCompra,
                                            rucEmisor = factura.ruc,
                                            context = context
                                        )
                                    }

                                    showClaveSolDialog = false
                                    claveSolInput = ""
                                }
                            },
                            enabled = claveSolInput.isNotEmpty()
                        ) {
                            Text("Guardar y Continuar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showClaveSolDialog = false
                                claveSolInput = ""
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
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