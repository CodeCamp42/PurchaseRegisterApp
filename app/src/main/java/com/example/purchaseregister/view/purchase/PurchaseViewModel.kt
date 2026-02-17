package com.example.purchaseregister.view.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.viewmodel.shared.InvoiceRepository
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
    val purchaseInvoices: StateFlow<List<Invoice>> = InvoiceRepository.purchaseInvoices
    val salesInvoices: StateFlow<List<Invoice>> = InvoiceRepository.salesInvoices
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadInvoicesFromDB(isPurchase: Boolean = true) {
        viewModelScope.launch {
            try {
                println("🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴")
                println("📥 INICIANDO CARGA DESDE BD")
                println("🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴")

                val response = apiService.getCompleteUserInvoices("1")

                println("📦 RESPUESTA BD RECIBIDA:")
                println("   - success: ${response.success}")
                println("   - facturas size: ${response.invoices?.size ?: 0}")

                if (response.invoices != null) {
                    response.invoices.forEachIndexed { index, f ->
                        println("   BD[$index]: ${f.series}-${f.number} (ID: ${f.invoiceId})")
                    }
                }

                if (response.success) {
                    println("✅ Facturas cargadas desde BD: ${response.invoices.size}")

                    val dbInvoices = response.invoices.map { invoiceResponse ->
                        println("🔄 Mapeando factura BD: ${invoiceResponse.series}-${invoiceResponse.number}")
                        Invoice(
                            id = invoiceResponse.invoiceId,
                            ruc = invoiceResponse.providerRuc ?: "",
                            businessName = invoiceResponse.provider?.businessName ?: "",
                            series = invoiceResponse.series,
                            number = invoiceResponse.number,
                            issueDate = invoiceResponse.issueDate,
                            documentType = "FACTURA",
                            currency = invoiceResponse.currency,
                            totalCost = invoiceResponse.totalCost,
                            igv = invoiceResponse.igv,
                            totalAmount = invoiceResponse.totalAmount,
                            status = invoiceResponse.status,
                            isSelected = false,
                            products = invoiceResponse.details?.map { detail ->
                                ProductItem(
                                    description = detail.description,
                                    quantity = detail.quantity,
                                    unitCost = detail.unitCost,
                                    unitOfMeasure = detail.unitOfMeasure
                                )
                            } ?: emptyList(),
                            year = invoiceResponse.issueDate.take(4),
                            exchangeRate = ""
                        )
                    }

                    val currentInvoices = if (isPurchase) {
                        InvoiceRepository.getPurchaseInvoices()
                    } else {
                        InvoiceRepository.getSalesInvoices()
                    }

                    println("📊 Facturas de API actuales: ${currentInvoices.size}")
                    currentInvoices.forEachIndexed { index, f ->
                        println("   API[$index]: ${f.series}-${f.number} (ID: ${f.id})")
                    }

                    val combinedInvoices = combineInvoices(
                        apiInvoices = currentInvoices,
                        localInvoices = dbInvoices
                    )

                    println("📊 Facturas combinadas (API + BD): ${combinedInvoices.size}")
                    combinedInvoices.forEachIndexed { index, f ->
                        println("   COMBINADA[$index]: ${f.series}-${f.number} (ID: ${f.id})")
                    }

                    if (isPurchase) {
                        InvoiceRepository.setPurchaseInvoices(combinedInvoices)
                        println("💾 Guardadas en purchaseInvoices: ${combinedInvoices.size}")
                    } else {
                        InvoiceRepository.setSalesInvoices(combinedInvoices)
                        println("💾 Guardadas en salesInvoices: ${combinedInvoices.size}")
                    }

                    val cacheKey = InvoiceRepository.getCacheKey(isPurchase, "ultimo_periodo")
                    InvoiceRepository.updateCache(cacheKey, combinedInvoices)
                    println("💾 Caché actualizado con ${combinedInvoices.size} facturas (incluyendo BD)")

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

    fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean = true,
        ruc: String,
        user: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ) {
        viewModelScope.launch {
            val cacheKey = InvoiceRepository.getCacheKey(isPurchase, periodStart)
            val cachedInvoices = InvoiceRepository.getCachedInvoices(cacheKey)

            _errorMessage.value = null

            if (cachedInvoices != null) {
                println("📦 Usando cache para periodo: $periodStart")

                if (isPurchase) {
                    InvoiceRepository.setPurchaseInvoices(cachedInvoices)
                } else {
                    InvoiceRepository.setSalesInvoices(cachedInvoices)
                }
                return@launch
            }

            _isLoading.value = true

            try {
                val response = apiService.getInvoices(
                    periodStart,
                    periodEnd,
                    ruc,
                    user,
                    solPassword,
                    clientId,
                    clientSecret
                )

                if (response.success) {
                    val apiInvoices = parseSunatContent(response.results, isPurchase)

                    println("🌐 Facturas obtenidas de SUNAT: ${apiInvoices.size}")

                    InvoiceRepository.updateCache(cacheKey, apiInvoices)

                    if (isPurchase) {
                        InvoiceRepository.setPurchaseInvoices(apiInvoices)
                    } else {
                        InvoiceRepository.setSalesInvoices(apiInvoices)
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

    private fun combineInvoices(
        apiInvoices: List<Invoice>,
        localInvoices: List<Invoice>
    ): List<Invoice> {
        val result = mutableMapOf<String, Invoice>()

        println("🔄 Combinando facturas:")
        println("   - API: ${apiInvoices.size}")
        println("   - Locales: ${localInvoices.size}")

        localInvoices.forEach { localInvoice ->
            val key = "${localInvoice.series}-${localInvoice.number}"
            result[key] = localInvoice
            println("   ✅ Local: ${key} (ID: ${localInvoice.id}, Estado: ${localInvoice.status})")
        }

        var added = 0
        apiInvoices.forEach { apiInvoice ->
            val key = "${apiInvoice.series}-${apiInvoice.number}"
            if (!result.containsKey(key)) {
                result[key] = apiInvoice
                added++
                println("   ➕ API nueva: ${key}")
            } else {
                println("   🔄 API duplicada: ${key} (preservando local)")
            }
        }

        println("📊 Resultado final: ${result.size} facturas (${localInvoices.size} locales + ${added} nuevas de API)")

        return result.values.sortedBy { invoice ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    private suspend fun parseSunatContent(
        results: List<SunatResult>,
        isPurchase: Boolean
    ): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        var idCounter = 1

        val existingPurchaseInvoices = InvoiceRepository.getPurchaseInvoices()
        val existingSalesInvoices = InvoiceRepository.getSalesInvoices()
        val allExistingInvoices = existingPurchaseInvoices + existingSalesInvoices

        val maxCurrentId = allExistingInvoices.maxOfOrNull { it.id } ?: 0
        idCounter = maxCurrentId + 1

        val invoicesToRegisterInDB = mutableListOf<RegisterInvoiceFromSunatRequest>()

        results.forEach { result ->
            result.content.forEach { item ->
                val documentNumber = "${item.series}-${item.number}"
                var statusFromDB = "CONSULTADO"
                var productsFromDB: List<ProductItem> = emptyList()
                var existingId: Int? = null

                try {
                    val invoiceUI = apiService.getInvoiceForUI(documentNumber)
                    statusFromDB = invoiceUI.invoice.status
                    existingId = invoiceUI.invoice.invoiceId

                    if (invoiceUI.invoice.details != null) {
                        productsFromDB = invoiceUI.invoice.details.map { detail ->
                            ProductItem(
                                description = detail.description,
                                quantity = detail.quantity,
                                unitCost = detail.unitCost,
                                unitOfMeasure = detail.unitOfMeasure
                            )
                        }
                    }
                } catch (e: Exception) {
                    statusFromDB = "CONSULTADO"

                    val respectiveBusinessName = if (isPurchase) {
                        item.issuerBusinessName
                    } else {
                        item.receiverName
                    }

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
                        totalCost = String.format("%.2f", item.taxableBase),
                        igv = String.format("%.2f", item.igv),
                        totalAmount = String.format("%.2f", item.total),
                        userId = 1
                    )
                    invoicesToRegisterInDB.add(invoiceRequest)
                }

                val existingInvoice = allExistingInvoices.firstOrNull { invoice ->
                    invoice.ruc == item.receiverDocNumber &&
                            invoice.series == item.series &&
                            invoice.number == item.number
                }

                val id = existingId ?: (existingInvoice?.id ?: idCounter++)

                val exchangeRate = when {
                    item.exchangeRate != null -> {
                        if (item.currency == "PEN" && item.exchangeRate == 1.0) {
                            ""
                        } else {
                            String.format("%.2f", item.exchangeRate)
                        }
                    }
                    else -> existingInvoice?.exchangeRate ?: ""
                }

                InvoiceRepository.setIssuerRuc(id, item.receiverDocNumber)

                val purchaseBusinessName = item.receiverName

                val invoice = Invoice(
                    id = id,
                    ruc = item.receiverDocNumber,
                    series = item.series,
                    number = item.number,
                    issueDate = item.issueDate,
                    businessName = purchaseBusinessName,
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
                    totalCost = String.format("%.2f", item.taxableBase),
                    igv = String.format("%.2f", item.igv),
                    totalAmount = String.format("%.2f", item.total),
                    status = existingInvoice?.status ?: statusFromDB,
                    isSelected = existingInvoice?.isSelected ?: false,
                    products = existingInvoice?.products ?: productsFromDB,
                    year = existingInvoice?.year ?: item.period.take(4),
                    exchangeRate = exchangeRate
                )
                invoices.add(invoice)
            }
        }

        if (invoicesToRegisterInDB.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    invoicesToRegisterInDB.forEach { request ->
                        try {
                            apiService.registerInvoiceFromSunat(request)
                        } catch (e: Exception) {
                            _errorMessage.value = "Error al registrar algunas facturas: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error al registrar facturas en BD: ${e.message}"
                }
            }
        }

        return invoices.sortedBy { invoice ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    fun getIssuerRuc(invoiceId: Int): String? = InvoiceRepository.getIssuerRuc(invoiceId)

    fun loadInvoiceDetailXmlWithUser(
        invoiceId: Int,
        isPurchase: Boolean,
        issuerRuc: String,
        context: Context,
        onLoadingComplete: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        val myRuc = SunatPrefs.getRuc(context)
        val user = SunatPrefs.getUser(context)
        val solPassword = SunatPrefs.getSolPassword(context)

        if (myRuc == null || user == null || solPassword == null) {
            _errorMessage.value = "Complete sus credenciales SUNAT primero"
            onLoadingComplete(false, "Complete sus credenciales SUNAT primero")
            return
        }

        val invoice = if (isPurchase) {
            InvoiceRepository.getPurchaseInvoices().firstOrNull { it.id == invoiceId }
        } else {
            InvoiceRepository.getSalesInvoices().firstOrNull { it.id == invoiceId }
        }

        if (invoice == null) {
            _errorMessage.value = "Factura no encontrada"
            onLoadingComplete(false, "Factura no encontrada")
            return
        }

        if (invoice.status == "CON DETALLE" || invoice.status == "REGISTRADO") {
            if (invoice.products.isNotEmpty()) {
                onLoadingComplete(true, "Detalles ya cargados")
            } else {
                onLoadingComplete(false, "No hay detalles disponibles")
            }
            return
        }

        if (invoice.status == "EN PROCESO") {
            onLoadingComplete(false, "Ya se está procesando esta factura")
            return
        }

        updateInvoiceStatus(invoiceId, "EN PROCESO", isPurchase)

        ScrapingManager.startScrapingWithQueue(
            viewModelScope = viewModelScope,
            invoiceId = invoiceId,
            isPurchase = isPurchase,
            issuerRuc = issuerRuc,
            invoice = invoice,
            context = context,
            onStatusUpdated = { id, status ->
                updateInvoiceStatus(id, status, isPurchase)
            },
            onProductsUpdated = { id, products, isPurchase ->
                updateInvoiceProducts(id, products, isPurchase)
            },
            onError = { error ->
                _errorMessage.value = error
                onLoadingComplete(false, error)
            },
            onJobQueued = { jobId ->
                onLoadingComplete(true, "Scraping encolado. Job ID: $jobId")
            }
        )
    }

    fun updateInvoiceStatus(invoiceId: Int, newStatus: String, isPurchase: Boolean) {
        viewModelScope.launch {
            if (isPurchase) {
                InvoiceRepository.updatePurchaseInvoices { list ->
                    list.map { invoice ->
                        if (invoice.id == invoiceId) {
                            InvoiceRepository.updateInvoiceInAllCaches(invoice, newStatus)
                            invoice.copy(status = newStatus)
                        } else {
                            invoice
                        }
                    }
                }
            } else {
                InvoiceRepository.updateSalesInvoices { list ->
                    list.map { invoice ->
                        if (invoice.id == invoiceId) {
                            InvoiceRepository.updateInvoiceInAllCaches(invoice, newStatus)
                            invoice.copy(status = newStatus)
                        } else {
                            invoice
                        }
                    }
                }
            }
        }
    }

    private fun updateInvoiceProducts(
        invoiceId: Int,
        products: List<ProductItem>,
        isPurchase: Boolean
    ) {
        viewModelScope.launch {
            if (isPurchase) {
                InvoiceRepository.updatePurchaseInvoices { list ->
                    list.map { invoice ->
                        if (invoice.id == invoiceId) {
                            invoice.copy(products = products)
                        } else {
                            invoice
                        }
                    }
                }
            } else {
                InvoiceRepository.updateSalesInvoices { list ->
                    list.map { invoice ->
                        if (invoice.id == invoiceId) {
                            invoice.copy(products = products)
                        } else {
                            invoice
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInvoices() {
        viewModelScope.launch {
            InvoiceRepository.clearAll()
        }
    }

    suspend fun validateSunatCredentials(
        ruc: String,
        user: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): Boolean {
        return try {
            println("🌐 [VALIDACIÓN] Enviando petición a /sunat/validar-credenciales")
            println("📦 [VALIDACIÓN] Datos: ruc=$ruc, usuario=$user, claveSol=****")

            val response = apiService.validateCredentials(
                ValidateCredentialsRequest(
                    ruc = ruc,
                    user = user,
                    solPassword = solPassword,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            )

            println("✅ [VALIDACIÓN] Respuesta recibida:")
            println("   - valid: ${response.valid}")
            println("   - message: ${response.message}")
            println("   - token: ${response.token}")

            response.valid
        } catch (e: Exception) {
            println("❌ [VALIDACIÓN] Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}