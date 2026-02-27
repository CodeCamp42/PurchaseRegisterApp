package com.example.purchaseregister.data.repository

import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.SunatResponse
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.purchaseregister.api.responses.AuthResponse
import com.example.purchaseregister.api.responses.SaveSunatCredentialsResponse
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse
import com.example.purchaseregister.utils.TokenPrefs

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

    override fun getIssuerRuc(invoiceId: Int): String? = _issuerRucs[invoiceId]

    private fun setIssuerRuc(invoiceId: Int, ruc: String) {
        _issuerRucs[invoiceId] = ruc
    }

    fun updateInvoiceInAllCaches(originalInvoice: Invoice, newStatus: String) {
        _invoicesCache.forEach { (key, cachedInvoices) ->
            val updatedInvoices = cachedInvoices.map { cachedInvoice ->
                if (cachedInvoice.ruc == originalInvoice.ruc &&
                    cachedInvoice.series == originalInvoice.series &&
                    cachedInvoice.number == originalInvoice.number
                ) {
                    cachedInvoice.copy(invoiceStatus = newStatus)
                } else {
                    cachedInvoice
                }
            }
            _invoicesCache[key] = updatedInvoices
        }
    }

    override suspend fun sendFcmToken(context: Context, token: String): Result<Unit> {
        return try {
            val authToken = TokenPrefs.getToken(context)

            if (authToken == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val response = apiService.sendFcmToken(
                authorization = "Bearer $authToken",
                request = FcmTokenRequest(token = token)
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Error al enviar token FCM"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        try {
            val response = apiService.getInvoices(
                periodStart,
                periodEnd,
            )

            return if (response.isNotEmpty()) {
                val apiInvoices = parseSunatContent(response, isPurchase)
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

    override suspend fun checkInvoiceStatus(
        invoiceId: Int
    ): Result<InvoiceDetailsResponse> {
        return try {
            val response = apiService.getInvoiceDetails(invoiceId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvoiceDetails(invoiceId: Int): Result<InvoiceDetailsResponse> {
        return try {
            val response = apiService.getInvoiceDetails(invoiceId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    override suspend fun updateInvoiceStatus(
        invoiceId: Int,
        newStatus: String,
        isPurchase: Boolean
    ) {
        if (isPurchase) {
            updatePurchaseInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) {
                        updateInvoiceInAllCaches(invoice, newStatus)
                        invoice.copy(invoiceStatus = newStatus)
                    } else invoice
                }
            }
        } else {
            updateSalesInvoices { list ->
                list.map { invoice ->
                    if (invoice.id == invoiceId) {
                        updateInvoiceInAllCaches(invoice, newStatus)
                        invoice.copy(invoiceStatus = newStatus)
                    } else invoice
                }
            }
        }
    }

    override suspend fun updateInvoiceProducts(
        invoiceId: Int,
        products: List<ProductItem>,
        isPurchase: Boolean
    ) {
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
                    val errorResponse =
                        gson.fromJson(errorBody, SaveSunatCredentialsResponse::class.java)
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

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        items.forEach { item ->
            val id = item.id

            val formattedDate = try {
                val date = isoFormat.parse(item.issueDate)
                targetFormat.format(date)
            } catch (e: Exception) {
                item.issueDate
            }

            // Guardar RUC para consultas de detalle
            setIssuerRuc(id, item.issuerRuc)

            val mappedStatus = when (item.invoiceStatus) {
                "CONSULTED" -> "CONSULTADO"
                "PENDING_DETAILS" -> "EN PROCESO"
                "WITH_DETAILS" -> "CON DETALLE"
                "REGISTERED" -> "REGISTRADO"
                else -> ""
            }

            val invoice = Invoice(
                id = id,
                ruc = if (isPurchase) item.receiverDocNumber else item.issuerRuc,
                businessName = if (isPurchase) item.receiverName else item.issuerName,
                series = item.series,
                number = item.number,
                issueDate = formattedDate,
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
                invoiceStatus = mappedStatus,
                isSelected = false,
                products = emptyList(),
                year = item.period.take(4),
                exchangeRate = item.exchangeRate.toString()
            )
            invoices.add(invoice)
        }

        val sortedInvoices = invoices.sortedBy { invoice ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time
                    ?: 0L
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

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<AuthResponse> {
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
                Result.failure(
                    Exception(
                        response.errorBody()?.string() ?: "Error al enviar el correo"
                    )
                )
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