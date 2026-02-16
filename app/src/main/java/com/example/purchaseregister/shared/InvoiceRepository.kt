package com.example.purchaseregister.viewmodel.shared

import com.example.purchaseregister.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object InvoiceRepository {
    private val _purchaseInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val purchaseInvoices: StateFlow<List<Invoice>> = _purchaseInvoices.asStateFlow()

    private val _salesInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val salesInvoices: StateFlow<List<Invoice>> = _salesInvoices.asStateFlow()

    private val _issuerRucs = mutableMapOf<Int, String>()
    private val _invoicesCache = mutableMapOf<String, List<Invoice>>()

    // ✅ METHODS TO ACCESS/MODIFY
    fun getPurchaseInvoices(): List<Invoice> = _purchaseInvoices.value
    fun getSalesInvoices(): List<Invoice> = _salesInvoices.value

    fun updatePurchaseInvoices(update: (List<Invoice>) -> List<Invoice>) {
        _purchaseInvoices.update { update(it) }
    }

    fun updateSalesInvoices(update: (List<Invoice>) -> List<Invoice>) {
        _salesInvoices.update { update(it) }
    }

    fun setPurchaseInvoices(invoices: List<Invoice>) {
        _purchaseInvoices.value = invoices
    }

    fun setSalesInvoices(invoices: List<Invoice>) {
        _salesInvoices.value = invoices
    }

    // Issuer RUCs
    fun setIssuerRuc(invoiceId: Int, ruc: String) {
        _issuerRucs[invoiceId] = ruc
    }

    fun getIssuerRuc(invoiceId: Int): String? = _issuerRucs[invoiceId]

    // Cache
    fun getCacheKey(isPurchase: Boolean, periodStart: String): String {
        return "${if (isPurchase) "COMPRAS" else "VENTAS"}-${periodStart}"
    }

    fun getCachedInvoices(key: String): List<Invoice>? = _invoicesCache[key]

    fun updateCache(key: String, invoices: List<Invoice>) {
        _invoicesCache[key] = invoices
    }

    fun updateInvoiceInAllCaches(originalInvoice: Invoice, newStatus: String) {
        _invoicesCache.forEach { (key, cachedInvoices) ->
            val updatedInvoices = cachedInvoices.map { cachedInvoice ->
                if (cachedInvoice.ruc == originalInvoice.ruc &&
                    cachedInvoice.series == originalInvoice.series &&
                    cachedInvoice.number == originalInvoice.number) {
                    cachedInvoice.copy(status = newStatus)
                } else {
                    cachedInvoice
                }
            }
            _invoicesCache[key] = updatedInvoices
        }
    }

    fun clearAll() {
        _purchaseInvoices.value = emptyList()
        _salesInvoices.value = emptyList()
        _invoicesCache.clear()
        _issuerRucs.clear()
    }
}