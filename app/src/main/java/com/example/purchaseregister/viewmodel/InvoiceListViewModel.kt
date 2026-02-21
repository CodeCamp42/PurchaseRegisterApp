package com.example.purchaseregister.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import java.text.SimpleDateFormat
import java.util.Locale

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
    private val _invoicesWithActiveTimer = MutableStateFlow<Set<Int>>(emptySet())
    val invoicesWithActiveTimer: StateFlow<Set<Int>> = _invoicesWithActiveTimer.asStateFlow()

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

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    fun loadInvoicesFromDB(isPurchase: Boolean) {
        viewModelScope.launch {
            try {
                val dbInvoices = repository.loadInvoicesFromDB(isPurchase)
                // Combinar con las facturas actuales de la API (si las hay)
                val currentApiInvoices = if (isPurchase) purchaseInvoices.value else salesInvoices.value
                val combined = combineInvoices(currentApiInvoices, dbInvoices)
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando de BD: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val ruc = SunatPrefs.getRuc(context)
            val solUsername = SunatPrefs.getSolUsername(context)
            val solPassword = SunatPrefs.getSolPassword(context)
            val clientId = SunatPrefs.getClientId(context)
            val clientSecret = SunatPrefs.getClientSecret(context)

            if (ruc == null || solUsername == null || solPassword == null ||
                clientId == null || clientSecret == null) {
                _errorMessage.value = "Credenciales no configuradas"
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null
            try {
                val apiInvoices = repository.loadInvoicesFromAPI(
                    periodStart, periodEnd, isPurchase, ruc, solUsername, solPassword, clientId, clientSecret
                )
                // El repositorio ya actualiza su propio StateFlow internamente
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con SUNAT: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Funciones de interacción con facturas ---

    fun loadInvoiceDetailXmlWithUser(
        invoiceId: Int,
        isPurchase: Boolean,
        issuerRuc: String,
        context: Context,
        onLoadingComplete: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        val invoice = if (isPurchase) purchaseInvoices.value.firstOrNull { it.id == invoiceId }
        else salesInvoices.value.firstOrNull { it.id == invoiceId }

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

        _showLoadingDialog.value = true
        _loadingStatus.value = "Encolando trabajo de scraping..."
        _loadingInvoiceId.value = invoiceId

        // Actualizar estado a EN PROCESO inmediatamente
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, "EN PROCESO", isPurchase)
        }

        viewModelScope.launch {
            repository.loadInvoiceDetail(
                invoiceId = invoiceId,
                isPurchase = isPurchase,
                issuerRuc = issuerRuc,
                context = context,
                onJobQueued = { jobId ->
                    _loadingStatus.value = "Scraping en cola. Job ID: $jobId"
                    onLoadingComplete(true, "Scraping encolado. Job ID: $jobId")
                },
                onStatusUpdate = { id, status ->
                    viewModelScope.launch {
                        repository.updateInvoiceStatus(id, status, isPurchase)
                        if (status == "CON DETALLE") {
                            startAutoRegisterTimer(id, isPurchase, context)
                        }
                    }
                },
                onProductsUpdate = { id, products, isPur ->
                    viewModelScope.launch {
                        repository.updateInvoiceProducts(id, products, isPur)
                    }
                },
                onError = { error ->
                    _errorMessage.value = error
                    _showLoadingDialog.value = false
                    _loadingInvoiceId.value = null
                    viewModelScope.launch {
                        repository.updateInvoiceStatus(invoiceId, "CONSULTADO", isPurchase)
                    }
                    onLoadingComplete(false, error)
                }
            )
        }
    }

    private fun startAutoRegisterTimer(invoiceId: Int, isPurchase: Boolean, context: Context) {
        viewModelScope.launch {
            _invoicesWithActiveTimer.value = _invoicesWithActiveTimer.value + invoiceId
            delay(10000L) // 10 segundos

            val currentInvoice = if (isPurchase) purchaseInvoices.value.firstOrNull { it.id == invoiceId }
            else salesInvoices.value.firstOrNull { it.id == invoiceId }

            if (currentInvoice?.status == "CON DETALLE") {
                val result = repository.registerInvoicesInDatabase(listOf(currentInvoice), isPurchase)
                if (result.isSuccess) {
                    Toast.makeText(context, "✅ Factura ${currentInvoice.series}-${currentInvoice.number} registrada", Toast.LENGTH_SHORT).show()
                }
            }
            _invoicesWithActiveTimer.value = _invoicesWithActiveTimer.value - invoiceId
        }
    }

    fun handleAutoRegisterInvoices(
        purchaseInvoices: List<Invoice>,
        salesInvoices: List<Invoice>,
        invoicesWithActiveTimer: Set<Int>,
        context: Context,
    ) {
        viewModelScope.launch {
            val allInvoices = purchaseInvoices + salesInvoices

            val invoicesToAutoRegister = allInvoices.filter { invoice ->
                invoice.status == "CON DETALLE" && !invoicesWithActiveTimer.contains(invoice.id)
            }

            invoicesToAutoRegister.forEach { invoice ->
                _invoicesWithActiveTimer.value = _invoicesWithActiveTimer.value + invoice.id

                launch {
                    delay(10000L)

                    val currentStatus = allInvoices.firstOrNull { it.id == invoice.id }?.status

                    if (currentStatus == "CON DETALLE") {
                        val isPurchase = purchaseInvoices.any { it.id == invoice.id }
                        val invoicesToRegister = listOf(invoice)

                        registerInvoicesInDatabase(
                            invoices = invoicesToRegister,
                            isPurchase = isPurchase,
                            context = context,
                            showLoading = false
                        )

                        updateInvoiceStatus(
                            invoiceId = invoice.id,
                            newStatus = "REGISTRADO",
                            isPurchase = isPurchase
                        )

                        Toast.makeText(
                            context,
                            "✅ Factura ${invoice.series}-${invoice.number} registrada automáticamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    _invoicesWithActiveTimer.value = _invoicesWithActiveTimer.value - invoice.id
                }
            }

            val invoicesWithDetail = allInvoices.filter { it.status == "CON DETALLE" }.map { it.id }.toSet()
            val timersToClean = invoicesWithActiveTimer.filter { !invoicesWithDetail.contains(it) }
            if (timersToClean.isNotEmpty()) {
                _invoicesWithActiveTimer.value = _invoicesWithActiveTimer.value.filter {
                    invoicesWithDetail.contains(it)
                }.toSet()
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

    fun detailAllInvoices(
        context: Context,
        invoicesToProcess: List<Invoice>,
        sectionActive: Section
    ) {
        viewModelScope.launch {
            if (invoicesToProcess.isEmpty()) {
                Toast.makeText(context, "No hay facturas en lista para detallar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val processableInvoices = invoicesToProcess.filter { invoice ->
                invoice.status != "CON DETALLE" &&
                        invoice.status != "REGISTRADO" &&
                        invoice.status != "EN PROCESO"
            }

            if (processableInvoices.isEmpty()) {
                Toast.makeText(
                    context,
                    "Todas las facturas ya tienen detalle o están en proceso",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val ruc = SunatPrefs.getRuc(context)
            val solUsername = SunatPrefs.getSolUsername(context)
            val solPassword = SunatPrefs.getSolPassword(context)

            if (ruc == null || solUsername == null || solPassword == null) {
                Toast.makeText(
                    context,
                    "⚠️ Primero configure sus credenciales SUNAT en el botón CONSULTAR",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            _isDetailingAll.value = true
            var successful = 0
            var failed = 0
            val total = processableInvoices.size

            _loadingStatus.value = "Procesando 0/$total facturas..."
            _showLoadingDialog.value = true

            processableInvoices.forEach { invoice ->
                val currentIsPurchase = (sectionActive == Section.PURCHASES)
                val issuerRuc = getIssuerRuc(invoice.id) ?: invoice.ruc

                val result = suspendCoroutine { continuation ->
                    loadInvoiceDetailXmlWithUser(
                        invoiceId = invoice.id,
                        isPurchase = currentIsPurchase,
                        issuerRuc = issuerRuc,
                        context = context
                    ) { success, _ ->
                        continuation.resume(success)
                    }
                }

                if (result) successful++ else failed++

                withContext(Dispatchers.Main) {
                    _loadingStatus.value = "Procesando ${successful + failed}/$total facturas...\n✅ Exitosas: $successful\n❌ Fallidas: $failed"
                }

                delay(300)
            }

            withContext(Dispatchers.Main) {
                _isDetailingAll.value = false
                _showLoadingDialog.value = false
                Toast.makeText(
                    context,
                    "✅ Proceso completado: $successful exitosas, $failed fallidas",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun updateInvoiceStatus(invoiceId: Int, newStatus: String, isPurchase: Boolean) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, newStatus, isPurchase)
        }
    }

    fun getIssuerRuc(invoiceId: Int): String? = repository.getIssuerRuc(invoiceId)

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLoadingDialog() {
        _showLoadingDialog.value = false
        _loadingDebugInfo.value = null
        _loadingInvoiceId.value = null
    }

    private fun combineInvoices(apiInvoices: List<Invoice>, localInvoices: List<Invoice>): List<Invoice> {
        val result = mutableMapOf<String, Invoice>()
        localInvoices.forEach { local ->
            val key = "${local.series}-${local.number}"
            result[key] = local
        }
        apiInvoices.forEach { api ->
            val key = "${api.series}-${api.number}"
            if (!result.containsKey(key)) {
                result[key] = api
            }
        }
        return result.values.sortedBy { inv ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(inv.issueDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    fun clearInvoices() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun validateSunatCredentials(
        ruc: String,
        solUsername: String,
        solPassword: String,
        clientId: String,
        clientSecret: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val isValid = repository.validateSunatCredentials(ruc, solUsername, solPassword, clientId, clientSecret)
            onResult(isValid)
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