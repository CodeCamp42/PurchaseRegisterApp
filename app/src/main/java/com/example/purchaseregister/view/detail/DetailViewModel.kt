package com.example.purchaseregister.view.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse
import com.example.purchaseregister.data.repository.InvoiceRepository
import com.example.purchaseregister.data.repository.InvoiceRepositoryImpl
import com.example.purchaseregister.utils.DownloadManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {

    private val repository: InvoiceRepository = InvoiceRepositoryImpl()

    // Estados para los detalles de la factura
    private val _invoiceDetails = MutableStateFlow<InvoiceDetailsResponse?>(null)
    val invoiceDetails: StateFlow<InvoiceDetailsResponse?> = _invoiceDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Estados para descarga de documentos
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadingDocument = MutableStateFlow<String?>(null)
    val downloadingDocument: StateFlow<String?> = _downloadingDocument.asStateFlow()

    // Función para cargar detalles de la factura
    fun loadInvoiceDetails(invoiceId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getInvoiceDetails(invoiceId)
                result.fold(
                    onSuccess = { response ->
                        _invoiceDetails.value = response
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Función para limpiar datos
    fun clearDetails() {
        _invoiceDetails.value = null
        _errorMessage.value = null
    }

    // Funciones de descarga
    fun downloadDocument(
        context: Context,
        documentNumber: String,
        type: String,
        baseUrl: String = "http://192.168.1.85:3043",
        onStart: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadingDocument.value = "${documentNumber}-${type}"
            onStart()

            DownloadManagerHelper.downloadDocument(
                context = context,
                documentNumber = documentNumber,
                type = type,
                baseUrl = baseUrl,
                onEnqueued = { downloadId ->
                    _isDownloading.value = false
                    _downloadingDocument.value = null
                    onSuccess()
                },
                onError = { error ->
                    _isDownloading.value = false
                    _downloadingDocument.value = null
                    _errorMessage.value = error
                    onError(error)
                }
            )
        }
    }
}