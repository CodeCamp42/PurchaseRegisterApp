package com.example.purchaseregister.view.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.viewmodel.shared.FacturaRepository
import com.example.purchaseregister.viewmodel.shared.ScrapingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.api.responses.*

class PurchaseViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService
    val facturasCompras: StateFlow<List<Invoice>> = FacturaRepository.facturasCompras
    val facturasVentas: StateFlow<List<Invoice>> = FacturaRepository.facturasVentas
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun cargarFacturasDesdeBD(esCompra: Boolean = true) {
        viewModelScope.launch {
            try {
                println("🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴")
                println("📥 INICIANDO CARGA DESDE BD")
                println("🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴")

                val response = apiService.obtenerFacturasUsuarioCompleto("1")

                println("📦 RESPUESTA BD RECIBIDA:")
                println("   - success: ${response.success}")
                println("   - facturas size: ${response.facturas?.size ?: 0}")

                if (response.facturas != null) {
                    response.facturas.forEachIndexed { index, f ->
                        println("   BD[$index]: ${f.serie}-${f.numero} (ID: ${f.idFactura})")
                    }
                }

                if (response.success) {
                    println("✅ Facturas cargadas desde BD: ${response.facturas.size}")

                    val facturasBD = response.facturas.map { facturaResponse ->
                        println("🔄 Mapeando factura BD: ${facturaResponse.serie}-${facturaResponse.numero}")
                        Invoice(
                            id = facturaResponse.idFactura,
                            ruc = facturaResponse.proveedorRuc ?: "",
                            razonSocial = facturaResponse.proveedor?.razonSocial ?: "",
                            serie = facturaResponse.serie,
                            numero = facturaResponse.numero,
                            fechaEmision = facturaResponse.fechaEmision,
                            tipoDocumento = "FACTURA",
                            moneda = facturaResponse.moneda,
                            costoTotal = facturaResponse.costoTotal,
                            igv = facturaResponse.igv,
                            importeTotal = facturaResponse.importeTotal,
                            estado = facturaResponse.estado,
                            isSelected = false,
                            productos = facturaResponse.detalles?.map { detalle ->
                                ProductItem(
                                    descripcion = detalle.descripcion,
                                    cantidad = detalle.cantidad,
                                    costoUnitario = detalle.costoUnitario,
                                    unidadMedida = detalle.unidadMedida
                                )
                            } ?: emptyList(),
                            anio = facturaResponse.fechaEmision.take(4),
                            tipoCambio = ""
                        )
                    }

                    // Obtener facturas actuales (de API) ANTES de combinar
                    val facturasActuales = if (esCompra) {
                        FacturaRepository.getFacturasCompras()
                    } else {
                        FacturaRepository.getFacturasVentas()
                    }

                    println("📊 Facturas de API actuales: ${facturasActuales.size}")
                    facturasActuales.forEachIndexed { index, f ->
                        println("   API[$index]: ${f.serie}-${f.numero} (ID: ${f.id})")
                    }

                    // COMBINAR: facturas de API + facturas de BD
                    val facturasCombinadas = combinarFacturas(
                        facturasDeAPI = facturasActuales,
                        facturasLocales = facturasBD
                    )

                    println("📊 Facturas combinadas (API + BD): ${facturasCombinadas.size}")
                    facturasCombinadas.forEachIndexed { index, f ->
                        println("   COMBINADA[$index]: ${f.serie}-${f.numero} (ID: ${f.id})")
                    }

                    // ✅ PASO CLAVE: Guardar las combinadas en el StateFlow principal
                    if (esCompra) {
                        FacturaRepository.setFacturasCompras(facturasCombinadas)
                        println("💾 Guardadas en facturasCompras: ${facturasCombinadas.size}")
                    } else {
                        FacturaRepository.setFacturasVentas(facturasCombinadas)
                        println("💾 Guardadas en facturasVentas: ${facturasCombinadas.size}")
                    }

                    // ✅ PASO CLAVE #2: Actualizar el CACHÉ que usa cargarFacturasDesdeAPI()
                    val cacheKey = FacturaRepository.getCacheKey(esCompra, "ultimo_periodo")
                    FacturaRepository.updateCache(cacheKey, facturasCombinadas)
                    println("💾 Caché actualizado con ${facturasCombinadas.size} facturas (incluyendo BD)")

                    println("✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅")
                    println("🎉 CARGA DESDE BD COMPLETADA EXITOSAMENTE")
                    println("✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅")
                } else {
                    println("❌❌❌ response.success es false")
                }
            } catch (e: Exception) {
                println("💥💥💥 ERROR EN CARGA DESDE BD: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun cargarFacturasDesdeAPI(
        periodoInicio: String,
        periodoFin: String,
        esCompra: Boolean = true,
        ruc: String,
        usuario: String,
        claveSol: String
    ) {
        viewModelScope.launch {
            val cacheKey = FacturaRepository.getCacheKey(esCompra, periodoInicio)
            val facturasEnCache = FacturaRepository.getCachedFacturas(cacheKey)

            _errorMessage.value = null

            // SI HAY CACHE, usarlo directamente
            if (facturasEnCache != null) {
                println("📦 Usando cache para periodo: $periodoInicio")

                if (esCompra) {
                    FacturaRepository.setFacturasCompras(facturasEnCache)
                } else {
                    FacturaRepository.setFacturasVentas(facturasEnCache)
                }
                return@launch
            }

            // SI NO HAY CACHE, consultar a SUNAT
            _isLoading.value = true

            try {
                val response = apiService.obtenerFacturas(
                    periodoInicio,
                    periodoFin,
                    ruc,
                    usuario,
                    claveSol
                )

                if (response.success) {
                    val facturasAPI = parsearContenidoSunat(response.resultados, esCompra)

                    println("🌐 Facturas obtenidas de SUNAT: ${facturasAPI.size}")

                    // Guardar en cache
                    FacturaRepository.updateCache(cacheKey, facturasAPI)

                    // SOLO guardar facturas de API (sin combinar aún)
                    if (esCompra) {
                        FacturaRepository.setFacturasCompras(facturasAPI)
                    } else {
                        FacturaRepository.setFacturasVentas(facturasAPI)
                    }
                } else {
                    _errorMessage.value = "Error en la respuesta del servidor"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con el servidor: ${e.message}"
                println("❌ Error en API: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Combina facturas de API con facturas locales SIN DUPLICADOS
    private fun combinarFacturas(
        facturasDeAPI: List<Invoice>,
        facturasLocales: List<Invoice>
    ): List<Invoice> {
        val resultado = mutableMapOf<String, Invoice>()

        println("🔄 Combinando facturas:")
        println("   - API: ${facturasDeAPI.size}")
        println("   - Locales: ${facturasLocales.size}")

        // 1. Primero agregar TODAS las facturas locales (prioridad máxima)
        facturasLocales.forEach { facturaLocal ->
            val clave = "${facturaLocal.serie}-${facturaLocal.numero}"
            resultado[clave] = facturaLocal
            println("   ✅ Local: ${clave} (ID: ${facturaLocal.id}, Estado: ${facturaLocal.estado})")
        }

        // 2. Agregar facturas de API que NO existen localmente
        var agregadas = 0
        facturasDeAPI.forEach { facturaAPI ->
            val clave = "${facturaAPI.serie}-${facturaAPI.numero}"
            if (!resultado.containsKey(clave)) {
                resultado[clave] = facturaAPI
                agregadas++
                println("   ➕ API nueva: ${clave}")
            } else {
                println("   🔄 API duplicada: ${clave} (preservando local)")
            }
        }

        println("📊 Resultado final: ${resultado.size} facturas (${facturasLocales.size} locales + ${agregadas} nuevas de API)")

        return resultado.values.sortedBy { factura ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    private suspend fun parsearContenidoSunat(
        resultados: List<SunatResultado>,
        esCompra: Boolean
    ): List<Invoice> {
        val facturas = mutableListOf<Invoice>()
        var idCounter = 1

        val facturasExistentesCompras = FacturaRepository.getFacturasCompras()
        val facturasExistentesVentas = FacturaRepository.getFacturasVentas()
        val todasFacturasExistentes = facturasExistentesCompras + facturasExistentesVentas

        val maxIdActual = todasFacturasExistentes.maxOfOrNull { it.id } ?: 0
        idCounter = maxIdActual + 1

        val facturasParaRegistrarEnBD = mutableListOf<RegistrarFacturaDesdeSunatRequest>()

        resultados.forEach { resultado ->
            resultado.contenido.forEach { item ->
                val numeroComprobante = "${item.serie}-${item.numero}"
                var estadoDesdeBD = "CONSULTADO"
                var productosDesdeBD: List<ProductItem> = emptyList()
                var idExistente: Int? = null

                try {
                    val facturaUI = apiService.obtenerFacturaParaUI(numeroComprobante)
                    estadoDesdeBD = facturaUI.factura.estado
                    idExistente = facturaUI.factura.idFactura

                    if (facturaUI.factura.detalles != null) {
                        productosDesdeBD = facturaUI.factura.detalles.map { detalle ->
                            ProductItem(
                                descripcion = detalle.descripcion,
                                cantidad = detalle.cantidad,
                                costoUnitario = detalle.costoUnitario,
                                unidadMedida = detalle.unidadMedida
                            )
                        }
                    }
                } catch (e: Exception) {
                    estadoDesdeBD = "CONSULTADO"

                    val razonSocialRespectiva = if (esCompra) {
                        item.razonSocialEmisor
                    } else {
                        item.nombreReceptor
                    }

                    val facturaRequest = RegistrarFacturaDesdeSunatRequest(
                        rucEmisor = item.rucEmisor,
                        serie = item.serie,
                        numero = item.numero,
                        fechaEmision = item.fechaEmision,
                        razonSocial = razonSocialRespectiva,
                        tipoDocumento = when (item.tipoCP) {
                            "01" -> "FACTURA"
                            "03" -> "BOLETA"
                            else -> "DOCUMENTO"
                        },
                        moneda = when (item.moneda) {
                            "PEN" -> "Soles (PEN)"
                            "USD" -> "Dólares (USD)"
                            else -> item.moneda
                        },
                        costoTotal = String.format("%.2f", item.baseGravada),
                        igv = String.format("%.2f", item.igv),
                        importeTotal = String.format("%.2f", item.total),
                        usuarioId = 1
                    )
                    facturasParaRegistrarEnBD.add(facturaRequest)
                }

                val facturaExistente = todasFacturasExistentes.firstOrNull { factura ->
                    factura.ruc == item.nroDocReceptor &&
                            factura.serie == item.serie &&
                            factura.numero == item.numero
                }

                val id = idExistente ?: (facturaExistente?.id ?: idCounter++)

                val tipoCambio = when {
                    item.tipodecambio != null -> {
                        if (item.moneda == "PEN" && item.tipodecambio == 1.0) {
                            ""
                        } else {
                            String.format("%.2f", item.tipodecambio)
                        }
                    }
                    else -> facturaExistente?.tipoCambio ?: ""
                }

                FacturaRepository.setRucEmisor(id, item.nroDocReceptor)

                val razonSocialCompra = item.nombreReceptor

                val factura = Invoice(
                    id = id,
                    ruc = item.nroDocReceptor,
                    serie = item.serie,
                    numero = item.numero,
                    fechaEmision = item.fechaEmision,
                    razonSocial = razonSocialCompra,
                    tipoDocumento = when (item.tipoCP) {
                        "01" -> "FACTURA"
                        "03" -> "BOLETA"
                        else -> "DOCUMENTO"
                    },
                    moneda = when (item.moneda) {
                        "PEN" -> "Soles (PEN)"
                        "USD" -> "Dólares (USD)"
                        else -> item.moneda
                    },
                    costoTotal = String.format("%.2f", item.baseGravada),
                    igv = String.format("%.2f", item.igv),
                    importeTotal = String.format("%.2f", item.total),
                    estado = facturaExistente?.estado ?: estadoDesdeBD,
                    isSelected = facturaExistente?.isSelected ?: false,
                    productos = facturaExistente?.productos ?: productosDesdeBD,
                    anio = facturaExistente?.anio ?: item.periodo.take(4),
                    tipoCambio = tipoCambio
                )
                facturas.add(factura)
            }
        }

        if (facturasParaRegistrarEnBD.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    facturasParaRegistrarEnBD.forEach { request ->
                        try {
                            apiService.registrarFacturaDesdeSunat(request)
                        } catch (e: Exception) {
                            _errorMessage.value = "Error al registrar algunas facturas: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error al registrar facturas en BD: ${e.message}"
                }
            }
        }

        return facturas.sortedBy { factura ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    fun getRucEmisor(facturaId: Int): String? = FacturaRepository.getRucEmisor(facturaId)

    fun cargarDetalleFacturaXmlConUsuario(
        facturaId: Int,
        esCompra: Boolean,
        rucEmisor: String,
        context: Context,
        onLoadingComplete: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        val miRuc = SunatPrefs.getRuc(context)
        val usuario = SunatPrefs.getUser(context)
        val claveSol = SunatPrefs.getClaveSol(context)

        if (miRuc == null || usuario == null || claveSol == null) {
            _errorMessage.value = "Complete sus credenciales SUNAT primero"
            onLoadingComplete(false, "Complete sus credenciales SUNAT primero")
            return
        }

        val factura = if (esCompra) {
            FacturaRepository.getFacturasCompras().firstOrNull { it.id == facturaId }
        } else {
            FacturaRepository.getFacturasVentas().firstOrNull { it.id == facturaId }
        }

        if (factura == null) {
            _errorMessage.value = "Factura no encontrada"
            onLoadingComplete(false, "Factura no encontrada")
            return
        }

        if (factura.estado == "CON DETALLE" || factura.estado == "REGISTRADO") {
            if (factura.productos.isNotEmpty()) {
                onLoadingComplete(true, "Detalles ya cargados")
            } else {
                onLoadingComplete(false, "No hay detalles disponibles")
            }
            return
        }

        if (factura.estado == "EN PROCESO") {
            onLoadingComplete(false, "Ya se está procesando esta factura")
            return
        }

        actualizarEstadoFactura(facturaId, "EN PROCESO", esCompra)

        ScrapingManager.iniciarScrapingConCola(
            viewModelScope = viewModelScope,
            facturaId = facturaId,
            esCompra = esCompra,
            rucEmisor = rucEmisor,
            factura = factura,
            context = context,
            onEstadoActualizado = { id, estado ->
                actualizarEstadoFactura(id, estado, esCompra)
            },
            onProductosActualizados = { id, productos, esCompra ->
                actualizarProductosFactura(id, productos, esCompra)
            },
            onError = { error ->
                _errorMessage.value = error
                onLoadingComplete(false, error)
            },
            onJobEncolado = { jobId ->
                onLoadingComplete(true, "Scraping encolado. Job ID: $jobId")
            }
        )
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

    private fun actualizarProductosFactura(
        facturaId: Int,
        productos: List<ProductItem>,
        esCompra: Boolean
    ) {
        viewModelScope.launch {
            if (esCompra) {
                FacturaRepository.updateFacturasCompras { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                FacturaRepository.updateFacturasVentas { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }

    fun limpiarError() {
        _errorMessage.value = null
    }

    fun limpiarFacturas() {
        viewModelScope.launch {
            FacturaRepository.clearAll()
        }
    }

    suspend fun validarCredencialesSUNAT(
        ruc: String,
        usuario: String,
        claveSol: String
    ): Boolean {
        return try {
            println("🌐 [VALIDACIÓN] Enviando petición a /sunat/validar-credenciales")
            println("📦 [VALIDACIÓN] Datos: ruc=$ruc, usuario=$usuario, claveSol=****")

            val response = apiService.validarCredenciales(
                ValidarCredencialesRequest(
                    ruc = ruc,
                    usuario = usuario,
                    claveSol = claveSol
                )
            )

            println("✅ [VALIDACIÓN] Respuesta recibida:")
            println("   - valido: ${response.valido}")
            println("   - mensaje: ${response.mensaje}")
            println("   - token: ${response.token}")

            response.valido
        } catch (e: Exception) {
            println("❌ [VALIDACIÓN] Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}