package com.example.purchaseregister.viewmodel.shared

import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import com.example.purchaseregister.model.Invoice

object ScrapingManager {
    private val apiService = RetrofitClient.sunatApiService

    fun iniciarScrapingConCola(
        viewModelScope: CoroutineScope,
        facturaId: Int,
        esCompra: Boolean,
        rucEmisor: String,
        factura: Invoice,
        context: Context,
        onEstadoActualizado: (Int, String) -> Unit,
        onProductosActualizados: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit,
        onJobEncolado: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val miRuc = SunatPrefs.getRuc(context) ?: return@launch
                val usuario = SunatPrefs.getUser(context) ?: return@launch
                val claveSol = SunatPrefs.getClaveSol(context) ?: return@launch

                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisor,
                    serie = factura.serie,
                    numero = factura.numero,
                    ruc = if (esCompra) miRuc else factura.ruc,
                    usuario_sol = usuario,
                    clave_sol = claveSol
                )

                val encoladoResponse = apiService.descargarXmlConCola(request)

                if (encoladoResponse.success) {
                    val jobId = encoladoResponse.jobId
                    onJobEncolado(jobId)

                    iniciarPollingJob(
                        viewModelScope = viewModelScope,
                        jobId = jobId,
                        facturaId = facturaId,
                        esCompra = esCompra,
                        onEstadoActualizado = onEstadoActualizado,
                        onProductosActualizados = onProductosActualizados,
                        onError = onError
                    )
                } else {
                    onEstadoActualizado(facturaId, "CONSULTADO")
                    onError("Error al encolar trabajo: ${encoladoResponse.message}")
                }
            } catch (e: Exception) {
                onEstadoActualizado(facturaId, "CONSULTADO")
                onError("Error: ${e.message}")
            }
        }
    }

    private fun iniciarPollingJob(
        viewModelScope: CoroutineScope,
        jobId: String,
        facturaId: Int,
        esCompra: Boolean,
        onEstadoActualizado: (Int, String) -> Unit,
        onProductosActualizados: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            var intentos = 0
            val maxIntentos = 60

            while (intentos < maxIntentos) {
                delay(3000)

                try {
                    val estadoJob = apiService.obtenerEstadoJob(jobId)

                    when (estadoJob.state) {
                        "completed" -> {
                            val productos = estadoJob.result?.items?.map { item ->
                                ProductItem(
                                    descripcion = item.descripcion,
                                    cantidad = item.cantidad.toString(),
                                    costoUnitario = String.format("%.2f", item.valorUnitario),
                                    unidadMedida = item.unidad
                                )
                            } ?: emptyList()

                            onProductosActualizados(facturaId, productos, esCompra)
                            onEstadoActualizado(facturaId, "CON DETALLE")
                            guardarProductosEnBackend(estadoJob.result?.id ?: "sin-id", productos)
                            break
                        }
                        "failed" -> {
                            onEstadoActualizado(facturaId, "CONSULTADO")
                            onError("Scraping falló: ${estadoJob.reason}")
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Ignorar
                }

                intentos++
            }

            if (intentos >= maxIntentos) {
                onEstadoActualizado(facturaId, "CONSULTADO")
                onError("Timeout: El scraping no se completó")
            }
        }
    }

    private suspend fun guardarProductosEnBackend(
        numeroComprobante: String,
        productos: List<ProductItem>
    ) {
        try {
            val productosParaGuardar = productos.map { producto ->
                ProductoRequest(
                    descripcion = producto.descripcion,
                    cantidad = producto.cantidad.toDoubleOrNull() ?: 0.0,
                    costoUnitario = producto.costoUnitario.toDoubleOrNull() ?: 0.0,
                    unidadMedida = producto.unidadMedida
                )
            }

            apiService.guardarProductosFactura(
                numeroComprobante,
                GuardarProductosRequest(productos = productosParaGuardar)
            )

            val scrapingRequest = ScrapingCompletadoRequest(
                productos = productosParaGuardar
            )

            apiService.marcarScrapingCompletado(numeroComprobante, scrapingRequest)
        } catch (e: Exception) {
            // Silencioso
        }
    }
}