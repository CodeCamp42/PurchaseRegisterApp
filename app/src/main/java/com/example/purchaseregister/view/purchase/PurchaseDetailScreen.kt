package com.example.purchaseregister.view.purchase

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purchaseregister.view.components.CustomDatePickerDialog
import com.example.purchaseregister.view.components.ProcessingInfoDialog
import androidx.compose.runtime.rememberCoroutineScope
import com.example.purchaseregister.view.components.ProfileDialog
import com.example.purchaseregister.view.components.StatusLegend
import com.example.purchaseregister.view.components.TutorialSunatDialog
import com.example.purchaseregister.view.components.SunatCredentialsDialog
import com.example.purchaseregister.view.components.LogoutDialog
import com.example.purchaseregister.view.components.PurchaseTopBar
import com.example.purchaseregister.view.components.SectionButtons
import com.example.purchaseregister.view.components.BottomActionButtons
import com.example.purchaseregister.view.detail.DetailRoute
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.view.components.CredentialErrorDialog
import com.example.purchaseregister.view.components.DetailsReadyDialog
import com.example.purchaseregister.view.components.EditCredentialsDialog
import com.example.purchaseregister.view.components.FilterDialog
import com.example.purchaseregister.view.components.ForgotPasswordDialog
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import com.example.purchaseregister.viewmodel.Section
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    viewModel: InvoiceListViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (DetailRoute) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados de UI locales
    var sectionActive by rememberSaveable { mutableStateOf(Section.PURCHASES) }
    var isListVisible by rememberSaveable { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Fechas
    val todayMillis = remember { getTodayMillisPeru() }
    val firstDayOfMonth = remember { getFirstDayOfMonthPeru(todayMillis) }
    val lastDayOfMonth = remember { getLastDayOfMonthPeru(todayMillis) }

    var selectedStartMillis by rememberSaveable { mutableStateOf<Long?>(firstDayOfMonth) }
    var selectedEndMillis by rememberSaveable { mutableStateOf<Long?>(lastDayOfMonth) }

    // Estados para diálogos
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var consultAfterLogin by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var isInitialLoadDone by rememberSaveable { mutableStateOf(false) }

    // Estados de entrada de credenciales SUNAT
    var clientIdInput by remember { mutableStateOf("") }
    var clientSecretInput by remember { mutableStateOf("") }

    // Observar estados del ViewModel
    val purchaseInvoices by viewModel.paginatedPurchaseInvoices.collectAsStateWithLifecycle()
    val salesInvoices by viewModel.paginatedSalesInvoices.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isDetailingAll by viewModel.isDetailingAll.collectAsStateWithLifecycle()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    val loadingStatus by viewModel.loadingStatus.collectAsStateWithLifecycle()
    val loadingDebugInfo by viewModel.loadingDebugInfo.collectAsStateWithLifecycle()

    var isAppLoggedIn by remember { mutableStateOf(SessionPrefs.isLoggedIn(context)) }
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsStateWithLifecycle()
    var showCredentialsForApiError by remember { mutableStateOf(false) }
    var showCredentialErrorDialog by remember { mutableStateOf(false) }
    var credentialErrorMessage by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val filteredPurchaseInvoices by viewModel.filteredPurchaseInvoices.collectAsStateWithLifecycle()
    val filteredSalesInvoices by viewModel.filteredSalesInvoices.collectAsStateWithLifecycle()
    val isFilterActive = viewModel.isFilterActive.value

    var showProcessingDialog by remember { mutableStateOf(false) }
    var pendingInvoiceCount by remember { mutableStateOf(0) }
    var showEditCredentialsDialog by remember { mutableStateOf(false) }
    var currentPeriodKey by rememberSaveable { mutableStateOf("") }
    var dialogShownForCurrentPeriod by rememberSaveable { mutableStateOf(false) }
    var showDetailsReadyDialog by remember { mutableStateOf(false) }
    var readyInvoiceCount by remember { mutableStateOf(0) }
    var totalProcessedInvoices by remember { mutableStateOf(0) }
    var verificationStage by remember { mutableStateOf(0) }
    var lastReportedReadyCount by remember { mutableStateOf(0) }
    var totalPendingToProcess by remember { mutableStateOf(0) }
    var isProcessingComplete by remember { mutableStateOf(false) }
    var hasSunatCredentials by remember { mutableStateOf(false) }

    fun updateCredentialsStatus() {
        hasSunatCredentials = isAppLoggedIn && (
                SunatPrefs.getRuc(context) != null &&
                        SunatPrefs.getSolUser(context) != null &&
                        SunatPrefs.getSolPassword(context) != null &&
                        SunatPrefs.getClientId(context) != null &&
                        SunatPrefs.getClientSecret(context) != null
                )
    }

    val currentPeriodKeyValue = remember(selectedStartMillis, selectedEndMillis) {
        if (selectedStartMillis != null && selectedEndMillis != null) {
            val startCalendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
                timeInMillis = selectedStartMillis!!
            }
            val endCalendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
                timeInMillis = selectedEndMillis!!
            }

            val startYear = startCalendar.get(Calendar.YEAR)
            val startMonth = startCalendar.get(Calendar.MONTH) + 1
            val endYear = endCalendar.get(Calendar.YEAR)
            val endMonth = endCalendar.get(Calendar.MONTH) + 1

            if (startYear == endYear && startMonth == endMonth) {
                "$startYear-${String.format("%02d", startMonth)}"
            } else {
                "${startYear}-${
                    String.format(
                        "%02d",
                        startMonth
                    )
                }_${endYear}-${String.format("%02d", endMonth)}"
            }
        } else {
            ""
        }
    }

    LaunchedEffect(currentPeriodKeyValue) {
        if (currentPeriodKeyValue != currentPeriodKey) {
            currentPeriodKey = currentPeriodKeyValue
            dialogShownForCurrentPeriod = false
        }
    }

    fun checkForPendingInvoices() {
        val pendingCount = (purchaseInvoices + salesInvoices).count {
            it.invoiceStatus == "CONSULTADO"
        }

        if (pendingCount > 0 && !dialogShownForCurrentPeriod && currentPeriodKey.isNotEmpty()) {
            pendingInvoiceCount = pendingCount
            showProcessingDialog = true
            dialogShownForCurrentPeriod = true
        }
    }

    fun checkProgressiveReadyInvoices(stage: Int) {
        val readyCount = (purchaseInvoices + salesInvoices).count {
            it.invoiceStatus == "CON DETALLE" || it.invoiceStatus == "REGISTRADO"
        }

        // Si ya se completaron todas, no mostrar más
        if (readyCount >= totalPendingToProcess) {
            isProcessingComplete = true
            // Mostrar una última vez que ya están todas listas
            readyInvoiceCount = readyCount
            totalProcessedInvoices = totalPendingToProcess
            showDetailsReadyDialog = true
            return
        }

        // Si hay más facturas listas que la última vez, mostrar diálogo
        if (readyCount > lastReportedReadyCount) {
            lastReportedReadyCount = readyCount
            readyInvoiceCount = readyCount
            totalProcessedInvoices = totalPendingToProcess
            showDetailsReadyDialog = true
        }

        // Programar siguiente verificación según la etapa
        when (stage) {
            1 -> {
                // Segunda verificación a los 10 segundos
                coroutineScope.launch {
                    delay(10000)
                    if (!isProcessingComplete) {
                        checkProgressiveReadyInvoices(2)
                    }
                }
            }

            2 -> {
                // Tercera verificación a los 20 segundos
                coroutineScope.launch {
                    delay(20000)
                    if (!isProcessingComplete) {
                        checkProgressiveReadyInvoices(3)
                    }
                }
            }

            3 -> {
                // Última verificación, ya no programamos más
                // Si aún no se completaron, mostramos estado actual
                if (readyCount < totalPendingToProcess && !isProcessingComplete) {
                    readyInvoiceCount = readyCount
                    totalProcessedInvoices = totalPendingToProcess
                    showDetailsReadyDialog = true
                }
            }
        }
    }

    fun executeConsult() {
        val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
        val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
        viewModel.loadInvoicesFromAPI(
            periodStart,
            periodEnd,
            sectionActive == Section.PURCHASES,
            context
        )
        isListVisible = true
    }

    LaunchedEffect(isAppLoggedIn) {
        updateCredentialsStatus()
    }

    // LÓGICA DE CARGA INICIAL
    LaunchedEffect(Unit) {
        if (!isInitialLoadDone) {
            isListVisible = true
            delay(500)

            when {
                !isAppLoggedIn -> {
                    showProfileDialog = true
                }
                isAppLoggedIn -> {
                    viewModel.loadSunatCredentialsFromBackend(context)
                    delay(500)
                    updateCredentialsStatus()

                    val hasCredentials = hasSunatCredentials

                    if (hasCredentials) {
                        executeConsult()
                        delay(1000)
                        checkForPendingInvoices()
                    } else {
                        consultAfterLogin = true
                        showCredentialsDialog = true
                    }
                }
            }
            isInitialLoadDone = true
        }
    }

    LaunchedEffect(purchaseInvoices, salesInvoices, selectedStartMillis, sectionActive) {
        if (isInitialLoadDone) {
            // Solo verificar si hay facturas cargadas
            if (purchaseInvoices.isNotEmpty() || salesInvoices.isNotEmpty()) {
                delay(500)
                checkForPendingInvoices()
            }
        }
    }

    LaunchedEffect(selectedStartMillis, selectedEndMillis, sectionActive) {
        if (isInitialLoadDone && hasSunatCredentials) {
            val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
            val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

            viewModel.loadInvoicesFromAPI(
                periodStart,
                periodEnd,
                sectionActive == Section.PURCHASES,
                context
            )
        }
    }

    // Efecto para mostrar errores
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            if (error.startsWith("CREDENTIAL_ERROR:")) {
                credentialErrorMessage = error.substringAfter("CREDENTIAL_ERROR:").trim()
                showCredentialErrorDialog = true
            } else if (error.contains("CREDENCIALES_INVALIDAS") ||
                error.contains("401") ||
                error.contains("No autorizado")
            ) {
                showCredentialsForApiError = true
                showCredentialsDialog = true
            } else if (error.contains("Credenciales de Sunat no encontradas")) {
                showCredentialsDialog = true
            } else {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Calcular lista filtrada
    val filteredList = remember(
        sectionActive, isListVisible, selectedStartMillis, selectedEndMillis,
        if (isFilterActive) filteredPurchaseInvoices else purchaseInvoices,
        if (isFilterActive) filteredSalesInvoices else salesInvoices,
        isFilterActive
    ) {
        val baseList = if (sectionActive == Section.PURCHASES) {
            if (isFilterActive) filteredPurchaseInvoices else purchaseInvoices
        } else {
            if (isFilterActive) filteredSalesInvoices else salesInvoices
        }
        val start = selectedStartMillis ?: todayMillis
        val end = selectedEndMillis ?: start

        baseList.filter { invoice ->
            try {
                val dateParts = invoice.issueDate.split("/")
                if (dateParts.size == 3) {
                    val day = dateParts[0].toInt()
                    val month = dateParts[1].toInt() - 1
                    val year = dateParts[2].toInt()
                    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val invoiceMillis = calendar.timeInMillis
                    invoiceMillis in start..end
                } else false
            } catch (e: Exception) {
                false
            }
        }.sortedByDescending { it.issueDate }
    }

    // Calcular valores para el botón de descarga
    val totalInvoices = filteredList.size
    val readyInvoices = filteredList.count {
        it.invoiceStatus == "CON DETALLE" || it.invoiceStatus == "REGISTRADO"
    }
    val pendingInvoices = totalInvoices - readyInvoices
    val isDownloadButtonEnabled = pendingInvoices == 0

    val hasInvoicesInProcess = filteredList.any { it.invoiceStatus == "EN PROCESO" }

    Scaffold(
        topBar = {
            PurchaseTopBar(
                onProfileClick = { showProfileDialog = true },
                isLogoutEnabled = isAppLoggedIn,
                onLogoutClick = {
                    if (isAppLoggedIn) {
                        showLogoutDialog = true
                    } else {
                        Toast.makeText(context, "Debes iniciar sesión primero", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionButtons(
                sectionActive = sectionActive,
                onSectionChange = { sectionActive = it }
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .height(40.dp)
                        .width(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Filtrar",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                DateFilterSection(
                    selectedStartMillis = selectedStartMillis,
                    selectedEndMillis = selectedEndMillis,
                    onDateRangeClick = { showCustomDatePicker = true },
                    hasInvoicesInProcess = hasInvoicesInProcess,
                    isDetailingAll = isDetailingAll
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            StatusLegend(
                totalInvoices = totalInvoices,
                readyInvoices = readyInvoices,
                pendingInvoices = pendingInvoices,
                isDownloadButtonEnabled = isDownloadButtonEnabled,
                onDownloadClick = {
                    if (isDownloadButtonEnabled) {
                        val startDate = convertDateToDownloadYYYYMMDD(selectedStartMillis ?: todayMillis)
                        val endDate = convertDateToDownloadYYYYMMDD(selectedEndMillis ?: todayMillis)

                        viewModel.exportInvoices(
                            startDate = startDate,
                            endDate = endDate,
                            context = context
                        ) { success, filePath, error ->
                            if (success) {
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error: ${error ?: "Error desconocido"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Pending details for $pendingInvoices invoices",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            InvoiceTable(
                invoices = filteredList,
                sectionActive = sectionActive,
                isListVisible = isListVisible,
                onInvoiceClick = { invoice, isPurchase ->
                    if (invoice.invoiceStatus == "CON DETALLE" || invoice.invoiceStatus == "REGISTRADO") {
                        viewModel.getInvoiceDetails(
                            invoiceId = invoice.id,
                            onResult = { success, error ->
                                if (success) {
                                    onNavigateToDetail(DetailRoute(invoice.id, isPurchase))
                                } else {
                                    Toast.makeText(
                                        context,
                                        error ?: "Error al obtener detalles",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    } else {
                        viewModel.checkInvoiceStatus(
                            invoiceId = invoice.id,
                            isPurchase = isPurchase,
                            context = context
                        ) { success, shouldNavigate, message ->
                            if (success) {
                                if (shouldNavigate) {
                                    onNavigateToDetail(DetailRoute(invoice.id, isPurchase))
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    message ?: "Error al verificar factura",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                isLoading = isLoading,
                isLoadingMore = isLoadingMore,
                modifier = Modifier.weight(1f),
                onLoadMore = {
                    viewModel.loadMoreInvoices(sectionActive == Section.PURCHASES)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            BottomActionButtons(
                isAppLoggedIn = isAppLoggedIn,
                hasSunatCredentials = hasSunatCredentials,
                hasCredentialError = showCredentialErrorDialog,
                onConsultClick = {
                    val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                    val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                    viewModel.loadInvoicesFromAPI(
                        periodStart,
                        periodEnd,
                        sectionActive == Section.PURCHASES,
                        context
                    )
                    isListVisible = true
                },
                onShowProfile = { showProfileDialog = true },
                onShowCredentials = {
                    if (hasSunatCredentials) {
                        showEditCredentialsDialog = true
                    } else {
                        showCredentialsDialog = true
                    }
                },
                sectionActive = sectionActive,
                onNavigateToRegister = onNavigateToRegister
            )
        }
    }

    // solo si las credenciales son invalidas
    if (showCredentialErrorDialog) {
        CredentialErrorDialog(
            errorMessage = credentialErrorMessage,
            onEditClick = {
                showEditCredentialsDialog = true
                consultAfterLogin = true
            },
            onDismiss = {
                showCredentialErrorDialog = false
            }
        )
    }

    // Editor de credenciales de sunat
    if (showEditCredentialsDialog) {
        EditCredentialsDialog(
            onDismiss = {
                showEditCredentialsDialog = false
            },
            onCredentialsSaved = {
            },
            onShowTutorial = { showTutorial = true },
            externalClientId = clientIdInput,
            externalClientSecret = clientSecretInput,
            onExternalCredentialsUpdated = {
            },
            onSaveToBackend = { ruc, solUser, solPassword, clientId, clientSecret, onResult ->
                viewModel.updateSunatCredentials(
                    ruc = ruc,
                    solUser = solUser,
                    solPassword = solPassword,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    onResult = onResult
                )
            },
            consultAfterLogin = consultAfterLogin,
            onConsultAfterLogin = {
                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                viewModel.loadInvoicesFromAPI(
                    periodStart,
                    periodEnd,
                    sectionActive == Section.PURCHASES,
                    context
                )
                isListVisible = true
                consultAfterLogin = false
                showEditCredentialsDialog = false
            }
        )
    }

    // Credenciales de sunat
    if (showCredentialsDialog) {
        SunatCredentialsDialog(
            onDismiss = {
                showCredentialsDialog = false
                showCredentialsForApiError = false
            },
            onCredentialsSaved = {
                clientIdInput = ""
                clientSecretInput = ""
                showCredentialsForApiError = false

                val tempLoggedIn = isAppLoggedIn
                isAppLoggedIn = false
                isAppLoggedIn = tempLoggedIn

                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                viewModel.loadInvoicesFromAPI(
                    periodStart,
                    periodEnd,
                    sectionActive == Section.PURCHASES,
                    context
                )
            },
            onShowTutorial = { showTutorial = true },
            externalClientId = clientIdInput,
            externalClientSecret = clientSecretInput,
            onExternalCredentialsUpdated = {
            },
            onSaveToBackend = { ruc, solUser, solPassword, clientId, clientSecret, onResult ->
                viewModel.saveSunatCredentials(
                    ruc, solUser = solUser, solPassword, clientId, clientSecret,
                    onResult = onResult
                )
            },
            consultAfterLogin = consultAfterLogin || showCredentialsForApiError,
            onConsultAfterLogin = {
                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                viewModel.loadInvoicesFromAPI(
                    periodStart,
                    periodEnd,
                    sectionActive == Section.PURCHASES,
                    context
                )
                isListVisible = true
                consultAfterLogin = false
                showCredentialsForApiError = false
            }
        )
    }

    // Logout
    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                viewModel.signOut(
                    context,
                    onComplete = { success, message ->
//                        if (!success) {
//                            Toast.makeText(
//                                context,
//                                message ?: "Error al cerrar sesión",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
                        isAppLoggedIn = false
                        isListVisible = false
                        showLogoutDialog = false
                        consultAfterLogin = false
                        showProfileDialog = true
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showCustomDatePicker) {
        CustomDatePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onPeriodSelected = { start, end ->
                selectedStartMillis = start
                selectedEndMillis = end
                showCustomDatePicker = false
            },
            onRangeSelected = { start, end ->
                selectedStartMillis = start
                selectedEndMillis = end
                showCustomDatePicker = false
            },
            initialStartMillis = selectedStartMillis,
            initialEndMillis = selectedEndMillis
        )
    }

    // Tutorial client ID y client secret
    if (showTutorial) {
        TutorialSunatDialog(
            onDismiss = { showTutorial = false },
            onCredentialsObtained = { clientId, clientSecret ->
                clientIdInput = clientId
                clientSecretInput = clientSecret
                showTutorial = false
                showCredentialsDialog = true
                Toast.makeText(context, "✅ Credenciales copiadas", Toast.LENGTH_SHORT).show()
            },
            prefillClientId = clientIdInput,
            prefillClientSecret = clientSecretInput
        )
    }

    if (showProcessingDialog) {
        // Guardar el total de facturas que se están procesando
        totalPendingToProcess = pendingInvoiceCount
        verificationStage = 0
        lastReportedReadyCount = 0
        isProcessingComplete = false

        ProcessingInfoDialog(
            showDialog = showProcessingDialog,
            invoiceCount = pendingInvoiceCount,
            onAccept = {
                showProcessingDialog = false
                executeConsult()

                // Iniciar la primera verificación a los 5 segundos
                coroutineScope.launch {
                    delay(5000)
                    if (!isProcessingComplete) {
                        checkProgressiveReadyInvoices(1)
                    }
                }
            },
            onDismiss = {
                showProcessingDialog = false
                executeConsult()

                // También iniciar si cierra sin aceptar
                coroutineScope.launch {
                    delay(5000)
                    if (!isProcessingComplete) {
                        checkProgressiveReadyInvoices(1)
                    }
                }
            }
        )
    }

    // Inicio de sesion
    if (showProfileDialog) {
        ProfileDialog(
            onDismiss = { showProfileDialog = false },
            onLoginSuccess = {
                Toast.makeText(context, "✅ Sesión iniciada", Toast.LENGTH_SHORT).show()
                showProfileDialog = false
                isAppLoggedIn = true
                viewModel.loadSunatCredentialsFromBackend(context)

                coroutineScope.launch {
                    delay(500)
                    updateCredentialsStatus()
                    val hasCredentials = hasSunatCredentials

                    if (hasCredentials) {
                        val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                        val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                        viewModel.loadInvoicesFromAPI(
                            periodStart,
                            periodEnd,
                            sectionActive == Section.PURCHASES,
                            context
                        )
                        isListVisible = true
                    } else {
                        consultAfterLogin = true
                        showCredentialsDialog = true
                    }
                }
            },
            onRegisterSuccess = {
                Toast.makeText(context, "✅ Registro exitoso", Toast.LENGTH_SHORT).show()
                showProfileDialog = false
                isAppLoggedIn = true
                showCredentialsDialog = true
            },
            onForgotPasswordClick = {
                showProfileDialog = false
                showForgotPasswordDialog = true
            },
            isLoggedIn = isAppLoggedIn,
            currentUsername = SessionPrefs.getCurrentUserName(context),
            currentEmail = SessionPrefs.getCurrentUserEmail(context),
            loginState = viewModel.loginState.collectAsStateWithLifecycle().value,
            registerState = viewModel.registerState.collectAsStateWithLifecycle().value,
            onLogin = { email, password ->
                viewModel.login(email, password, context)
            },
            onRegister = { name, email, password ->
                viewModel.register(name, email, password, context)
            },
            onResetStates = {
                viewModel.resetAuthStates()
            }
        )
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = {
                showForgotPasswordDialog = false
                viewModel.resetForgotPasswordState()
            },
            onBackToLogin = {
                showForgotPasswordDialog = false
                showProfileDialog = true
            },
            forgotPasswordState = forgotPasswordState,
            onSendResetEmail = { email ->
                viewModel.requestPasswordReset(email, context)
            },
            onResetState = {
                viewModel.resetForgotPasswordState()
            }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            onFilterClick = { businessName, ruc, status ->
                viewModel.applyFilters(
                    businessName = businessName,
                    ruc = ruc,
                    status = status,
                    isPurchase = sectionActive == Section.PURCHASES
                )
                showFilterDialog = false
            }
        )
    }

    if (showDetailsReadyDialog) {
        DetailsReadyDialog(
            showDialog = showDetailsReadyDialog,
            totalInvoices = totalProcessedInvoices,
            readyCount = readyInvoiceCount,
            onAccept = {
                showDetailsReadyDialog = false
                executeConsult()
            },
            onDismiss = {
                showDetailsReadyDialog = false
            }
        )
    }
}

fun convertDateToPeriod(millis: Long): String {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${year}-${String.format("%02d", month)}-${String.format("%02d", day)}"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PurchaseDetailScreenPreview() {
    PurchaseDetailScreen(
        viewModel = viewModel(),
        onNavigateToRegister = { },
        onNavigateToDetail = { }
    )
}