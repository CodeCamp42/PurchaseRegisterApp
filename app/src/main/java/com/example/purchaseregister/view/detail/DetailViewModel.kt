package com.example.purchaseregister.view.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse
import com.example.purchaseregister.data.repository.InvoiceRepository
import com.example.purchaseregister.data.repository.InvoiceRepositoryImpl
import com.example.purchaseregister.utils.DocumentDownloadHelper
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
        invoiceId: Int,
        documentType: String,
        onStart: () -> Unit = {},
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadingDocument.value = "${invoiceId}-${documentType}"
            onStart()

            try {
                val result = repository.downloadSunatDocument(invoiceId, documentType, context)

                result.fold(
                    onSuccess = { response ->
                        DocumentDownloadHelper.processDownloadResponse(
                            context = context,
                            fileName = response.fileName,
                            fileContentBase64 = response.fileContent,
                            onSuccess = { filePath ->
                                _isDownloading.value = false
                                _downloadingDocument.value = null
                                onSuccess(filePath)
                            },
                            onError = { error ->
                                _isDownloading.value = false
                                _downloadingDocument.value = null
                                onError(error)
                            }
                        )
                    },
                    onFailure = { exception ->
                        _isDownloading.value = false
                        _downloadingDocument.value = null
                        onError(exception.message ?: "Error desconocido")
                    }
                )
            } catch (e: Exception) {
                _isDownloading.value = false
                _downloadingDocument.value = null
                onError(e.message ?: "Error de conexión")
            }
        }
    }
}