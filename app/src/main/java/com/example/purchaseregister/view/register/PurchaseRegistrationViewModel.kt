package com.example.purchaseregister.view.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.data.repository.InvoiceRepository
import com.example.purchaseregister.data.repository.InvoiceRepositoryImpl
import com.example.purchaseregister.model.ProductItem
import kotlinx.coroutines.launch

class PurchaseRegistrationViewModel : ViewModel() {

    // Usar la interfaz, no la implementación concreta directamente (ideal para testeo)
    private val repository: InvoiceRepository = InvoiceRepositoryImpl()

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
            val invoiceData = mapOf(
                "ruc" to ruc,
                "businessName" to businessName,
                "series" to series,
                "number" to number,
                "issueDate" to issueDate,
                "documentType" to documentType,
                "currency" to currency,
                "totalCost" to totalCost,
                "igv" to igv,
                "totalAmount" to totalAmount,
                "year" to year,
                "exchangeRate" to exchangeRate,
                "products" to products
            )

            val newInvoice = repository.registerNewPurchaseInvoice(invoiceData)

            newInvoice?.let {
                println("✅ Factura registrada con ID: ${it.id}")
            } ?: run {
                println("❌ Error al registrar factura")
            }
        }
    }
}