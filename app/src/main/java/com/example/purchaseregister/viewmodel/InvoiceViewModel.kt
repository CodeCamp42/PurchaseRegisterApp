package com.example.purchaseregister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.api.responses.*
import kotlinx.coroutines.delay
import com.example.purchaseregister.view.detail.DocumentItem
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import okhttp3.ResponseBody
import java.io.File

class InvoiceViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    private val _facturasCompras = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasCompras: StateFlow<List<Invoice>> = _facturasCompras.asStateFlow()

    private val _facturasVentas = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasVentas: StateFlow<List<Invoice>> = _facturasVentas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _registroCompletado = MutableStateFlow(false)
    val registroCompletado: StateFlow<Boolean> = _registroCompletado.asStateFlow()

    private val _rucEmisores = mutableMapOf<Int, String>()
    private val _facturasCache = mutableMapOf<String, List<Invoice>>()

    fun registrarFacturasEnBaseDeDatos(
        facturas: List<Invoice>,
        esCompra: Boolean,
        context: Context,
        mostrarLoading: Boolean = true  // ← Añade este parámetro
    ) {
        viewModelScope.launch {
            if (mostrarLoading) {  // ← Solo activar loading si se solicita
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
                val facturasRegistradas = response.resultados.count { it.success }

                if (todosExitosos) {
                    response.resultados.forEach { resultado ->
                        // Puedes dejar esto vacío o agregar logs si necesitas
                    }

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
                if (mostrarLoading) {  // ← Solo desactivar loading si se activó
                    _isLoading.value = false
                }
            }
        }
    }

    private fun getCacheKey(esCompra: Boolean, periodoInicio: String): String {
        return "${if (esCompra) "COMPRAS" else "VENTAS"}-${periodoInicio}"
    }

    fun cargarFacturasDesdeAPI(periodoInicio: String, periodoFin: String, esCompra: Boolean = true) {
        viewModelScope.launch {
            val cacheKey = getCacheKey(esCompra, periodoInicio)
            val facturasEnCache = _facturasCache[cacheKey]

            _errorMessage.value = null

            // ✅ 1. SI HAY CACHE: Solo actualizar estados SIN loading
            if (facturasEnCache != null) {
                try {
                    val facturasActualizadas = mutableListOf<Invoice>()

                    for (factura in facturasEnCache) {
                        try {
                            val numeroComprobante = "${factura.serie}-${factura.numero}"
                            val facturaUI = apiService.obtenerFacturaParaUI(numeroComprobante)

                            val estadoActual = facturaUI.factura.estado

                            val productosActuales = if (facturaUI.factura.detalles != null) {
                                facturaUI.factura.detalles.map { detalle ->
                                    ProductItem(
                                        descripcion = detalle.descripcion,
                                        cantidad = detalle.cantidad,
                                        costoUnitario = detalle.costoUnitario,
                                        unidadMedida = detalle.unidadMedida
                                    )
                                }
                            } else {
                                factura.productos
                            }

                            val facturaActualizada = factura.copy(
                                estado = estadoActual,
                                productos = productosActuales
                            )

                            facturasActualizadas.add(facturaActualizada)

                        } catch (e: Exception) {
                            // Si falla, mantener la factura original del cache
                            facturasActualizadas.add(factura)
                        }
                    }

                    // ✅ Actualizar el cache con los nuevos estados
                    _facturasCache[cacheKey] = facturasActualizadas

                    // ✅ Mostrar en UI
                    if (esCompra) {
                        _facturasCompras.value = facturasActualizadas
                    } else {
                        _facturasVentas.value = facturasActualizadas
                    }

                } catch (e: Exception) {
                    _errorMessage.value = "Error actualizando estados: ${e.message}"
                    // Si falla, mostrar cache original
                    if (esCompra) {
                        _facturasCompras.value = facturasEnCache
                    } else {
                        _facturasVentas.value = facturasEnCache
                    }
                }

                // ✅ IMPORTANTE: NO activamos isLoading ni lo desactivamos
                return@launch
            }

            // ✅ 2. NO HAY CACHE: Consultar SUNAT CON loading
            _isLoading.value = true

            try {
                val response = apiService.obtenerFacturas(periodoInicio, periodoFin)

                if (response.success) {
                    val facturas = parsearContenidoSunat(response.resultados, esCompra)

                    _facturasCache[cacheKey] = facturas

                    if (esCompra) {
                        _facturasCompras.value = facturas
                    } else {
                        _facturasVentas.value = facturas
                    }
                } else {
                    _errorMessage.value = "Error en la respuesta del servidor"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con el servidor: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarError() {
        _errorMessage.value = null
    }

    private suspend fun parsearContenidoSunat(
        resultados: List<SunatResultado>,
        esCompra: Boolean
    ): List<Invoice> {
        val facturas = mutableListOf<Invoice>()
        var idCounter = 1

        val facturasExistentesCompras = _facturasCompras.value
        val facturasExistentesVentas = _facturasVentas.value
        val todasFacturasExistentes = facturasExistentesCompras + facturasExistentesVentas

        val maxIdActual = todasFacturasExistentes.maxOfOrNull { it.id } ?: 0
        idCounter = maxIdActual + 1

        val facturasParaRegistrarEnBD = mutableListOf<RegistrarFacturaDesdeSunatRequest>()

        resultados.forEach { resultado ->
            resultado.contenido.forEach { item ->
                val numeroComprobante = "${item.serie}-${item.numero}"
                var estadoDesdeBD = "CONSULTADO"
                var productosDesdeBD: List<ProductItem> = emptyList()

                try {
                    val facturaUI = apiService.obtenerFacturaParaUI(numeroComprobante)
                    estadoDesdeBD = facturaUI.factura.estado

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
                        // Para COMPRAS: El proveedor es el emisor
                        item.razonSocialEmisor
                    } else {
                        // Para VENTAS: El cliente es el receptor
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

                val id = if (facturaExistente != null) {
                    facturaExistente.id
                } else {
                    idCounter++
                }

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

                _rucEmisores[id] = item.nroDocReceptor

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
                    estado = estadoDesdeBD,
                    isSelected = facturaExistente?.isSelected ?: false,
                    productos = productosDesdeBD,
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
                            val response = apiService.registrarFacturaDesdeSunat(request)
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

    private fun actualizarProductosFactura(
        facturaId: Int,
        productos: List<ProductItem>,
        esCompra: Boolean
    ) {
        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                _facturasVentas.update { lista ->
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

    fun getRucEmisor(facturaId: Int): String? = _rucEmisores[facturaId]

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
            _facturasCompras.value.firstOrNull { it.id == facturaId }
        } else {
            _facturasVentas.value.firstOrNull { it.id == facturaId }
        }

        if (factura == null) {
            _errorMessage.value = "Factura no encontrada"
            onLoadingComplete(false, "Factura no encontrada")
            return
        }

        val numeroComprobante = "${factura.serie}-${factura.numero}"

        // Si ya tiene detalle, notificar éxito inmediato
        if (factura.estado == "CON DETALLE" || factura.estado == "REGISTRADO") {
            if (factura.productos.isNotEmpty()) {
                onLoadingComplete(true, "Detalles ya cargados")
            } else {
                onLoadingComplete(false, "No hay detalles disponibles")
            }
            return
        }

        // Si está en proceso, notificar
        if (factura.estado == "EN PROCESO") {
            onLoadingComplete(false, "Ya se está procesando esta factura")
            return
        }

        // Usar el nuevo sistema de colas
        iniciarScrapingConCola(
            facturaId = facturaId,
            esCompra = esCompra,
            rucEmisor = rucEmisor,
            context = context
        ) { jobId ->
            // Llamar al callback con éxito
            onLoadingComplete(true, "Scraping encolado. Job ID: $jobId")
        }
    }

    fun agregarNuevaFacturaCompra(
        ruc: String,
        razonSocial: String,
        serie: String,
        numero: String,
        fechaEmision: String,
        tipoDocumento: String,
        moneda: String = "",
        costoTotal: String = "",
        igv: String = "",
        importeTotal: String = "",
        anio: String = "",
        tipoCambio: String = "",
        productos: List<ProductItem> = emptyList()
    ) {
        viewModelScope.launch {
            val estadoInicial = if (productos.isNotEmpty()) {
                "CON DETALLE"
            } else {
                "CONSULTADO"
            }

            _facturasCompras.update { lista ->
                val nuevoId = if (lista.isEmpty()) 1 else lista.maxOf { it.id } + 1

                val nuevaFactura = Invoice(
                    id = nuevoId,
                    ruc = ruc,
                    razonSocial = razonSocial,
                    serie = serie,
                    numero = numero,
                    fechaEmision = fechaEmision,
                    tipoDocumento = tipoDocumento,
                    anio = anio,
                    moneda = moneda,
                    costoTotal = costoTotal,
                    igv = igv,
                    tipoCambio = tipoCambio,
                    importeTotal = importeTotal,
                    estado = estadoInicial,
                    isSelected = false,
                    productos = productos
                )

                (lista + nuevaFactura).sortedBy { factura ->
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
            }
        }
    }

    fun iniciarScrapingConCola(
        facturaId: Int,
        esCompra: Boolean,
        rucEmisor: String,
        context: Context,
        onJobEncolado: (jobId: String) -> Unit = { _ -> }
    ) {
        viewModelScope.launch {
            try {
                // 1. Cambiar estado a "EN PROCESO"
                actualizarEstadoFactura(facturaId, "EN PROCESO", esCompra)

                // 2. Obtener credenciales
                val miRuc = SunatPrefs.getRuc(context) ?: return@launch
                val usuario = SunatPrefs.getUser(context) ?: return@launch
                val claveSol = SunatPrefs.getClaveSol(context) ?: return@launch

                // 3. Obtener factura para datos
                val factura = if (esCompra) {
                    _facturasCompras.value.firstOrNull { it.id == facturaId }
                } else {
                    _facturasVentas.value.firstOrNull { it.id == facturaId }
                } ?: return@launch

                // 4. Encolar trabajo (POST /descargar-xml)
                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisor,
                    serie = factura.serie,
                    numero = factura.numero,
                    ruc = if (esCompra) miRuc else factura.ruc, // Ajustar según sea compra/venta
                    usuario_sol = usuario,
                    clave_sol = claveSol
                )

                // NUEVO: Este endpoint ahora retorna jobId
                val encoladoResponse = apiService.descargarXmlConCola(request)

                if (encoladoResponse.success) {
                    val jobId = encoladoResponse.jobId
                    onJobEncolado(jobId)

                    // 5. Iniciar polling para verificar estado
                    iniciarPollingJob(jobId, facturaId, esCompra, context)
                } else {
                    // Si falla el encolado, volver a CONSULTADO
                    actualizarEstadoFactura(facturaId, "CONSULTADO", esCompra)
                    _errorMessage.value = "Error al encolar trabajo: ${encoladoResponse.message}"
                }

            } catch (e: Exception) {
                actualizarEstadoFactura(facturaId, "CONSULTADO", esCompra)
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    // NUEVO: Método para hacer polling del estado del job
    private fun iniciarPollingJob(
        jobId: String,
        facturaId: Int,
        esCompra: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            var intentos = 0
            val maxIntentos = 60 // 3 minutos si polling cada 3 segundos

            while (intentos < maxIntentos) {
                delay(3000) // Polling cada 3 segundos

                try {
                    val estadoJob = apiService.obtenerEstadoJob(jobId)

                    when (estadoJob.state) {
                        "completed" -> {
                            // Scraping completado exitosamente
                            val productos = estadoJob.result?.items?.map { item ->
                                ProductItem(
                                    descripcion = item.descripcion,
                                    cantidad = item.cantidad.toString(),
                                    costoUnitario = String.format("%.2f", item.valorUnitario),
                                    unidadMedida = item.unidad
                                )
                            } ?: emptyList()

                            // Actualizar productos y estado
                            actualizarProductosFactura(facturaId, productos, esCompra)
                            actualizarEstadoFactura(facturaId, "CON DETALLE", esCompra)

                            // Guardar en backend (opcional)
                            guardarProductosEnBackend(
                                estadoJob.result?.id ?: "sin-id",
                                productos
                            )
                            break
                        }
                        "failed" -> {
                            // Scraping falló
                            actualizarEstadoFactura(facturaId, "CONSULTADO", esCompra)
                            _errorMessage.value = "Scraping falló: ${estadoJob.reason}"
                            break
                        }
                        // "waiting", "active", "delayed" -> continuar polling
                    }

                } catch (e: Exception) {
                    // Error en polling, continuar intentando
                }

                intentos++
            }

            if (intentos >= maxIntentos) {
                // Timeout
                actualizarEstadoFactura(facturaId, "CONSULTADO", esCompra)
                _errorMessage.value = "Timeout: El scraping no se completó en el tiempo esperado"
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

            // También marcar scraping como completado
            val scrapingRequest = ScrapingCompletadoRequest(
                productos = productosParaGuardar
            )

            apiService.marcarScrapingCompletado(numeroComprobante, scrapingRequest)
        } catch (e: Exception) {
            // Silencioso, no es crítico
        }
    }

    fun actualizarEstadoFactura(facturaId: Int, nuevoEstado: String, esCompra: Boolean) {
        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            actualizarFacturaEnCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                _facturasVentas.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            actualizarFacturaEnCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }

    private fun actualizarFacturaEnCaches(facturaOriginal: Invoice, nuevoEstado: String) {
        _facturasCache.forEach { (key, facturasEnCache) ->
            val facturasActualizadas = facturasEnCache.map { facturaCache ->
                // Comparar por ruc, serie y número (no por id que puede cambiar)
                if (facturaCache.ruc == facturaOriginal.ruc &&
                    facturaCache.serie == facturaOriginal.serie &&
                    facturaCache.numero == facturaOriginal.numero) {
                    facturaCache.copy(estado = nuevoEstado)
                } else {
                    facturaCache
                }
            }
            _facturasCache[key] = facturasActualizadas
        }
    }

    // Método para descargar archivo
    fun descargarDocumento(
        context: Context,
        numeroComprobante: String,
        tipo: String
    ) {
        viewModelScope.launch {
            try {
                // Obtener la respuesta del API
                val responseBody = apiService.descargarArchivo(numeroComprobante, tipo)

                // Guardar el archivo
                guardarArchivo(context, responseBody, numeroComprobante, tipo)

            } catch (e: Exception) {
                _errorMessage.value = "Error al descargar: ${e.message}"
            }
        }
    }

    // Método alternativo usando DownloadManager (recomendado)
    fun descargarConDownloadManager(
        context: Context,
        numeroComprobante: String,
        tipo: String,
        baseUrl: String = "http://192.168.1.85:3043"
    ) {
        println("🔽 [descargarConDownloadManager] INICIANDO descarga")
        println("🔽 Número comprobante: $numeroComprobante")
        println("🔽 Tipo archivo: $tipo")
        println("🔽 Base URL: $baseUrl")

        // URL COMPLETA - MUESTRA ANTES DE CUALQUIER COSA
        val url = "$baseUrl/factura/descargar/$numeroComprobante/$tipo"
        println("🔗🔗🔗 URL COMPLETA PARA PROBAR EN NAVEGADOR: $url")
        println("⚠️ Abre esta URL en tu navegador para ver si el archivo existe")

        try {
            // Obtener DownloadManager
            println("📱 Obteniendo DownloadManager...")
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            println("✅ DownloadManager obtenido: $downloadManager")

            // Configurar extensión
            val extension = when (tipo.lowercase()) {
                "pdf" -> "pdf"
                "xml" -> "xml"
                "cdr" -> "zip"
                else -> {
                    println("⚠️ Tipo desconocido: $tipo, usando 'dat'")
                    "dat"
                }
            }
            println("📄 Extensión: $extension")

            // Construir nombre de archivo
            val fileName = "${numeroComprobante}_${tipo.uppercase()}.$extension"
            println("📁 Nombre archivo: $fileName")
            println("💾 Se guardará en: ${Environment.DIRECTORY_DOWNLOADS}/$fileName")

            // Verificar URL antes de crear la URI
            println("🔍 Verificando formato de URL...")
            println("🔍 URL tiene http/https: ${url.startsWith("http")}")

            // Crear URI
            println("🌐 Parseando URI...")
            val uri = Uri.parse(url)
            println("✅ URI parseada: $uri")
            println("✅ Esquema: ${uri.scheme}")
            println("✅ Host: ${uri.host}")
            println("✅ Puerto: ${uri.port}")
            println("✅ Path: ${uri.path}")

            // Crear request
            println("⚙️ Creando DownloadManager.Request...")
            val request = DownloadManager.Request(uri)
                .setTitle("Descargando $tipo: $numeroComprobante")
                .setDescription("Descargando archivo $tipo de la factura")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            println("✅ Request configurado")

            // Encolar descarga
            println("🚀 Encolando descarga con DownloadManager...")
            val downloadId = downloadManager.enqueue(request)

            println("✅✅✅ DESCARGA ENCOLADA EXITOSAMENTE")
            println("📋 ID de descarga: $downloadId")
            println("📱 La descarga se gestionará por el sistema Android")
            println("🔔 Se mostrará notificación cuando termine")

            // Mostrar mensaje en UI
            _errorMessage.value = "Descarga iniciada. ID: $downloadId"

            // Mensaje adicional en logs
            println("ℹ️ Si la descarga falla, verifica:")
            println("ℹ️ 1. Que la URL funcione en el navegador: $url")
            println("ℹ️ 2. Que el servidor esté accesible desde tu dispositivo")
            println("ℹ️ 3. Que el archivo exista en el servidor")

        } catch (e: SecurityException) {
            println("❌❌❌ ERROR DE PERMISOS")
            println("❌ Mensaje: ${e.message}")
            println("❌ Verifica permisos WRITE_EXTERNAL_STORAGE")
            e.printStackTrace()
            _errorMessage.value = "Error de permisos: ${e.message}"

        } catch (e: IllegalArgumentException) {
            println("❌❌❌ ERROR EN LA URL")
            println("❌ URL inválida: $url")
            println("❌ Mensaje: ${e.message}")
            e.printStackTrace()
            _errorMessage.value = "URL inválida: ${e.message}"

        } catch (e: NullPointerException) {
            println("❌❌❌ ERROR DE NULL")
            println("❌ Context o DownloadManager es nulo")
            println("❌ Mensaje: ${e.message}")
            e.printStackTrace()
            _errorMessage.value = "Error interno: ${e.message}"

        } catch (e: Exception) {
            println("❌❌❌ ERROR GENERAL")
            println("❌ Tipo: ${e::class.java.name}")
            println("❌ Mensaje: ${e.message}")
            println("❌ URL intentada: $url")
            e.printStackTrace()
            _errorMessage.value = "Error al descargar: ${e.message}"
        }

        println("🏁 [descargarConDownloadManager] FINALIZADO")
        println("==================================================================")
    }

    // Función privada para guardar archivo localmente
    private fun guardarArchivo(
        context: Context,
        responseBody: ResponseBody,
        numeroComprobante: String,
        tipo: String
    ) {
        try {
            val extension = when (tipo.lowercase()) {
                "pdf" -> "pdf"
                "xml" -> "xml"
                "cdr" -> "zip"
                else -> tipo
            }

            val fileName = "${numeroComprobante}_${tipo.uppercase()}.$extension"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            file.outputStream().use { output ->
                responseBody.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            // Mostrar notificación de éxito
            _errorMessage.value = "Archivo descargado: ${file.absolutePath}"

        } catch (e: Exception) {
            _errorMessage.value = "Error al guardar archivo: ${e.message}"
        }
    }
}