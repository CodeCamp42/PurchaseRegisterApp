package com.example.purchaseregister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.viewmodel.shared.FacturaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*

class InvoiceViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    val facturasCompras: StateFlow<List<Invoice>> = FacturaRepository.facturasCompras
    val facturasVentas: StateFlow<List<Invoice>> = FacturaRepository.facturasVentas

    private val _isLoading = MutableStateFlow(false)

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _registroCompletado = MutableStateFlow(false)

    fun registrarFacturasEnBaseDeDatos(
        facturas: List<Invoice>,
        esCompra: Boolean,
        context: Context,
        mostrarLoading: Boolean = true
    ) {
        viewModelScope.launch {
            if (mostrarLoading) {
                _isLoading.value = true
            }
            _errorMessage.value = null
            _registroCompletado.value = false

            try {
                val facturasParaRegistrar = facturas.map { factura ->
                    FacturaParaRegistrar(
                        id = factura.id,
                        rucEmisor = factura.ruc,
                        serie = factura.serie,
                        numero = factura.numero,
                        fechaEmision = factura.fechaEmision,
                        razonSocial = factura.razonSocial,
                        tipoDocumento = factura.tipoDocumento,
                        moneda = factura.moneda,
                        costoTotal = factura.costoTotal,
                        igv = factura.igv,
                        importeTotal = factura.importeTotal,
                        productos = factura.productos.map { producto ->
                            ProductoParaRegistrar(
                                descripcion = producto.descripcion,
                                cantidad = producto.cantidad,
                                costoUnitario = producto.costoUnitario,
                                unidadMedida = producto.unidadMedida
                            )
                        }
                    )
                }

                val request = RegistroFacturasRequest(facturas = facturasParaRegistrar)
                val response = apiService.registrarFacturasEnBD(request)

                val todosExitosos = response.resultados.all { it.success }

                if (todosExitosos) {
                    facturas.forEach { factura ->
                        actualizarEstadoFactura(factura.id, "REGISTRADO", esCompra)
                    }
                    _registroCompletado.value = true
                } else {
                    val errores = response.resultados.filter { !it.success }
                    val errorMsg = "Algunas facturas no se pudieron registrar: ${errores.map { it.numeroComprobante }}"
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = "Error de conexión al registrar en BD: ${e.message}"
                _errorMessage.value = errorMsg
            } finally {
                if (mostrarLoading) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun actualizarEstadoFactura(facturaId: Int, nuevoEstado: String, esCompra: Boolean) {
        viewModelScope.launch {
            if (esCompra) {
                FacturaRepository.updateFacturasCompras { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            FacturaRepository.updateFacturaInAllCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                FacturaRepository.updateFacturasVentas { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            FacturaRepository.updateFacturaInAllCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }
}