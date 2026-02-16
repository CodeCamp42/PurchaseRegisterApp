package com.example.purchaseregister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.viewmodel.shared.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*

class InvoiceViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    val purchaseInvoices: StateFlow<List<Invoice>> = InvoiceRepository.purchaseInvoices
    val salesInvoices: StateFlow<List<Invoice>> = InvoiceRepository.salesInvoices

    private val _isLoading = MutableStateFlow(false)

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _registrationCompleted = MutableStateFlow(false)

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

                val allSuccessful = response.results.all { it.success }

                if (allSuccessful) {
                    invoices.forEach { invoice ->
                        updateInvoiceStatus(invoice.id, "REGISTRADO", isPurchase)
                    }
                    _registrationCompleted.value = true
                } else {
                    val errors = response.results.filter { !it.success }
                    val errorMsg = "Algunas facturas no se pudieron registrar: ${errors.map { it.documentNumber }}"
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = "Error de conexión al registrar en BD: ${e.message}"
                _errorMessage.value = errorMsg
            } finally {
                if (showLoading) {
                    _isLoading.value = false
                }
            }
        }
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
}