package com.example.purchaseregister.viewmodel.shared

import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import com.example.purchaseregister.model.Invoice

object ScrapingManager {
    private val apiService = RetrofitClient.sunatApiService

    fun startScrapingWithQueue(
        viewModelScope: CoroutineScope,
        invoiceId: Int,
        isPurchase: Boolean,
        issuerRuc: String,
        invoice: Invoice,
        context: Context,
        onStatusUpdated: (Int, String) -> Unit,
        onProductsUpdated: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit,
        onJobQueued: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val myRuc = SunatPrefs.getRuc(context) ?: return@launch
                val user = SunatPrefs.getUser(context) ?: return@launch
                val solPassword = SunatPrefs.getSolPassword(context) ?: return@launch

                val request = InvoiceDetailRequest(
                    issuerRuc = issuerRuc,
                    series = invoice.series,
                    number = invoice.number,
                    ruc = if (isPurchase) myRuc else invoice.ruc,
                    solUser = user,
                    solPassword = solPassword
                )

                val queuedResponse = apiService.downloadXmlWithQueue(request)

                if (queuedResponse.success) {
                    val jobId = queuedResponse.jobId
                    onJobQueued(jobId)

                    startPollingJob(
                        viewModelScope = viewModelScope,
                        jobId = jobId,
                        invoiceId = invoiceId,
                        isPurchase = isPurchase,
                        onStatusUpdated = onStatusUpdated,
                        onProductsUpdated = onProductsUpdated,
                        onError = onError
                    )
                } else {
                    onStatusUpdated(invoiceId, "CONSULTADO")
                    onError("Error al encolar trabajo: ${queuedResponse.message}")
                }
            } catch (e: Exception) {
                onStatusUpdated(invoiceId, "CONSULTADO")
                onError("Error: ${e.message}")
            }
        }
    }

    private fun startPollingJob(
        viewModelScope: CoroutineScope,
        jobId: String,
        invoiceId: Int,
        isPurchase: Boolean,
        onStatusUpdated: (Int, String) -> Unit,
        onProductsUpdated: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                delay(3000)

                try {
                    val jobStatus = apiService.getJobStatus(jobId)

                    when (jobStatus.state) {
                        "completed" -> {
                            val products = jobStatus.result?.items?.map { item ->
                                ProductItem(
                                    description = item.description,
                                    quantity = item.quantity.toString(),
                                    unitCost = String.format("%.2f", item.unitValue),
                                    unitOfMeasure = item.unit
                                )
                            } ?: emptyList()

                            onProductsUpdated(invoiceId, products, isPurchase)
                            onStatusUpdated(invoiceId, "CON DETALLE")
                            saveProductsInBackend(jobStatus.result?.id ?: "sin-id", products)
                            break
                        }
                        "failed" -> {
                            onStatusUpdated(invoiceId, "CONSULTADO")
                            onError("Scraping falló: ${jobStatus.reason}")
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Ignorar
                }

                attempts++
            }

            if (attempts >= maxAttempts) {
                onStatusUpdated(invoiceId, "CONSULTADO")
                onError("Timeout: El scraping no se completó")
            }
        }
    }

    private suspend fun saveProductsInBackend(
        documentNumber: String,
        products: List<ProductItem>
    ) {
        try {
            val productsToSave = products.map { product ->
                ProductRequest(
                    description = product.description,
                    quantity = product.quantity.toDoubleOrNull() ?: 0.0,
                    unitCost = product.unitCost.toDoubleOrNull() ?: 0.0,
                    unitOfMeasure = product.unitOfMeasure
                )
            }

            apiService.saveInvoiceProducts(
                documentNumber,
                SaveProductsRequest(products = productsToSave)
            )

            val scrapingRequest = ScrapingCompletedRequest(
                products = productsToSave
            )

            apiService.markScrapingCompleted(documentNumber, scrapingRequest)
        } catch (e: Exception) {
            // Silencioso
        }
    }
}