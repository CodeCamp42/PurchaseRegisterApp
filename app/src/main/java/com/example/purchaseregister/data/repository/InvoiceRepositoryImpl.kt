package com.example.purchaseregister.data.repository

import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.RegisteredInvoiceResponse
import com.example.purchaseregister.api.responses.SunatResult
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceRepositoryImpl : InvoiceRepository {

    private val apiService = RetrofitClient.sunatApiService

    // --- Estado Local
    private val _purchaseInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    override val purchaseInvoices: StateFlow<List<Invoice>> = _purchaseInvoices.asStateFlow()

    private val _salesInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    override val salesInvoices: StateFlow<List<Invoice>> = _salesInvoices.asStateFlow()

    private val _issuerRucs = mutableMapOf<Int, String>()
    private val _invoicesCache = mutableMapOf<String, List<Invoice>>()

    // --- Helper privados
    private fun getPurchaseInvoices(): List<Invoice> = _purchaseInvoices.value
    private fun getSalesInvoices(): List<Invoice> = _salesInvoices.value

    private fun updatePurchaseInvoices(update: (List<Invoice>) -> List<Invoice>) {
        _purchaseInvoices.update { update(it) }
    }

    private fun updateSalesInvoices(update: (List<Invoice>) -> List<Invoice>) {
        _salesInvoices.update { update(it) }
    }

    private fun setPurchaseInvoices(invoices: List<Invoice>) {
        _purchaseInvoices.value = invoices
    }

    private fun setSalesInvoices(invoices: List<Invoice>) {
        _salesInvoices.value = invoices
    }

    override fun getIssuerRuc(invoiceId: Int): String? = _issuerRucs[invoiceId]

    private fun setIssuerRuc(invoiceId: Int, ruc: String) {
        _issuerRucs[invoiceId] = ruc
    }

    private fun getCacheKey(isPurchase: Boolean, periodStart: String): String {
        return "${if (isPurchase) "COMPRAS" else "VENTAS"}-${periodStart}"
    }

    private fun getCachedInvoices(key: String): List<Invoice>? = _invoicesCache[key]

    private fun updateCache(key: String, invoices: List<Invoice>) {
        _invoicesCache[key] = invoices
    }

    fun updateInvoiceInAllCaches(originalInvoice: Invoice, newStatus: String) {
        _invoicesCache.forEach { (key, cachedInvoices) ->
            val updatedInvoices = cachedInvoices.map { cachedInvoice ->
                if (cachedInvoice.ruc == originalInvoice.ruc &&
                    cachedInvoice.series == originalInvoice.series &&
                    cachedInvoice.number == originalInvoice.number) {
                    cachedInvoice.copy(status = newStatus)
                } else {
                    cachedInvoice
                }
            }
            _invoicesCache[key] = updatedInvoices
        }
    }

    override fun clearAll() {
        _purchaseInvoices.value = emptyList()
        _salesInvoices.value = emptyList()
        _invoicesCache.clear()
        _issuerRucs.clear()
    }

    // --- Implementación de métodos de la interfaz ---
    override suspend fun loadInvoicesFromDB(isPurchase: Boolean): List<Invoice> {
        return try {
            println("🔴🔴🔴 [Repository] INICIANDO CARGA DESDE BD")
            val response = apiService.getCompleteUserInvoices("1")

            if (response.success == true && response.invoices != null) {
                println("✅ [Repository] Facturas BD: ${response.invoices.size}")
                response.invoices.mapNotNull { invoiceResponse -> // Usamos mapNotNull para filtrar nulos
                    mapRegisteredInvoiceToModel(invoiceResponse)
                }
            } else {
                println("❌ [Repository] response.success es false")
                emptyList()
            }
        } catch (e: Exception) {
            println("💥 [Repository] ERROR EN CARGA BD: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        ruc: String,
        user: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): List<Invoice> {
        val cacheKey = getCacheKey(isPurchase, periodStart)
        val cached = getCachedInvoices(cacheKey)
        if (cached != null) {
            println("📦 [Repository] Usando cache")
            return cached
        }

        println("🌐 [Repository] Llamando a API SUNAT")
        val response = apiService.getInvoices(
            periodStart, periodEnd, ruc, user, solPassword, clientId, clientSecret
        )

        return if (response.success == true) {
            val apiInvoices = parseSunatContent(response.results, isPurchase)
            updateCache(cacheKey, apiInvoices)
            apiInvoices
        } else {
            println("❌ [Repository] Error en respuesta API")
            emptyList()
        }
    }

    override suspend fun loadInvoiceDetail(
        invoiceId: Int,
        isPurchase: Boolean,
        issuerRuc: String,
        context: Context,
        onJobQueued: (String) -> Unit,
        onStatusUpdate: (Int, String) -> Unit,
        onProductsUpdate: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        // Esta función ahora contiene la lógica de ScrapingManager
        val invoice = if (isPurchase) getPurchaseInvoices().firstOrNull { it.id == invoiceId }
        else getSalesInvoices().firstOrNull { it.id == invoiceId } ?: run {
            onError("Factura no encontrada")
            return
        }

        val myRuc = SunatPrefs.getRuc(context) ?: run {
            onError("Complete sus credenciales SUNAT primero")
            return
        }
        val user = SunatPrefs.getUser(context) ?: run {
            onError("Complete sus credenciales SUNAT primero")
            return
        }
        val solPassword = SunatPrefs.getSolPassword(context) ?: run {
            onError("Complete sus credenciales SUNAT primero")
            return
        }

        // Iniciar proceso de scraping
        try {
            val request = InvoiceDetailRequest(
                issuerRuc = issuerRuc,
                series = invoice?.series,
                number = invoice?.number,
                ruc = if (isPurchase) myRuc else invoice?.ruc,
                solUser = user,
                solPassword = solPassword
            )

            val queuedResponse = apiService.downloadXmlWithQueue(request)

            if (queuedResponse.success == true && queuedResponse.jobId != null) {
                onJobQueued(queuedResponse.jobId)
                startPollingJob(
                    jobId = queuedResponse.jobId,
                    invoiceId = invoiceId,
                    isPurchase = isPurchase,
                    onStatusUpdate = onStatusUpdate,
                    onProductsUpdate = onProductsUpdate,
                    onError = onError
                )
            } else {
                onError("Error al encolar trabajo: ${queuedResponse.message}")
            }
        } catch (e: Exception) {
            onError("Error: ${e.message}")
        }
    }

    // Función privada para el polling, ahora dentro del repositorio
    private suspend fun startPollingJob(
        jobId: String,
        invoiceId: Int,
        isPurchase: Boolean,
        onStatusUpdate: (Int, String) -> Unit,
        onProductsUpdate: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        var attempts = 0
        val maxAttempts = 60

        while (attempts < maxAttempts) {
            delay(3000)
            try {
                val jobStatus = apiService.getJobStatus(jobId)

                when (jobStatus.state) {
                    "completed" -> {
                        val products = jobStatus.result?.items?.mapNotNull { item ->
                            // Mapeo con manejo de nulos
                            item?.let {
                                ProductItem(
                                    description = it.description ?: "",
                                    quantity = it.quantity?.toString() ?: "0",
                                    unitCost = String.format("%.2f", it.unitValue ?: 0.0),
                                    unitOfMeasure = it.unit ?: ""
                                )
                            }
                        } ?: emptyList()

                        onProductsUpdate(invoiceId, products, isPurchase)
                        onStatusUpdate(invoiceId, "CON DETALLE")
                        saveProductsInBackend(jobStatus.result?.id ?: "sin-id", products)
                        return
                    }
                    "failed" -> {
                        onError("Scraping falló: ${jobStatus.reason}")
                        return
                    }
                }
            } catch (e: Exception) {
                // Ignorar errores de polling y reintentar
            }
            attempts++
        }
        onError("Timeout: El scraping no se completó")
    }

    private suspend fun saveProductsInBackend(documentNumber: String, products: List<ProductItem>) {
        try {
            val productsToSave = products.map { product ->
                ProductRequest(
                    description = product.description,
                    quantity = product.quantity.toDoubleOrNull() ?: 0.0,
                    unitCost = product.unitCost.toDoubleOrNull() ?: 0.0,
                    unitOfMeasure = product.unitOfMeasure
                )
            }
            apiService.saveInvoiceProducts(documentNumber, SaveProductsRequest(products = productsToSave))
            apiService.markScrapingCompleted(documentNumber, ScrapingCompletedRequest(products = productsToSave))
        } catch (e: Exception) {
            // Silencioso
        }
    }

    override suspend fun registerInvoicesInDatabase(
        invoices: List<Invoice>,
        isPurchase: Boolean
    ): Result<Unit> {
        return try {
            val invoicesToRegister = invoices.map { invoice ->
                InvoiceToRegister(
                    id = invoice.id,
                    issuerRuc = invoice.ruc,
                    series = invoice.series,
                    number = invoice.number,
                    issueDate = invoice.issueDate,
                    businessName = invoice.businessName,
                    documentType = invoice.documentType,
                    currency = invoice.currency,
                    totalCost = invoice.totalCost,
                    igv = invoice.igv,
                    totalAmount = invoice.totalAmount,
                    products = invoice.products.map { product ->
                        ProductToRegister(
                            description = product.description,
                            quantity = product.quantity,
                            unitCost = product.unitCost,
                            unitOfMeasure = product.unitOfMeasure
                        )
                    }
                )
            }

            val request = RegisterInvoicesRequest(invoices = invoicesToRegister)
            val response = apiService.registerInvoicesInDB(request)

            val allSuccessful = response.results?.all { it.success == true } ?: false

            if (allSuccessful) {
                invoices.forEach { invoice ->
                    updateInvoiceStatus(invoice.id, "REGISTRADO", isPurchase)
                }
                Result.success(Unit)
            } else {
                val errorMsg = "Algunas facturas no se pudieron registrar"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerNewPurchaseInvoice(invoiceData: Map<String, Any>): Invoice? {
        return try {
            val request = RegisterInvoiceFromSunatRequest(
                issuerRuc = invoiceData["ruc"] as? String,
                series = invoiceData["series"] as? String,
                number = invoiceData["number"] as? String,
                issueDate = invoiceData["issueDate"] as? String,
                businessName = invoiceData["businessName"] as? String,
                documentType = invoiceData["documentType"] as? String,
                currency = invoiceData["currency"] as? String,
                totalCost = invoiceData["totalCost"] as? String,
                igv = invoiceData["igv"] as? String,
                totalAmount = invoiceData["totalAmount"] as? String,
                userId = 1
            )

            val response = apiService.registerInvoiceFromSunat(request)

            if (response.success == true && response.invoiceId != null) {
                val products = invoiceData["products"] as? List<ProductItem> ?: emptyList()
                val initialStatus = if (products.isNotEmpty()) "CON DETALLE" else "CONSULTADO"

                Invoice(
                    id = response.invoiceId!!,
                    ruc = request.issuerRuc ?: "",
                    businessName = request.businessName ?: "",
                    series = request.series ?: "",
                    number = request.number ?: "",
                    issueDate = request.issueDate ?: "",
                    documentType = request.documentType ?: "",
                    year = invoiceData["year"] as? String ?: "",
                    currency = request.currency ?: "",
                    totalCost = request.totalCost ?: "",
                    igv = request.igv ?: "",
                    exchangeRate = invoiceData["exchangeRate"] as? String ?: "",
                    totalAmount = request.totalAmount ?: "",
                    status = initialStatus,
                    isSelected = false,
                    products = products
                )
            } else {
                println("❌ Error registrando factura en BD: ${response.message}")
                null
            }
        } catch (e: Exception) {
            println("❌ Excepción al registrar factura: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateInvoiceStatus(invoiceId: Int, newStatus: String, isPurchase: Boolean) {
        if (isPurchase) {
            updatePurchaseInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) {
                        updateInvoiceInAllCaches(invoice, newStatus)
                        invoice.copy(status = newStatus)
                    } else invoice
                }
            }
        } else {
            updateSalesInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) {
                        updateInvoiceInAllCaches(invoice, newStatus)
                        invoice.copy(status = newStatus)
                    } else invoice
                }
            }
        }
    }

    override suspend fun updateInvoiceProducts(invoiceId: Int, products: List<ProductItem>, isPurchase: Boolean) {
        if (isPurchase) {
            updatePurchaseInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) invoice.copy(products = products) else invoice
                }
            }
        } else {
            updateSalesInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) invoice.copy(products = products) else invoice
                }
            }
        }
    }

    override suspend fun validateSunatCredentials(
        ruc: String,
        user: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): Boolean {
        return try {
            val response = apiService.validateCredentials(
                ValidateCredentialsRequest(
                    ruc = ruc,
                    user = user,
                    solPassword = solPassword,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            )
            response.valid == true
        } catch (e: Exception) {
            false
        }
    }

    // --- Funciones de mapeo privadas con manejo de nulos ---

    private fun mapRegisteredInvoiceToModel(response: RegisteredInvoiceResponse): Invoice? {
        // Si falta un dato crítico, podrías retornar null o manejarlo con valores por defecto.
        // Aquí optamos por valores por defecto para no perder la factura, pero es una decisión de diseño.
        return try {
            Invoice(
                id = response.invoiceId ?: 0,
                ruc = response.providerRuc ?: "",
                businessName = response.provider?.businessName ?: "",
                series = response.series ?: "",
                number = response.number ?: "",
                issueDate = response.issueDate ?: "",
                documentType = "FACTURA", // Podrías mapearlo si viene en la respuesta
                currency = response.currency ?: "",
                totalCost = response.totalCost ?: "",
                igv = response.igv ?: "",
                totalAmount = response.totalAmount ?: "",
                status = response.status ?: "CONSULTADO",
                isSelected = false,
                products = response.details?.mapNotNull { detail ->
                    detail?.let {
                        ProductItem(
                            description = it.description ?: "",
                            quantity = it.quantity ?: "",
                            unitCost = it.unitCost ?: "",
                            unitOfMeasure = it.unitOfMeasure ?: ""
                        )
                    }
                } ?: emptyList(),
                year = response.issueDate?.take(4) ?: "",
                exchangeRate = "" // No viene en esta respuesta
            )
        } catch (e: Exception) {
            println("Error mapeando RegisteredInvoiceResponse: ${e.message}")
            null
        }
    }

    private suspend fun parseSunatContent(
        results: List<SunatResult>?,
        isPurchase: Boolean
    ): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        val allExistingInvoices = getPurchaseInvoices() + getSalesInvoices()
        val maxCurrentId = (allExistingInvoices.maxOfOrNull { it.id } ?: 0) + 1
        var idCounter = maxCurrentId

        val invoicesToRegisterInDB = mutableListOf<RegisterInvoiceFromSunatRequest>()

        results?.forEach { result ->
            result.content?.forEach { item ->
                if (item == null) return@forEach

                val documentNumber = "${item.series ?: ""}-${item.number ?: ""}"
                var statusFromDB = "CONSULTADO"
                var productsFromDB: List<ProductItem> = emptyList()
                var existingId: Int? = null

                try {
                    val invoiceUI = apiService.getInvoiceForUI(documentNumber)
                    if (invoiceUI.invoice != null) {
                        statusFromDB = invoiceUI.invoice.status ?: "CONSULTADO"
                        existingId = invoiceUI.invoice.invoiceId

                        productsFromDB = invoiceUI.invoice.details?.mapNotNull { detail ->
                            detail?.let {
                                ProductItem(
                                    description = it.description ?: "",
                                    quantity = it.quantity ?: "",
                                    unitCost = it.unitCost ?: "",
                                    unitOfMeasure = it.unitOfMeasure ?: ""
                                )
                            }
                        } ?: emptyList()
                    }
                } catch (e: Exception) {
                    // Factura no encontrada en BD, la registraremos después
                    val respectiveBusinessName = if (isPurchase) item.issuerBusinessName else item.receiverName
                    val invoiceRequest = RegisterInvoiceFromSunatRequest(
                        issuerRuc = item.issuerRuc,
                        series = item.series,
                        number = item.number,
                        issueDate = item.issueDate,
                        businessName = respectiveBusinessName,
                        documentType = when (item.documentType) {
                            "01" -> "FACTURA"
                            "03" -> "BOLETA"
                            else -> "DOCUMENTO"
                        },
                        currency = when (item.currency) {
                            "PEN" -> "Soles (PEN)"
                            "USD" -> "Dólares (USD)"
                            else -> item.currency
                        },
                        totalCost = item.taxableBase?.let { String.format("%.2f", it) } ?: "",
                        igv = item.igv?.let { String.format("%.2f", it) } ?: "",
                        totalAmount = item.total?.let { String.format("%.2f", it) } ?: "",
                        userId = 1
                    )
                    invoicesToRegisterInDB.add(invoiceRequest)
                }

                val id = existingId ?: idCounter++

                // Guardar RUC del emisor/receptor según corresponda
                setIssuerRuc(id, item.receiverDocNumber ?: "")

                val invoice = Invoice(
                    id = id,
                    ruc = item.receiverDocNumber ?: "",
                    series = item.series ?: "",
                    number = item.number ?: "",
                    issueDate = item.issueDate ?: "",
                    businessName = if (isPurchase) item.receiverName ?: "" else item.issuerBusinessName ?: "",
                    documentType = when (item.documentType) {
                        "01" -> "FACTURA"
                        "03" -> "BOLETA"
                        else -> "DOCUMENTO"
                    },
                    currency = when (item.currency) {
                        "PEN" -> "Soles (PEN)"
                        "USD" -> "Dólares (USD)"
                        else -> item.currency ?: ""
                    },
                    totalCost = item.taxableBase?.let { String.format("%.2f", it) } ?: "",
                    igv = item.igv?.let { String.format("%.2f", it) } ?: "",
                    totalAmount = item.total?.let { String.format("%.2f", it) } ?: "",
                    status = statusFromDB,
                    isSelected = false,
                    products = productsFromDB,
                    year = item.period?.take(4) ?: "",
                    exchangeRate = item.exchangeRate?.let { String.format("%.2f", it) } ?: ""
                )
                invoices.add(invoice)
            }
        }

        // Registrar en BD en segundo plano las facturas que no existían
        if (invoicesToRegisterInDB.isNotEmpty()) {
            // Disparamos esto sin esperar el resultado para no bloquear
            CoroutineScope(context = kotlinx.coroutines.Job()).launch {
                invoicesToRegisterInDB.forEach { request ->
                    try { apiService.registerInvoiceFromSunat(request) } catch (e: Exception) { }
                }
            }
        }

        return invoices.sortedBy { invoice ->
            try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time ?: 0L }
            catch (e: Exception) { 0L }
        }
    }
}