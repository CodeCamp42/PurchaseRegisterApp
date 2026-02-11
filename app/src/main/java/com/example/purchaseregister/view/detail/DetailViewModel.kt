package com.example.purchaseregister.view.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.purchaseregister.utils.DownloadManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadingDocument = MutableStateFlow<String?>(null)
    val downloadingDocument: StateFlow<String?> = _downloadingDocument.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    fun descargarDocumento(
        context: Context,
        numeroComprobante: String,
        tipo: String,
        baseUrl: String = "http://192.168.1.85:3043",
        onStart: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadingDocument.value = "${numeroComprobante}-${tipo}"
            onStart()

            DownloadManagerHelper.descargarDocumento(
                context = context,
                numeroComprobante = numeroComprobante,
                tipo = tipo,
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