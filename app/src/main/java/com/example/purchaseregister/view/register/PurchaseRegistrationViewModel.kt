package com.example.purchaseregister.view.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.RegisterInvoiceFromSunatRequest
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.viewmodel.shared.InvoiceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PurchaseRegistrationViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    fun addNewPurchaseInvoice(
        ruc: String,
        businessName: String,
        series: String,
        number: String,
        issueDate: String,
        documentType: String,
        currency: String = "",
        totalCost: String = "",
        igv: String = "",
        totalAmount: String = "",
        year: String = "",
        exchangeRate: String = "",
        products: List<ProductItem> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val request = RegisterInvoiceFromSunatRequest(
                    issuerRuc = ruc,
                    series = series,
                    number = number,
                    issueDate = issueDate,
                    businessName = businessName,
                    documentType = documentType,
                    currency = currency,
                    totalCost = totalCost,
                    igv = igv,
                    totalAmount = totalAmount,
                    userId = 1
                )

                val response = apiService.registerInvoiceFromSunat(request)

                if (response.success && response.invoiceId != null) {
                    val initialStatus = if (products.isNotEmpty()) "CON DETALLE" else "CONSULTADO"

                    InvoiceRepository.updatePurchaseInvoices { list ->
                        val newInvoice = Invoice(
                            id = response.invoiceId!!,
                            ruc = ruc,
                            businessName = businessName,
                            series = series,
                            number = number,
                            issueDate = issueDate,
                            documentType = documentType,
                            year = year,
                            currency = currency,
                            totalCost = totalCost,
                            igv = igv,
                            exchangeRate = exchangeRate,
                            totalAmount = totalAmount,
                            status = initialStatus,
                            isSelected = false,
                            products = products
                        )

                        (list + newInvoice).sortedBy { invoice ->
                            try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(invoice.issueDate)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    }
                } else {
                    println("❌ Error registrando factura en BD: ${response.message}")
                }
            } catch (e: Exception) {
                println("❌ Excepción al registrar factura: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}