package com.example.purchaseregister.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.api.responses.AuthResponse
import com.example.purchaseregister.data.repository.InvoiceRepository
import com.example.purchaseregister.data.repository.InvoiceRepositoryImpl
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.utils.SessionPrefs
import com.example.purchaseregister.utils.TokenPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.example.purchaseregister.model.ProductItem
import java.util.Locale
import com.google.firebase.messaging.FirebaseMessaging
import com.example.purchaseregister.api.request.UpdateSunatCredentialsRequest

class InvoiceListViewModel : ViewModel() {

    // Dependencia del repositorio
    private val repository: InvoiceRepository = InvoiceRepositoryImpl()

    // Estados observables desde la UI
    val purchaseInvoices: StateFlow<List<Invoice>> = repository.purchaseInvoices
    val salesInvoices: StateFlow<List<Invoice>> = repository.salesInvoices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _registrationCompleted = MutableStateFlow(false)
    val registrationCompleted: StateFlow<Boolean> = _registrationCompleted.asStateFlow()

    // Estados específicos de la pantalla de lista
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

    // Funciones de interacción con facturas
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
                                // Verificar si tiene detalles usando invoiceDetails
                                if (response.invoiceDetails.isNotEmpty()) {
                                    val products = response.invoiceDetails.map { detail ->
                                        ProductItem(
                                            description = detail.description ?: "",
                                            quantity = detail.quantity ?: "0",
                                            unitCost = detail.unitCost ?: "0",
                                            unitOfMeasure = detail.unitOfMeasureCode ?: ""
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
        startDate: String,
        endDate: String,
        context: Context,
        onResult: (Boolean, String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.exportInvoicesToCsv(startDate, endDate, context)
                result.fold(
                    onSuccess = {
                        onResult(true, "Archivo guardado en: $it", null)
                    },
                    onFailure = { exception ->
                        onResult(false, null, exception.message)
                    }
                )
            } catch (e: Exception) {
                onResult(false, null, e.message)
            } finally {
                _isLoading.value = false
            }
        }
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
                        // Guardar el token
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
                // Si no hay token, solo limpiamos localmente
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

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun applyFilters(businessName: String?, ruc: String?, status: String?, isPurchase: Boolean) {
        val sourceList = if (isPurchase) purchaseInvoices.value else salesInvoices.value

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
        } else {
            _filteredSalesInvoices.value = filtered
        }

        isFilterActive.value = (businessName != null || ruc != null || status != null)
    }

    fun clearFilters(isPurchase: Boolean) {
        if (isPurchase) {
            _filteredPurchaseInvoices.value = purchaseInvoices.value
        } else {
            _filteredSalesInvoices.value = salesInvoices.value
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