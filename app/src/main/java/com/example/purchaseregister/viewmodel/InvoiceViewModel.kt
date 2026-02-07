package com.example.purchaseregister.viewmodel

import java.util.Locale
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.PUT
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import android.content.Context

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun obtenerFacturas(
        @Query("periodoInicio") periodoInicio: String,
        @Query("periodoFin") periodoFin: String
    ): SunatResponse

    @POST("sunat/descargar-xml")
    suspend fun obtenerDetalleFacturaXml(
        @Body request: DetalleFacturaRequest
    ): DetalleFacturaXmlResponse

    @PUT("factura/scraping-completado/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun marcarScrapingCompletado(
        @Path("numeroComprobante") numeroComprobante: String,
        @Body request: ScrapingCompletadoRequest? = null
    ): ScrapingCompletadoResponse

    @POST("factura/guardar-productos/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun guardarProductosFactura(
        @Path("numeroComprobante") numeroComprobante: String,
        @Body request: GuardarProductosRequest
    ): GuardarProductosResponse

    @POST("factura/procesarFactura")
    @Headers("Content-Type: application/json")
    suspend fun registrarFacturasEnBD(
        @Body request: RegistroFacturasRequest
    ): RegistroFacturasResponse

    @GET("factura/{numeroComprobante}")
    suspend fun verificarFacturaRegistrada(
        @Path("numeroComprobante") numeroComprobante: String
    ): FacturaRegistradaResponse

    @POST("factura/registrar-desde-sunat")
    @Headers("Content-Type: application/json")
    suspend fun registrarFacturaDesdeSunat(
        @Body request: RegistrarFacturaDesdeSunatRequest
    ): RegistrarFacturaDesdeSunatResponse

    @GET("factura/ui/{numeroComprobante}")
    suspend fun obtenerFacturaParaUI(
        @Path("numeroComprobante") numeroComprobante: String
    ): FacturaUIResponse

    @GET("factura/ui/usuario/{usuarioId}")
    suspend fun obtenerFacturasUsuarioParaUI(
        @Path("usuarioId") usuarioId: String
    ): FacturasUIResponse
}

// Nuevas clases de datos para productos
data class ProductoRequest(
    val descripcion: String,
    val cantidad: Double,
    val costoUnitario: Double,
    val unidadMedida: String
)

data class GuardarProductosRequest(
    val productos: List<ProductoRequest>
)

data class GuardarProductosResponse(
    val success: Boolean,
    val message: String,
    val productosGuardados: Int,
    val facturaId: Int?,
    val estadoActualizado: Boolean?
)

data class ScrapingCompletadoRequest(
    val productos: List<ProductoRequest>? = null
)

data class FacturaUIResponse(
    val success: Boolean,
    val message: String,
    val factura: FacturaRegistradaResponse,
    val nota: String
)

data class FacturasUIResponse(
    val success: Boolean,
    val message: String,
    val count: Int,
    val distribucionEstados: Map<String, Int>,
    val facturas: List<FacturaRegistradaResponse>,
    val nota: String
)

data class DetalleFacturaRequest(
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val ruc: String,
    val usuario_sol: String,
    val clave_sol: String
)

data class DetalleFacturaXmlResponse(
    val id: String,
    val fechaEmision: String?,
    val horaEmision: String?,
    val moneda: String?,
    val emisor: EmisorResponse?,
    val receptor: ReceptorResponse?,
    val subtotal: Double?,
    val igv: Double?,
    val total: Double?,
    val items: List<ItemResponse>?,
    val archivoXml: String?
)

data class EmisorResponse(val ruc: String?, val nombre: String?)
data class ReceptorResponse(val ruc: String?, val nombre: String?)

data class ItemResponse(
    val cantidad: Double,
    val unidad: String,
    val codigo: String?,
    val descripcion: String,
    val valorUnitario: Double
)

data class SunatResponse(
    val success: Boolean,
    val periodoInicio: String,
    val periodoFin: String,
    val resultados: List<SunatResultado>
)

data class SunatResultado(
    val periodo: String,
    val contenido: List<ContenidoItem>
)

data class ContenidoItem(
    val rucEmisor: String,
    val razonSocialEmisor: String,
    val periodo: String,
    val carSunat: String,
    val fechaEmision: String,
    val tipoCP: String,
    val serie: String,
    val numero: String,
    val tipoDocReceptor: String,
    val nroDocReceptor: String,
    val nombreReceptor: String,
    val baseGravada: Double,
    val igv: Double,
    val montoNoGravado: Double,
    val total: Double,
    val moneda: String,
    val tipodecambio: Double?,
    val estado: String
)

data class ProductoParaRegistrar(
    val descripcion: String,
    val cantidad: String,
    val costoUnitario: String,
    val unidadMedida: String
)

data class FacturaParaRegistrar(
    val id: Int,
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val fechaEmision: String,
    val razonSocial: String,
    val tipoDocumento: String,
    val moneda: String,
    val costoTotal: String,
    val igv: String,
    val importeTotal: String,
    val productos: List<ProductoParaRegistrar>
)

data class RegistroFacturasRequest(
    val facturas: List<FacturaParaRegistrar>
)

data class RegistroFacturasResponse(
    val message: String,
    val resultados: List<ResultadoRegistro>
)

data class ResultadoRegistro(
    val success: Boolean,
    val id: Int,
    val numeroComprobante: String
)

data class FacturaRegistradaResponse(
    val idFactura: Int,
    val numeroComprobante: String,
    val fechaEmision: String,
    val estado: String,
    val proveedorRuc: String,
    val costoTotal: String,
    val igv: String,
    val importeTotal: String,
    val moneda: String,
    val numero: String,
    val serie: String,
    val detalles: List<DetalleRegistrado>?,
    val proveedor: ProveedorRegistrado?
)

data class DetalleRegistrado(
    val descripcion: String,
    val cantidad: String,
    val costoUnitario: String,
    val unidadMedida: String
)

data class ProveedorRegistrado(
    val rucProveedor: String,
    val razonSocial: String
)

data class ScrapingCompletadoResponse(
    val message: String,
    val timestamp: String,
    val factura: FacturaScrapingResponse?,
    val estado: String?,
    val productosGuardados: Int?,
    val advertencia: String?
)

data class FacturaScrapingResponse(
    val idFactura: Int,
    val numeroComprobante: String,
    val estado: String
)

data class RegistrarFacturaDesdeSunatRequest(
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val fechaEmision: String,
    val razonSocial: String,
    val tipoDocumento: String,
    val moneda: String,
    val costoTotal: String,
    val igv: String,
    val importeTotal: String,
    val usuarioId: Int = 1
)

data class RegistrarFacturaDesdeSunatResponse(
    val success: Boolean,
    val idFactura: Int?,
    val numeroComprobante: String,
    val message: String
)

class InvoiceViewModel : ViewModel() {

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.76:3043/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
        .build()

    private val sunatApiService = retrofit.create(SunatApiService::class.java)

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

    fun registrarFacturasEnBaseDeDatos(
        facturas: List<Invoice>,
        esCompra: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _registroCompletado.value = false

            try {
                println("📤 [ViewModel] Preparando ${facturas.size} facturas para registrar en BD...")

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

                println("📤 [ViewModel] Enviando ${facturasParaRegistrar.size} facturas a BD...")

                val request = RegistroFacturasRequest(facturas = facturasParaRegistrar)
                val response = sunatApiService.registrarFacturasEnBD(request)

                val todosExitosos = response.resultados.all { it.success }
                val facturasRegistradas = response.resultados.count { it.success }

                if (todosExitosos) {
                    println("✅ [ViewModel] Facturas registradas en BD: $facturasRegistradas")
                    println("✅ Mensaje del servidor: ${response.message}")

                    response.resultados.forEach { resultado ->
                        println("✅   Factura ${resultado.numeroComprobante} registrada con ID: ${resultado.id}")
                    }

                    facturas.forEach { factura ->
                        actualizarEstadoFactura(factura.id, "REGISTRADO", esCompra)
                    }

                    _registroCompletado.value = true

                } else {
                    val errores = response.resultados.filter { !it.success }
                    val errorMsg = "Algunas facturas no se pudieron registrar: ${errores.map { it.numeroComprobante }}"
                    println("❌ [ViewModel] $errorMsg")
                    _errorMessage.value = errorMsg
                }

            } catch (e: Exception) {
                val errorMsg = "Error de conexión al registrar en BD: ${e.message}"
                println("❌ [ViewModel] $errorMsg")
                _errorMessage.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetRegistroCompletado() {
        _registroCompletado.value = false
    }

    fun cargarFacturasDesdeAPI(periodoInicio: String, periodoFin: String, esCompra: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = sunatApiService.obtenerFacturas(periodoInicio, periodoFin)

                if (response.success) {
                    val facturas = parsearContenidoSunat(response.resultados)

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

    private suspend fun parsearContenidoSunat(resultados: List<SunatResultado>): List<Invoice> {
        val facturas = mutableListOf<Invoice>()
        var idCounter = 1

        val facturasExistentesCompras = _facturasCompras.value
        val facturasExistentesVentas = _facturasVentas.value
        val todasFacturasExistentes = facturasExistentesCompras + facturasExistentesVentas

        val maxIdActual = todasFacturasExistentes.maxOfOrNull { it.id } ?: 0
        idCounter = maxIdActual + 1

        resultados.forEach { resultado ->
            resultado.contenido.forEach { item ->
                val numeroComprobante = "${item.serie}-${item.numero}"
                var estadoDesdeBD = "CONSULTADO"
                var productosDesdeBD: List<ProductItem> = emptyList()

                try {
                    val facturaUI = sunatApiService.obtenerFacturaParaUI(numeroComprobante)
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
                        println("✅ Factura $numeroComprobante - Estado UI: $estadoDesdeBD, Productos: ${productosDesdeBD.size}")
                    } else {
                        println("✅ Factura $numeroComprobante - Estado UI: $estadoDesdeBD, Sin productos")
                    }
                } catch (e: Exception) {
                    println("ℹ️ Factura $numeroComprobante no está registrada en BD aún")
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

                _rucEmisores[id] = item.rucEmisor

                val factura = Invoice(
                    id = id,
                    ruc = item.nroDocReceptor,
                    serie = item.serie,
                    numero = item.numero,
                    fechaEmision = item.fechaEmision,
                    razonSocial = item.nombreReceptor,
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

        return facturas.sortedBy { factura ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    fun cargarDetalleFacturaXml(
        rucEmisor: String,
        serie: String,
        numero: String,
        ruc: String,
        usuarioSol: String,
        claveSol: String,
        facturaId: Int,
        esCompra: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                println("📤 [ViewModel] Enviando solicitud XML:")
                println("📤 RUC Emisor: $rucEmisor")
                println("📤 Serie: $serie, Número: $numero")
                println("📤 RUC Receptor: $ruc")
                println("📤 Usuario: $usuarioSol")

                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisor,
                    serie = serie,
                    numero = numero,
                    ruc = ruc,
                    usuario_sol = usuarioSol,
                    clave_sol = claveSol
                )

                val detalle = sunatApiService.obtenerDetalleFacturaXml(request)

                println("📥 [ViewModel] Respuesta recibida del API:")
                println("📥 Número de items: ${detalle.items?.size ?: 0}")

                if (detalle.items != null && detalle.items.isNotEmpty()) {
                    val productos = detalle.items.map { item ->
                        ProductItem(
                            descripcion = item.descripcion,
                            cantidad = item.cantidad.toString(),
                            costoUnitario = String.format("%.2f", item.valorUnitario),
                            unidadMedida = item.unidad
                        )
                    }

                    println("✅ [ViewModel] Productos convertidos: ${productos.size}")

                    actualizarProductosFactura(facturaId, productos, esCompra)

                    println("✅ Detalles XML obtenidos: ${productos.size} productos")
                } else {
                    println("⚠️ El XML no contiene items")
                    _errorMessage.value = "El XML no contiene detalles de productos"
                }
            } catch (e: Exception) {
                println("❌ Error obteniendo detalles XML: ${e.message}")
                _errorMessage.value = "Error al obtener detalles: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun actualizarProductosFactura(
        facturaId: Int,
        productos: List<ProductItem>,
        esCompra: Boolean
    ) {
        println("🔥 [ViewModel] actualizarProductosFactura INICIADO")
        println("🔥 [ViewModel] facturaId: $facturaId")
        println("🔥 [ViewModel] productos.size: ${productos.size}")

        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            println("🔥 [ViewModel] ¡ENCONTRADA! Actualizando factura ID=$facturaId con ${productos.size} productos")
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

    // MÉTODO PRINCIPAL MODIFICADO - CON GUARDADO DE PRODUCTOS
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

        println("🔍 [ViewModel] Datos para consulta XML:")
        println("🔍 RUC (YO): $miRuc")
        println("🔍 RUC Emisor recibido: $rucEmisor")
        println("🔍 Tipo operación: ${if (esCompra) "COMPRA" else "VENTA"}")
        println("🔍 Usuario SOL: $usuario")

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
        println("🔢 [ViewModel] Número comprobante: $numeroComprobante")

        if (factura.estado == "CON DETALLE" || factura.estado == "REGISTRADO") {
            println("✅ [ViewModel] Factura ya tiene estado '${factura.estado}' - Mostrando datos existentes")
            if (factura.productos.isNotEmpty()) {
                println("✅ [ViewModel] Ya tiene ${factura.productos.size} productos cargados")
                onLoadingComplete(true, "Detalles ya cargados")
            } else {
                println("⚠️ [ViewModel] Estado es '${factura.estado}' pero no tiene productos")
                _errorMessage.value = "No hay detalles disponibles"
                onLoadingComplete(false, "No hay detalles disponibles")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Registrar la factura en BD si no existe
                try {
                    val facturaEnBD = sunatApiService.verificarFacturaRegistrada(numeroComprobante)
                    println("✅ Factura ya está en BD con ID: ${facturaEnBD.idFactura}")
                } catch (e: Exception) {
                    println("⚠️ Factura no está en BD, registrándola...")

                    val registroRequest = RegistrarFacturaDesdeSunatRequest(
                        rucEmisor = rucEmisor,
                        serie = factura.serie,
                        numero = factura.numero,
                        fechaEmision = factura.fechaEmision,
                        razonSocial = factura.razonSocial,
                        tipoDocumento = factura.tipoDocumento,
                        moneda = factura.moneda,
                        costoTotal = factura.costoTotal,
                        igv = factura.igv,
                        importeTotal = factura.importeTotal,
                        usuarioId = 1
                    )

                    val registroResponse = sunatApiService.registrarFacturaDesdeSunat(registroRequest)

                    if (registroResponse.success) {
                        println("✅ Factura registrada en BD con ID: ${registroResponse.idFactura}")
                    } else {
                        println("⚠️ No se pudo registrar factura: ${registroResponse.message}")
                    }
                }

                // 2. Actualizar estado local a "PROCESANDO..."
                actualizarEstadoFactura(facturaId, "PROCESANDO...", esCompra)

                val rucEmisorParaAPI = if (esCompra) factura.ruc else miRuc
                val rucReceptorParaAPI = if (esCompra) miRuc else factura.ruc

                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisorParaAPI,
                    serie = factura.serie,
                    numero = factura.numero,
                    ruc = rucReceptorParaAPI,
                    usuario_sol = usuario,
                    clave_sol = claveSol
                )

                // 3. Obtener XML con productos
                val detalle = sunatApiService.obtenerDetalleFacturaXml(request)

                if (detalle.items != null && detalle.items.isNotEmpty()) {
                    val productos = detalle.items.map { item ->
                        ProductItem(
                            descripcion = item.descripcion,
                            cantidad = item.cantidad.toString(),
                            costoUnitario = String.format("%.2f", item.valorUnitario),
                            unidadMedida = item.unidad
                        )
                    }

                    // 4. Actualizar localmente
                    actualizarProductosFactura(facturaId, productos, esCompra)
                    println("✅ [ViewModel] Productos actualizados localmente: ${productos.size}")

                    // 5. Convertir a formato para backend
                    val productosParaGuardar = productos.map { producto ->
                        ProductoRequest(
                            descripcion = producto.descripcion,
                            cantidad = producto.cantidad.toDoubleOrNull() ?: 0.0,
                            costoUnitario = producto.costoUnitario.toDoubleOrNull() ?: 0.0,
                            unidadMedida = producto.unidadMedida
                        )
                    }

                    println("📤 [ViewModel] Preparando ${productosParaGuardar.size} productos para guardar en BD...")

                    try {
                        // OPCIÓN 1: Guardar productos directamente
                        println("📤 [ViewModel] Guardando productos en BD...")
                        val guardarProductosResponse = sunatApiService.guardarProductosFactura(
                            numeroComprobante,
                            GuardarProductosRequest(productos = productosParaGuardar)
                        )

                        if (guardarProductosResponse.success) {
                            println("✅ [ViewModel] Productos guardados en BD: ${guardarProductosResponse.productosGuardados}")
                        } else {
                            println("⚠️ [ViewModel] Error al guardar productos: ${guardarProductosResponse.message}")
                        }
                    } catch (e: Exception) {
                        println("⚠️ [ViewModel] Error al guardar productos: ${e.message}")
                    }

                    try {
                        // OPCIÓN 2: Marcar scraping como completado (con o sin productos)
                        println("📤 [ViewModel] Marcando scraping como completado...")

                        val scrapingRequest = ScrapingCompletadoRequest(
                            productos = productosParaGuardar
                        )

                        val respuestaBackend = sunatApiService.marcarScrapingCompletado(
                            numeroComprobante,
                            scrapingRequest
                        )

                        println("✅ [ViewModel] Backend confirmó: ${respuestaBackend.message}")
                        println("✅ [ViewModel] Estado persistido: ${respuestaBackend.estado}")
                        println("✅ [ViewModel] Productos guardados: ${respuestaBackend.productosGuardados ?: 0}")

                    } catch (e: Exception) {
                        println("⚠️ [ViewModel] Error al marcar scraping: ${e.message}")
                        _errorMessage.value = "Detalles obtenidos, pero error al guardar en servidor"
                    }

                    // 6. Actualizar estado local a "CON DETALLE"
                    val estadoActual = if (esCompra) {
                        _facturasCompras.value.firstOrNull { it.id == facturaId }?.estado
                    } else {
                        _facturasVentas.value.firstOrNull { it.id == facturaId }?.estado
                    }

                    if (estadoActual != "REGISTRADO") {
                        actualizarEstadoFactura(facturaId, "CON DETALLE", esCompra)
                    }

                    onLoadingComplete(true, "Detalles obtenidos y guardados exitosamente")
                    println("✅ [ViewModel] Proceso completado exitosamente")

                } else {
                    _errorMessage.value = "El XML no contiene detalles de productos"
                    onLoadingComplete(false, "El XML no contiene detalles de productos")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al obtener detalles: ${e.message}"
                onLoadingComplete(false, "Error: ${e.message}")
                println("❌ [ViewModel] Error: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
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
            println("🆕 [ViewModel] Agregando nueva factura COMPRA...")
            println("📝 Datos: RUC=$ruc, Serie=$serie, Número=$numero, Fecha=$fechaEmision")

            val estadoInicial = if (productos.isNotEmpty()) {
                "CON DETALLE"
            } else {
                "CONSULTADO"
            }

            println("📝 Estado asignado: $estadoInicial")

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

    fun actualizarEstadoFactura(facturaId: Int, nuevoEstado: String, esCompra: Boolean) {
        println("🔄 [ViewModel] Llamando actualizarEstadoFactura")
        println("🔄 [ViewModel] ID: $facturaId, Estado: '$nuevoEstado', esCompra: $esCompra")

        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            println("✅ [ViewModel] Factura COMPRA actualizada: ID=${factura.id}")
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
                            println("✅ [ViewModel] Factura VENTA actualizada: ID=${factura.id}")
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }

    fun actualizarSeleccionCompras(id: Int, isSelected: Boolean) {
        println("🔄 [ViewModel] actualizarSeleccionCompras - ID: $id, Selected: $isSelected")
        viewModelScope.launch {
            _facturasCompras.update { lista ->
                lista.map { factura ->
                    if (factura.id == id) {
                        println("✅ [ViewModel] Factura COMPRA actualizada: ID=${factura.id}, Selected=$isSelected")
                        factura.copy(isSelected = isSelected)
                    } else {
                        factura
                    }
                }
            }
        }
    }

    fun actualizarSeleccionVentas(id: Int, isSelected: Boolean) {
        println("🔄 [ViewModel] actualizarSeleccionVentas - ID: $id, Selected: $isSelected")
        viewModelScope.launch {
            _facturasVentas.update { lista ->
                lista.map { factura ->
                    if (factura.id == id) {
                        println("✅ [ViewModel] Factura VENTA actualizada: ID=${factura.id}, Selected=$isSelected")
                        factura.copy(isSelected = isSelected)
                    } else {
                        factura
                    }
                }
            }
        }
    }

    fun seleccionarTodasCompras(seleccionar: Boolean) {
        viewModelScope.launch {
            _facturasCompras.update { lista ->
                lista.map { factura ->
                    factura.copy(isSelected = seleccionar)
                }
            }
        }
    }

    fun seleccionarTodasVentas(seleccionar: Boolean) {
        viewModelScope.launch {
            _facturasVentas.update { lista ->
                lista.map { factura ->
                    factura.copy(isSelected = seleccionar)
                }
            }
        }
    }

    // Función para refrescar datos desde el backend
    suspend fun refrescarDatosDesdeBackend(usuarioId: Int = 1) {
        println("🔄 [ViewModel] Refrescando datos desde backend...")

        try {
            val response = sunatApiService.obtenerFacturasUsuarioParaUI(usuarioId.toString())

            if (response.success) {
                println("✅ [ViewModel] Datos refrescados: ${response.count} facturas")
                println("✅ [ViewModel] Distribución: ${response.distribucionEstados}")

                // Aquí puedes actualizar tu estado local con los datos del backend
                // Por ahora solo logueamos la información
                response.facturas.forEach { factura ->
                    println("📋 Factura ${factura.numeroComprobante}: ${factura.estado}, Productos: ${factura.detalles?.size ?: 0}")
                }
            }
        } catch (e: Exception) {
            println("❌ [ViewModel] Error al refrescar datos: ${e.message}")
        }
    }
}