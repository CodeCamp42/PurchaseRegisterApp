package com.example.purchaseregister.data.repository

import android.content.Context
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import kotlinx.coroutines.flow.StateFlow
import com.example.purchaseregister.api.responses.AuthResponse

interface InvoiceRepository {
    // Flows para observar los datos
    val purchaseInvoices: StateFlow<List<Invoice>>
    val salesInvoices: StateFlow<List<Invoice>>

    // Carga de datos
    suspend fun loadInvoicesFromDB(isPurchase: Boolean): List<Invoice>
    suspend fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        ruc: String,
        solUsername: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): List<Invoice>

    // Gestión de facturas individuales
    suspend fun loadInvoiceDetail(
        invoiceId: Int,
        isPurchase: Boolean,
        issuerRuc: String,
        context: Context,
        onJobQueued: (String) -> Unit,
        onStatusUpdate: (Int, String) -> Unit,
        onProductsUpdate: (Int, List<ProductItem>, Boolean) -> Unit,
        onError: (String) -> Unit
    )

    suspend fun registerInvoicesInDatabase(
        invoices: List<Invoice>,
        isPurchase: Boolean
    ): Result<Unit>

    suspend fun registerNewPurchaseInvoice(invoiceData: Map<String, Any>): Invoice?

    // Actualizaciones locales
    suspend fun updateInvoiceStatus(invoiceId: Int, newStatus: String, isPurchase: Boolean)
    suspend fun updateInvoiceProducts(invoiceId: Int, products: List<ProductItem>, isPurchase: Boolean)

    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun register(name: String, email: String, password: String): Result<AuthResponse>

    suspend fun requestPasswordReset(email: String): Result<Unit>

    // Getters auxiliares (pueden ser suspend si son lentos, pero aquí son rápidos)
    fun getIssuerRuc(invoiceId: Int): String?
    fun clearAll()

    // Validación
    suspend fun validateSunatCredentials(
        ruc: String,
        solUsername: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): Boolean
}