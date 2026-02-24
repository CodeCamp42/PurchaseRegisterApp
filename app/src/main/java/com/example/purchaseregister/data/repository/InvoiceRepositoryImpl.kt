package com.example.purchaseregister.data.repository

import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.RegisteredInvoiceResponse
import com.example.purchaseregister.api.responses.SunatResponse
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
import com.example.purchaseregister.api.responses.AuthResponse
import com.example.purchaseregister.api.responses.SaveSunatCredentialsResponse

class InvoiceRepositoryImpl : InvoiceRepository {

    private val apiService = RetrofitClient.sunatApiService

    // Estado Local
    private val _purchaseInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    override val purchaseInvoices: StateFlow<List<Invoice>> = _purchaseInvoices.asStateFlow()

    private val _salesInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    override val salesInvoices: StateFlow<List<Invoice>> = _salesInvoices.asStateFlow()

    private val _issuerRucs = mutableMapOf<Int, String>()
    private val _invoicesCache = mutableMapOf<String, List<Invoice>>()

    // Helper privados
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

    override suspend fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        ruc: String,
        solUsername: String,
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

        try {
            val response = apiService.getInvoices(
                periodStart,
                periodEnd,
                ruc,
                solUsername,
                solPassword,
                clientId,
                clientSecret
            )

            return if (response.isNotEmpty()) {
                val apiInvoices = parseSunatContent(response, isPurchase)
                updateCache(cacheKey, apiInvoices)
                apiInvoices
            } else {
                emptyList()
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()

            val errorMessage = try {
                val gson = com.google.gson.Gson()
                val errorResponse = gson.fromJson(errorBody, Map::class.java)

                if (errorResponse["code"] == "SIRE_AUTH_ERROR") {
                    "${errorResponse["message"]}"
                } else {
                    errorResponse["message"] as? String ?: "Error en la petición"
                }
            } catch (jsonEx: Exception) {
                "Error al autenticar con SUNAT: ${e.message}"
            }

            throw Exception(errorMessage)

        } catch (e: Exception) {
            throw Exception("Error de conexión: ${e.message}")
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
        val solUsername = SunatPrefs.getSolUsername(context) ?: run {
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
                solUsername = solUsername,
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

    // Función privada para el polling
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
        // Como ya no existe el endpoint, retornamos null
        return null
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
        solUsername: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): Result<Boolean> {
        return try {
            val response = apiService.saveSunatCredentials(
                SaveSunatCredentialsRequest(
                    ruc = ruc,
                    solUsername = solUsername,
                    solPassword = solPassword,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            )

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = com.google.gson.Gson()
                    val errorResponse = gson.fromJson(errorBody, SaveSunatCredentialsResponse::class.java)
                    errorResponse.message ?: "Error ${response.code()}"
                } catch (e: Exception) {
                    "Error ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun parseSunatContent(
        items: List<SunatResponse>,
        isPurchase: Boolean
    ): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        val allExistingInvoices = getPurchaseInvoices() + getSalesInvoices()
        val maxCurrentId = (allExistingInvoices.maxOfOrNull { it.id } ?: 0) + 1
        var idCounter = maxCurrentId

        items.forEach { item ->
            val id = idCounter++

            // Guardar RUC para consultas de detalle
            setIssuerRuc(id, item.issuerRuc)

            val invoice = Invoice(
                id = id,
                ruc = if (isPurchase) item.receiverDocNumber else item.issuerRuc,
                businessName = if (isPurchase) item.receiverName else item.issuerName,
                series = item.series,
                number = item.number,
                issueDate = item.issueDate,
                documentType = when (item.docType) {
                    "01" -> "FACTURA"
                    "03" -> "BOLETA"
                    else -> "DOCUMENTO"
                },
                currency = when (item.currency) {
                    "PEN" -> "Soles (PEN)"
                    "USD" -> "Dólares (USD)"
                    else -> item.currency
                },
                totalCost = item.taxableAmount.toString(),
                igv = item.igv.toString(),
                totalAmount = item.totalAmount.toString(),
                status = "CONSULTADO", // Por defecto
                isSelected = false,
                products = emptyList(),
                year = item.period.take(4),
                exchangeRate = item.exchangeRate.toString()
            )
            invoices.add(invoice)
        }

        val sortedInvoices = invoices.sortedBy { invoice ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        if (isPurchase) {
            _purchaseInvoices.value = sortedInvoices
        } else {
            _salesInvoices.value = sortedInvoices
        }

        return sortedInvoices
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    val gson = com.google.gson.Gson()
                    gson.fromJson(errorBody, AuthResponse::class.java)
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorResponse?.message ?: "Error ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(name, email, password))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    val gson = com.google.gson.Gson()
                    gson.fromJson(errorBody, AuthResponse::class.java)
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorResponse?.message ?: "Error ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        return try {
            val response = apiService.requestPasswordReset(
                ForgotPasswordRequest(email)
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error al enviar el correo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(token: String): Result<Unit> {
        return try {
            val authHeader = "Bearer $token"
            val response = apiService.signOut(authHeader)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Error al cerrar sesión"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}