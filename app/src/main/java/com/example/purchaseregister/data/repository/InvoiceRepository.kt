package com.example.purchaseregister.data.repository

import android.content.Context
import com.example.purchaseregister.api.request.UpdateSunatCredentialsRequest
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import kotlinx.coroutines.flow.StateFlow
import com.example.purchaseregister.api.responses.AuthResponse
import com.example.purchaseregister.api.responses.DownloadDocumentResponse
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse

interface InvoiceRepository {

    // Flows para observar los datos
    val purchaseInvoices: StateFlow<List<Invoice>>
    val salesInvoices: StateFlow<List<Invoice>>

    // Carga de datos
    suspend fun loadInvoicesFromAPI(
        periodStart: String,
        periodEnd: String,
        isPurchase: Boolean,
        ruc: String,
        solUser: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): List<Invoice>

    suspend fun checkInvoiceStatus(
        invoiceId: String
    ): Result<InvoiceDetailsResponse>

    suspend fun registerInvoicesInDatabase(
        invoices: List<Invoice>,
        isPurchase: Boolean
    ): Result<Unit>

    suspend fun registerNewPurchaseInvoice(invoiceData: Map<String, Any>): Invoice?

    // Actualizaciones locales
    suspend fun updateInvoiceStatus(invoiceId: String, newStatus: String, isPurchase: Boolean)
    suspend fun updateInvoiceProducts(
        invoiceId: String,
        products: List<ProductItem>,
        isPurchase: Boolean
    )

    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun register(name: String, email: String, password: String): Result<AuthResponse>

    suspend fun requestPasswordReset(email: String): Result<Unit>

    suspend fun signOut(token: String): Result<Unit>

    suspend fun sendFcmToken(context: Context, token: String): Result<Unit>

    suspend fun getInvoiceDetails(invoiceId: String): Result<InvoiceDetailsResponse>

    // Getters auxiliares
    fun getIssuerRuc(invoiceId: String): String?
    fun clearAll()

    // Validación
    suspend fun validateSunatCredentials(
        ruc: String,
        solUser: String,
        solPassword: String,
        clientId: String,
        clientSecret: String
    ): Result<Boolean>

    suspend fun updateSunatCredentials(
        request: UpdateSunatCredentialsRequest
    ): Result<Unit>

    suspend fun exportInvoicesToCsv(
        startDate: String,
        endDate: String,
        context: Context
    ): Result<Unit>

    suspend fun downloadSunatDocument(
        invoiceId: String,
        fileId: Int,
        context: Context
    ): Result<DownloadDocumentResponse>
}