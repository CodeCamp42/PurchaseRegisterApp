package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.purchaseregister.view.register.DetailScreen
import kotlinx.serialization.Serializable

// 1. Ahora es una data class con los campos que quieres mostrar
@Serializable
data class DetailRoute(
    val rucProveedor: String,
    val serie: String,
    val numero: String,
    val fecha: String,
    val razonSocial: String,
    val tipoDocumento: String,
    val anio: String,
    val moneda: String,
    val costoTotal: String,
    val igv: String,
    val tipoCambio: String,
    val importeTotal: String
)

// 2. Extensión actualizada
fun NavGraphBuilder.detailPurchaseRoute(
    onBack: () -> Unit
) {
    composable<DetailRoute> { backStackEntry ->
        // Extraemos los argumentos de la ruta
        val args = backStackEntry.toRoute<DetailRoute>()

        // Se los pasamos a la pantalla de detalle
        DetailScreen(
            onBack = onBack,
            rucProveedor = args.rucProveedor,
            serie = args.serie,
            numero = args.numero,
            fecha = args.fecha,
            razonSocial = args.razonSocial,
            tipoDocumento = args.tipoDocumento,
            anio = args.anio,
            moneda = args.moneda,
            costoTotal = args.costoTotal,
            igv = args.igv,
            tipoCambio = args.tipoCambio,
            importeTotal = args.importeTotal
        )
    }
}