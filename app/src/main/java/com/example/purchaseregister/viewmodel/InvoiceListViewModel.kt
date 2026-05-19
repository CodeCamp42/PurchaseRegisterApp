package com.example.purchaseregister.viewmodel

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.api.RetrofitClient.sunatApiService
import com.example.purchaseregister.api.responses.AuthResponse
import com.example.purchaseregister.data.repository.InvoiceRepository
import com.example.purchaseregister.data.repository.InvoiceRepositoryImpl
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.utils.SessionPrefs
import com.example.purchaseregister.utils.TokenPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.purchaseregister.model.ProductItem
import com.google.firebase.messaging.FirebaseMessaging
import com.example.purchaseregister.api.request.UpdateSunatCredentialsRequest
import java.io.File

class InvoiceListViewModel : ViewModel() {

    private val repository: InvoiceRepository = InvoiceRepositoryImpl()

    val purchaseInvoices: StateFlow<List<Invoice>> = repository.purchaseInvoices
    val salesInvoices: StateFlow<List<Invoice>> = repository.salesInvoices

    private val _allPurchaseInvoices = mutableStateOf<List<Invoice>>(emptyList())
    private val _allSalesInvoices = mutableStateOf<List<Invoice>>(emptyList())

    private val _paginatedPurchaseInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val paginatedPurchaseInvoices: StateFlow<List<Invoice>> = _paginatedPurchaseInvoices.asStateFlow()

    private val _paginatedSalesInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val paginatedSalesInvoices: StateFlow<List<Invoice>> = _paginatedSalesInvoices.asStateFlow()

    private var currentPurchasePage = 0
    private var currentSalesPage = 0
    private val pageSize = 20

    private var hasMorePurchaseInvoices = true
    private var hasMoreSalesInvoices = true

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _registrationCompleted = MutableStateFlow(false)
    val registrationCompleted: StateFlow<Boolean> = _registrationCompleted.asStateFlow()

    private val _isDetailingAll = MutableStateFlow(false)
    val isDetailingAll: StateFlow<Boolean> = _isDetailingAll.asStateFlow()

    private val _loadingStatus = MutableStateFlow("Obteniendo detalle de factura...")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog.asStateFlow()

    private val _loadingInvoiceId = MutableStateFlow<Int?>(null)
    val loadingInvoiceId: StateFlow<Int?> = _loadingInvoiceId.asStateFlow()

    private val _loadingDebugInfo = MutableStateFlow<String?>(null)
    val loadingDebugInfo: StateFlow<String?> = _loadingDebugInfo.asStateFlow()

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    private val _filteredPurchaseInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val filteredPurchaseInvoices: StateFlow<List<Invoice>> = _filteredPurchaseInvoices.asStateFlow()

    private val _filteredSalesInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val filteredSalesInvoices: StateFlow<List<Invoice>> = _filteredSalesInvoices.asStateFlow()

    var isFilterActive = mutableStateOf(false)

    fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val ruc = SunatPrefs.getRuc(context)
            val solUser = SunatPrefs.getSolUser(context)
            val solPassword = SunatPrefs.getSolPassword(context)
            val clientId = SunatPrefs.getClientId(context)
            val clientSecret = SunatPrefs.getClientSecret(context)

            if (ruc == null || solUser == null || solPassword == null ||
                clientId == null || clientSecret == null
            ) {
                _errorMessage.value = "Credenciales no configuradas"
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null
            resetPagination(isPurchase)

            try {
                repository.loadInvoicesFromAPI(
                    periodStart,
                    periodEnd,
                    isPurchase,
                    ruc,
                    solUser,
                    solPassword,
                    clientId,
                    clientSecret
                )
                delay(500)

                if (isPurchase) {
                    val allInvoices = purchaseInvoices.value
                    _allPurchaseInvoices.value = allInvoices

                    val firstPage = allInvoices.take(pageSize)
                    _paginatedPurchaseInvoices.value = firstPage
                    currentPurchasePage = 0
                    hasMorePurchaseInvoices = allInvoices.size > pageSize
                } else {
                    val allInvoices = salesInvoices.value
                    _allSalesInvoices.value = allInvoices

                    val firstPage = allInvoices.take(pageSize)
                    _paginatedSalesInvoices.value = firstPage
                    currentSalesPage = 0
                    hasMoreSalesInvoices = allInvoices.size > pageSize
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error al conectar con SUNAT"
                if (errorMsg.contains("No fue posible autenticar con SUNAT SIRE") ||
                    errorMsg.contains("CREDENCIALES_INVALIDAS") ||
                    errorMsg.contains("401")
                ) {
                    _errorMessage.value = "CREDENTIAL_ERROR: $errorMsg"
                } else {
                    _errorMessage.value = errorMsg
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreInvoices(isPurchase: Boolean) {
        if (_isLoadingMore.value) return

        if (isPurchase) {
            if (!hasMorePurchaseInvoices) return

            val startIndex = (currentPurchasePage + 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, _allPurchaseInvoices.value.size)

            if (startIndex < _allPurchaseInvoices.value.size) {
                _isLoadingMore.value = true

                viewModelScope.launch {
                    delay(300)

                    val currentList = _paginatedPurchaseInvoices.value.toMutableList()
                    val newItems = _allPurchaseInvoices.value.subList(startIndex, endIndex)
                    currentList.addAll(newItems)
                    _paginatedPurchaseInvoices.value = currentList

                    currentPurchasePage++
                    hasMorePurchaseInvoices = endIndex < _allPurchaseInvoices.value.size

                    _isLoadingMore.value = false
                }
            } else {
                hasMorePurchaseInvoices = false
            }
        } else {
            if (!hasMoreSalesInvoices) return

            val startIndex = (currentSalesPage + 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, _allSalesInvoices.value.size)

            if (startIndex < _allSalesInvoices.value.size) {
                _isLoadingMore.value = true

                viewModelScope.launch {
                    delay(300)

                    val currentList = _paginatedSalesInvoices.value.toMutableList()
                    val newItems = _allSalesInvoices.value.subList(startIndex, endIndex)
                    currentList.addAll(newItems)
                    _paginatedSalesInvoices.value = currentList

                    currentSalesPage++
                    hasMoreSalesInvoices = endIndex < _allSalesInvoices.value.size

                    _isLoadingMore.value = false
                }
            } else {
                hasMoreSalesInvoices = false
            }
        }
    }

    fun resetPagination(isPurchase: Boolean) {
        if (isPurchase) {
            _paginatedPurchaseInvoices.value = emptyList()
            _allPurchaseInvoices.value = emptyList()
            currentPurchasePage = 0
            hasMorePurchaseInvoices = true
        } else {
            _paginatedSalesInvoices.value = emptyList()
            _allSalesInvoices.value = emptyList()
            currentSalesPage = 0
            hasMoreSalesInvoices = true
        }
        _isLoadingMore.value = false
    }

    fun getInvoiceDetails(
        invoiceId: Int,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getInvoiceDetails(invoiceId)
                result.fold(
                    onSuccess = { response ->
                        onResult(true, null)
                    },
                    onFailure = { exception ->
                        onResult(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkInvoiceStatus(
        invoiceId: Int,
        isPurchase: Boolean,
        context: Context,
        onResult: (success: Boolean, shouldNavigate: Boolean, message: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = repository.checkInvoiceStatus(invoiceId)

                result.fold(
                    onSuccess = { response ->
                        when (response.invoiceStatus) {
                            "PROCESSING" -> {
                                onResult(
                                    true,
                                    false,
                                    "⏳ La factura está en proceso de obtención de detalles"
                                )
                            }

                            "COMPLETED", "WITH_DETAILS" -> {
                                if (response.invoiceDetails.isNotEmpty()) {
                                    val products = response.invoiceDetails.map { detail ->
                                        ProductItem(
                                            description = detail.description ?: "",
                                            quantity = detail.quantity ?: "0",
                                            unitCost = detail.unitPrice ?: "0",
                                            unitOfMeasure = detail.unitOfMeasure ?: ""
                                        )
                                    }

                                    viewModelScope.launch {
                                        repository.updateInvoiceProducts(
                                            invoiceId,
                                            products,
                                            isPurchase
                                        )
                                        repository.updateInvoiceStatus(
                                            invoiceId,
                                            "CON DETALLE",
                                            isPurchase
                                        )
                                    }

                                    onResult(
                                        true,
                                        true,
                                        null
                                    )
                                } else {
                                    onResult(
                                        true,
                                        false,
                                        "Factura sin detalles disponibles"
                                    )
                                }
                            }

                            else -> {
                                onResult(
                                    false,
                                    false,
                                    "Estado desconocido: ${response.invoiceStatus}"
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        onResult(
                            false,
                            false,
                            "Error: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                onResult(
                    false,
                    false,
                    "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun exportInvoices(
        invoicesToExport: List<Invoice>? = null,  // Nuevo parámetro opcional
        startDate: String,
        endDate: String,
        context: Context,
        onResult: (Boolean, String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (invoicesToExport != null) {
                    val result = exportSpecificInvoicesToCsv(invoicesToExport, startDate, endDate, context)
                    result.fold(
                        onSuccess = { filePath ->
                            onResult(true, filePath, null)
                        },
                        onFailure = { exception ->
                            onResult(false, null, exception.message)
                        }
                    )
                } else {
                    val result = repository.exportInvoicesToCsv(startDate, endDate, context)
                    result.fold(
                        onSuccess = {
                            onResult(true, "Archivo guardado en Descargas", null)
                        },
                        onFailure = { exception ->
                            onResult(false, null, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                onResult(false, null, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun exportSpecificInvoicesToCsv(
        invoices: List<Invoice>,
        startDate: String,
        endDate: String,
        context: Context
    ): Result<String> {
        return try {
            val fileName = "facturas_${startDate}_${endDate}.csv"
            val csvContent = generateCsvFromInvoices(invoices)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    Result.success("Descargas/$fileName")
                } ?: Result.failure(Exception("No se pudo crear el archivo"))
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val file = File(downloadsDir, fileName)
                file.writeText(csvContent)
                Result.success("Descargas/$fileName")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateCsvFromInvoices(invoices: List<Invoice>): String {
        val headers = listOf(
            "RUC", "Razón Social", "Serie", "Número", "Fecha Emisión",
            "Tipo Documento", "Moneda", "Costo Total", "IGV", "Monto Total",
            "Estado", "Productos"
        )

        val csvString = StringBuilder()
        csvString.append(headers.joinToString(",")).append("\n")

        invoices.forEach { invoice ->
            val productsStr = invoice.products.joinToString(";") { product ->
                "${product.description}|${product.quantity}|${product.unitCost}|${product.unitOfMeasure}"
            }

            val row = listOf(
                escapeCsv(invoice.ruc),
                escapeCsv(invoice.businessName),
                escapeCsv(invoice.series),
                escapeCsv(invoice.number),
                escapeCsv(invoice.issueDate),
                escapeCsv(invoice.documentType),
                escapeCsv(invoice.currency),
                escapeCsv(invoice.totalCost),
                escapeCsv(invoice.igv),
                escapeCsv(invoice.totalAmount),
                escapeCsv(invoice.invoiceStatus),
                escapeCsv(productsStr)
            )
            csvString.append(row.joinToString(",")).append("\n")
        }

        return csvString.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    fun registerInvoicesInDatabase(
        invoices: List<Invoice>,
        isPurchase: Boolean,
        context: Context,
        showLoading: Boolean = true
    ) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoading.value = true
            }
            _errorMessage.value = null
            _registrationCompleted.value = false

            try {
                val result = repository.registerInvoicesInDatabase(invoices, isPurchase)

                if (result.isSuccess) {
                    invoices.forEach { invoice ->
                        updateInvoiceStatus(invoice.id, "REGISTRADO", isPurchase)
                    }
                    _registrationCompleted.value = true
                } else {
                    _errorMessage.value = "Algunas facturas no se pudieron registrar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión al registrar en BD: ${e.message}"
            } finally {
                if (showLoading) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateInvoiceStatus(invoiceId: Int, newStatus: String, isPurchase: Boolean) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, newStatus, isPurchase)
        }
    }

    fun clearInvoices() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun register(name: String, email: String, password: String, context: Context) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            val result = repository.register(name, email, password)

            result.fold(
                onSuccess = { response ->
                    if (response.user != null && response.token != null) {
                        SessionPrefs.saveSession(
                            context,
                            email,
                            name
                        )
                        TokenPrefs.saveToken(context, response.token)
                        getAndSendFcmToken(context)
                        _registerState.value = AuthState.Success(response)
                    } else {
                        _registerState.value = AuthState.Error(
                            response.message ?: "Error en registro"
                        )
                    }
                },
                onFailure = { exception ->
                    _registerState.value = AuthState.Error(exception.message ?: "Error de conexión")
                }
            )
        }
    }

    fun login(email: String, password: String, context: Context) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = repository.login(email, password)

            result.fold(
                onSuccess = { response ->
                    if (response.user != null && response.token != null) {
                        SessionPrefs.saveSession(
                            context,
                            email,
                            response.user.name ?: "Usuario"
                        )
                        TokenPrefs.saveToken(context, response.token)
                        getAndSendFcmToken(context)
                        _loginState.value = AuthState.Success(response)
                    } else {
                        _loginState.value = AuthState.Error(
                            response.message ?: "Error en autenticación"
                        )
                    }
                },
                onFailure = { exception ->
                    _loginState.value = AuthState.Error(exception.message ?: "Error de conexión")
                }
            )
        }
    }

    private fun getAndSendFcmToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                sendTokenToBackend(context, token)
            }
        }
    }

    private fun sendTokenToBackend(context: Context, token: String) {
        viewModelScope.launch {
            try {
                val result = repository.sendFcmToken(context, token)
                if (result.isSuccess) {
                    println("✅ Token FCM enviado correctamente: $token")
                } else {
                    println("❌ Error al enviar token: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("❌ Excepción al enviar token: ${e.message}")
            }
        }
    }

    fun saveSunatCredentials(
        ruc: String,
        solUser: String,
        solPassword: String,
        clientId: String,
        clientSecret: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.validateSunatCredentials(
                ruc, solUser, solPassword, clientId, clientSecret
            )

            result.fold(
                onSuccess = {
                    onResult(true, null)
                },
                onFailure = { exception ->
                    onResult(false, exception.message)
                }
            )
        }
    }

    fun updateSunatCredentials(
        ruc: String?,
        solUser: String?,
        solPassword: String?,
        clientId: String?,
        clientSecret: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateSunatCredentialsRequest(
                    ruc = ruc.takeIf { it?.isNotEmpty() == true },
                    solUser = solUser.takeIf { it?.isNotEmpty() == true },
                    solPassword = solPassword.takeIf { it?.isNotEmpty() == true },
                    clientId = clientId.takeIf { it?.isNotEmpty() == true },
                    clientSecret = clientSecret.takeIf { it?.isNotEmpty() == true }
                )

                val result = repository.updateSunatCredentials(request)
                result.fold(
                    onSuccess = {
                        onResult(true, null)
                    },
                    onFailure = { exception ->
                        onResult(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(
        context: Context,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        viewModelScope.launch {
            val token = TokenPrefs.getToken(context)
            if (token == null) {
                performLocalLogout(context)
                onComplete(true, null)
                return@launch
            }

            val result = repository.signOut(token)

            result.fold(
                onSuccess = {
                    performLocalLogout(context)
                    onComplete(true, null)
                },
                onFailure = { exception ->
                    performLocalLogout(context)
                    onComplete(false, exception.message)
                }
            )
        }
    }

    private fun performLocalLogout(context: Context) {
        SessionPrefs.clearSession(context)
        TokenPrefs.clearToken(context)
        clearInvoices()
    }

    fun resetAuthStates() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
    }

    fun requestPasswordReset(email: String, context: Context) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading

            try {
                val result = repository.requestPasswordReset(email)

                result.fold(
                    onSuccess = { response ->
                        _forgotPasswordState.value = ForgotPasswordState.Success(
                            "Se ha enviado un enlace a tu correo electrónico"
                        )
                    },
                    onFailure = { exception ->
                        _forgotPasswordState.value = ForgotPasswordState.Error(
                            exception.message ?: "Error al enviar el correo"
                        )
                    }
                )
            } catch (e: Exception) {
                _forgotPasswordState.value = ForgotPasswordState.Error(
                    e.message ?: "Error de conexión"
                )
            }
        }
    }

    fun loadSunatCredentialsFromBackend(context: Context) {
        viewModelScope.launch {
            try {
                val token = TokenPrefs.getToken(context)
                if (token.isNullOrEmpty()) {
                    return@launch
                }
                val response = sunatApiService.getSunatCredentials()

                if (response.isSuccessful && response.body() != null) {
                    val creds = response.body()!!

                    SunatPrefs.saveRuc(context, creds.ruc)
                    SunatPrefs.saveSolUser(context, creds.solUser)
                    SunatPrefs.saveSolPassword(context, creds.solPassword)
                    SunatPrefs.saveClientId(context, creds.clientId)
                    SunatPrefs.saveClientSecret(context, creds.clientSecret)
                } else {
                    when (response.code()) {
                        401 -> println("   - Error 401: Token inválido o expirado")
                        404 -> println("   - Error 404: Endpoint no encontrado")
                        500 -> println("   - Error 500: Error interno del servidor")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun applyFilters(businessName: String?, ruc: String?, status: String?, isPurchase: Boolean) {
        val sourceList = if (isPurchase) _allPurchaseInvoices.value else _allSalesInvoices.value

        val filtered = sourceList.filter { invoice ->
            (businessName == null || invoice.businessName.contains(
                businessName,
                ignoreCase = true
            )) &&
                    (ruc == null || invoice.ruc.contains(ruc)) &&
                    (status == null || invoice.invoiceStatus == status)
        }

        if (isPurchase) {
            _filteredPurchaseInvoices.value = filtered
            val firstPage = filtered.take(pageSize)
            _paginatedPurchaseInvoices.value = firstPage
            currentPurchasePage = 0
            hasMorePurchaseInvoices = filtered.size > pageSize
        } else {
            _filteredSalesInvoices.value = filtered
            val firstPage = filtered.take(pageSize)
            _paginatedSalesInvoices.value = firstPage
            currentSalesPage = 0
            hasMoreSalesInvoices = filtered.size > pageSize
        }

        isFilterActive.value = (businessName != null || ruc != null || status != null)
    }

    fun clearFilters(isPurchase: Boolean) {
        if (isPurchase) {
            _filteredPurchaseInvoices.value = _allPurchaseInvoices.value
        } else {
            _filteredSalesInvoices.value = _allSalesInvoices.value
        }
        isFilterActive.value = false
    }
}

enum class Section { PURCHASES, SALES }

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}