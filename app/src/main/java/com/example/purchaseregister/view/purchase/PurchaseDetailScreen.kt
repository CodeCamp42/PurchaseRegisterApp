package com.example.purchaseregister.view.purchase

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import com.example.purchaseregister.view.components.InvoiceLoadingDialog
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
import com.example.purchaseregister.view.components.ForgotPasswordDialog
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import com.example.purchaseregister.viewmodel.Section
import com.example.purchaseregister.viewmodel.SessionState
import java.util.Calendar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    viewModel: InvoiceListViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (DetailRoute) -> Unit
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

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
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsStateWithLifecycle()
    val salesInvoices by viewModel.salesInvoices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val invoicesWithActiveTimer by viewModel.invoicesWithActiveTimer.collectAsStateWithLifecycle()
    val isDetailingAll by viewModel.isDetailingAll.collectAsStateWithLifecycle()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    val loadingStatus by viewModel.loadingStatus.collectAsStateWithLifecycle()
    val loadingDebugInfo by viewModel.loadingDebugInfo.collectAsStateWithLifecycle()

    var isAppLoggedIn by remember { mutableStateOf(SessionPrefs.isLoggedIn(context)) }
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsStateWithLifecycle()

    var hasSunatCredentials by remember {
        mutableStateOf(
            SunatPrefs.getRuc(context) != null &&
                    SunatPrefs.getUser(context) != null &&
                    SunatPrefs.getSolPassword(context) != null &&
                    SunatPrefs.getClientId(context) != null &&
                    SunatPrefs.getClientSecret(context) != null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.checkSession(context)
    }

    fun loadInitialData() {
        viewModel.loadInvoicesFromDB(sectionActive == Section.PURCHASES)
        isListVisible = true

        when {
            !hasSunatCredentials -> {
                consultAfterLogin = true
                showCredentialsDialog = true
            }
            else -> {
                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                viewModel.loadInvoicesFromAPI(periodStart, periodEnd, sectionActive == Section.PURCHASES, context)
            }
        }
        isInitialLoadDone = true
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Invalid -> {
                showProfileDialog = true
                isInitialLoadDone = false
            }
            is SessionState.Valid -> {
                if (!isInitialLoadDone) {
                    loadInitialData()
                }
            }
            else -> {}
        }
    }

    // Efecto para auto-registro de facturas
    LaunchedEffect(purchaseInvoices, salesInvoices, invoicesWithActiveTimer) {
        viewModel.handleAutoRegisterInvoices(
            purchaseInvoices = purchaseInvoices,
            salesInvoices = salesInvoices,
            invoicesWithActiveTimer = invoicesWithActiveTimer,
            context = context
        )
    }

    // Efecto para mostrar errores
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Calcular lista filtrada
    val filteredList = remember(
        sectionActive, isListVisible, selectedStartMillis, selectedEndMillis,
        purchaseInvoices, salesInvoices
    ) {
        val baseList = if (sectionActive == Section.PURCHASES) purchaseInvoices else salesInvoices
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

    val hasInvoicesInProcess = filteredList.any { it.status == "EN PROCESO" }

    Scaffold(
        topBar = {
            PurchaseTopBar(
                onProfileClick = { showProfileDialog = true },
                isLogoutEnabled = isAppLoggedIn,
                onLogoutClick = {
                    if (isAppLoggedIn) {
                        showLogoutDialog = true
                    } else {
                        Toast.makeText(context, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
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

            DateFilterSection(
                selectedStartMillis = selectedStartMillis,
                selectedEndMillis = selectedEndMillis,
                onDateRangeClick = { showCustomDatePicker = true },
                onDetailAllClick = {
                    if (filteredList.isEmpty()) {
                        Toast.makeText(context, "No hay facturas en lista para detallar", Toast.LENGTH_SHORT).show()
                        return@DateFilterSection
                    }

                    val processableInvoices = filteredList.filter { invoice ->
                        invoice.status !in setOf("CON DETALLE", "REGISTRADO", "EN PROCESO")
                    }

                    if (processableInvoices.isEmpty()) {
                        Toast.makeText(context, "Todas las facturas ya tienen detalle o están en proceso", Toast.LENGTH_SHORT).show()
                        return@DateFilterSection
                    }

                    val ruc = SunatPrefs.getRuc(context)
                    val user = SunatPrefs.getUser(context)
                    val solPassword = SunatPrefs.getSolPassword(context)

                    if (ruc == null || user == null || solPassword == null) {
                        Toast.makeText(context, "⚠️ Primero configure sus credenciales SUNAT", Toast.LENGTH_LONG).show()
                        return@DateFilterSection
                    }

                    viewModel.detailAllInvoices(context, processableInvoices, sectionActive)
                },
                hasInvoicesInProcess = hasInvoicesInProcess,
                isDetailingAll = isDetailingAll
            )

            Spacer(modifier = Modifier.height(15.dp))
            StatusLegend()
            Spacer(modifier = Modifier.height(10.dp))

            InvoiceTable(
                invoices = filteredList,
                sectionActive = sectionActive,
                isListVisible = isListVisible,
                onInvoiceClick = { invoice, isPurchase ->
                    if (invoice.status == "CON DETALLE" || invoice.status == "REGISTRADO") {
                        onNavigateToDetail(DetailRoute(invoice.id, isPurchase))
                    } else {
                        val issuerRuc = viewModel.getIssuerRuc(invoice.id) ?: invoice.ruc
                        viewModel.loadInvoiceDetailXmlWithUser(
                            invoiceId = invoice.id,
                            isPurchase = isPurchase,
                            issuerRuc = issuerRuc,
                            context = context
                        ) { success, message ->
                            if (success) {
                                onNavigateToDetail(DetailRoute(invoice.id, isPurchase))
                            } else {
                                Toast.makeText(context, message ?: "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            BottomActionButtons(
                isAppLoggedIn = isAppLoggedIn,
                hasSunatCredentials = hasSunatCredentials,
                onConsultClick = {
                    val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                    val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                    viewModel.loadInvoicesFromAPI(periodStart, periodEnd, sectionActive == Section.PURCHASES, context)
                    isListVisible = true
                },
                onShowProfile = { showProfileDialog = true },
                onShowCredentials = { showCredentialsDialog = true },
                sectionActive = sectionActive,
                onNavigateToRegister = onNavigateToRegister
            )
        }
    }

    // Credenciales de sunat
    if (showCredentialsDialog) {
        SunatCredentialsDialog(
            onDismiss = {
                showCredentialsDialog = false
                consultAfterLogin = false
            },
            onCredentialsSaved = {
                hasSunatCredentials = true
                clientIdInput = ""
                clientSecretInput = ""
            },
            onShowTutorial = { showTutorial = true },
            externalClientId = clientIdInput,
            externalClientSecret = clientSecretInput,
            onExternalCredentialsUpdated = {
            },
            consultAfterLogin = consultAfterLogin,
            onConsultAfterLogin = {
                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                viewModel.loadInvoicesFromAPI(periodStart, periodEnd, sectionActive == Section.PURCHASES, context)
                isListVisible = true
                consultAfterLogin = false
            }
        )
    }

    // Logout
    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                SessionPrefs.clearSession(context)
                isAppLoggedIn = false
                hasSunatCredentials = false
                viewModel.clearInvoices()
                isListVisible = false
                showLogoutDialog = false
                consultAfterLogin = false
                showProfileDialog = true
                Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
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

    if (showLoadingDialog) {
        InvoiceLoadingDialog(
            isLoading = true,
            statusMessage = loadingStatus,
            debugInfo = loadingDebugInfo,
            onDismiss = { viewModel.clearLoadingDialog() }
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
                if (!hasSunatCredentials) {
                    showCredentialsDialog = true
                } else {
                    val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                    val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)
                    viewModel.loadInvoicesFromAPI(periodStart, periodEnd, sectionActive == Section.PURCHASES, context)
                    isListVisible = true
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
}

fun convertDateToPeriod(millis: Long): String {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    return "${year}${String.format("%02d", month)}"
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