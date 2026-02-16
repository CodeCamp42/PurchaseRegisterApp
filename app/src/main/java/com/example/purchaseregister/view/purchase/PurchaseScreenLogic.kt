package com.example.purchaseregister.view.purchase

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.*
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun convertDateToPeriod(millis: Long): String {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    return "${year}${String.format("%02d", month)}"
}

@Composable
fun calculateFilteredList(
    sectionActive: Section,
    isListVisible: Boolean,
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    hasLoadedSunatData: Boolean,
    purchaseInvoices: List<Invoice>,
    salesInvoices: List<Invoice>,
    todayMillis: Long
): List<Invoice> {
    return remember(
        sectionActive,
        isListVisible,
        selectedStartMillis,
        selectedEndMillis,
        hasLoadedSunatData,
        purchaseInvoices,
        salesInvoices
    ) {
        derivedStateOf {
            if (!hasLoadedSunatData) return@derivedStateOf emptyList()
            if (!isListVisible) return@derivedStateOf emptyList()

            val start = selectedStartMillis ?: todayMillis
            val end = selectedEndMillis ?: start

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                timeZone = PERU_TIME_ZONE
            }

            val currentBaseList = if (sectionActive == Section.PURCHASES) purchaseInvoices else salesInvoices

            val filteredInvoices = currentBaseList.filter { invoice ->
                try {
                    val invoiceDateMillis = sdf.parse(invoice.issueDate)?.time ?: 0L
                    invoiceDateMillis in start..end
                } catch (e: Exception) {
                    false
                }
            }

            filteredInvoices.sortedByDescending { invoice ->
                try {
                    sdf.parse(invoice.issueDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }.value
    }
}

@Composable
fun handleAutoRegisterInvoices(
    purchaseInvoices: List<Invoice>,
    salesInvoices: List<Invoice>,
    invoicesWithActiveTimer: Set<Int>,
    purchaseViewModel: PurchaseViewModel,
    invoiceViewModel: InvoiceViewModel,
    context: Context,
    onTimerUpdate: (Set<Int>) -> Unit
) {
    LaunchedEffect(purchaseInvoices, salesInvoices) {
        val allInvoices = purchaseInvoices + salesInvoices

        val invoicesToAutoRegister = allInvoices.filter { invoice ->
            invoice.status == "CON DETALLE" && !invoicesWithActiveTimer.contains(invoice.id)
        }

        invoicesToAutoRegister.forEach { invoice ->
            val newTimers = invoicesWithActiveTimer + invoice.id
            onTimerUpdate(newTimers)

            launch {
                delay(10000L)

                val currentStatus = allInvoices.firstOrNull { it.id == invoice.id }?.status

                if (currentStatus == "CON DETALLE") {
                    val isPurchase = purchaseInvoices.any { it.id == invoice.id }
                    val invoicesToRegister = listOf(invoice)

                    invoiceViewModel.registerInvoicesInDatabase(
                        invoices = invoicesToRegister,
                        isPurchase = isPurchase,
                        context = context,
                        showLoading = false
                    )

                    purchaseViewModel.updateInvoiceStatus(
                        invoiceId = invoice.id,
                        newStatus = "REGISTRADO",
                        isPurchase = isPurchase
                    )

                    Toast.makeText(
                        context,
                        "✅ Factura ${invoice.series}-${invoice.number} registrada automáticamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                onTimerUpdate(invoicesWithActiveTimer - invoice.id)
            }
        }

        val invoicesWithDetail = allInvoices.filter { it.status == "CON DETALLE" }.map { it.id }.toSet()
        val timersToClean = invoicesWithActiveTimer.filter { !invoicesWithDetail.contains(it) }
        if (timersToClean.isNotEmpty()) {
            onTimerUpdate(invoicesWithActiveTimer.filter { invoicesWithDetail.contains(it) }.toSet())
        }
    }
}

@Composable
fun setupCommonEffects(
    isLoadingViewModel: Boolean,
    errorMessage: String?,
    showLoadingDialog: Boolean,
    loadingInvoiceId: Int?,
    isLoadingPurchase: Boolean,
    viewModel: PurchaseViewModel,
    onIsLoadingChange: (Boolean) -> Unit,
    onLoadingDialogChange: (Boolean) -> Unit,
    onNavigateToDetail: (Int, Boolean) -> Unit,
    onLoadingStatusChange: (String) -> Unit,
    onLoadingDebugInfoChange: (String?) -> Unit,
    onLoadingInvoiceIdChange: (Int?) -> Unit
) {
    LaunchedEffect(isLoadingViewModel) {
        onIsLoadingChange(isLoadingViewModel)

        if (!isLoadingViewModel && showLoadingDialog) {
            Handler(Looper.getMainLooper()).postDelayed({
                onLoadingDialogChange(false)
                onLoadingDebugInfoChange(null)

                loadingInvoiceId?.let { id ->
                    onNavigateToDetail(id, isLoadingPurchase)
                }
                onLoadingInvoiceIdChange(null)
            }, 1500)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            if (showLoadingDialog) {
                onLoadingStatusChange("Error: $message")
                Handler(Looper.getMainLooper()).postDelayed({
                    onLoadingDialogChange(false)
                    onLoadingDebugInfoChange(null)
                    onLoadingInvoiceIdChange(null)
                    viewModel.clearError()
                }, 3000)
            }
        }
    }
}