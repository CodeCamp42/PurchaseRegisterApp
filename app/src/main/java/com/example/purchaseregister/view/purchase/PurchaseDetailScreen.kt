package com.example.purchaseregister.view.purchase

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel  // ← AGREGAR ESTE IMPORT PARA EL PREVIEW
import com.example.purchaseregister.components.CustomDatePickerDialog
import com.example.purchaseregister.components.InvoiceLoadingDialog
import com.example.purchaseregister.components.StatusLegend
import com.example.purchaseregister.components.TutorialSunatDialog
import com.example.purchaseregister.navigation.DetailRoute
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.viewmodel.InvoiceListViewModel
import com.example.purchaseregister.viewmodel.Section
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    // Estados de UI locales
    var sectionActive by remember { mutableStateOf(Section.PURCHASES) }
    var isListVisible by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Fechas
    val todayMillis = remember { getTodayMillisPeru() }
    val firstDayOfMonth = remember { getFirstDayOfMonthPeru(todayMillis) }
    val lastDayOfMonth = remember { getLastDayOfMonthPeru(todayMillis) }

    var selectedStartMillis by remember { mutableStateOf<Long?>(firstDayOfMonth) }
    var selectedEndMillis by remember { mutableStateOf<Long?>(lastDayOfMonth) }

    // Estados para diálogos de credenciales
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var consultAfterLogin by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    // Estados de entrada de credenciales
    var rucInput by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    var solPasswordInput by remember { mutableStateOf("") }
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
    val loadingInvoiceId by viewModel.loadingInvoiceId.collectAsStateWithLifecycle()

    val hasActiveSession = remember {
        SunatPrefs.getRuc(context) != null &&
                SunatPrefs.getUser(context) != null &&
                SunatPrefs.getSolPassword(context) != null
    }

    // Carga inicial
    LaunchedEffect(Unit) {
        // Cargar facturas de BD al inicio
        viewModel.loadInvoicesFromDB(sectionActive == Section.PURCHASES)
        isListVisible = true

        // Auto-consultar si hay credenciales guardadas
        val ruc = SunatPrefs.getRuc(context)
        val user = SunatPrefs.getUser(context)
        val solPassword = SunatPrefs.getSolPassword(context)
        val clientId = SunatPrefs.getClientId(context)
        val clientSecret = SunatPrefs.getClientSecret(context)

        if (ruc != null && user != null && solPassword != null &&
            clientId != null && clientSecret != null
        ) {
            // Pequeña pausa para que se vea la carga inicial
            delay(500)

            val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
            val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

            viewModel.loadInvoicesFromAPI(
                periodStart = periodStart,
                periodEnd = periodEnd,
                isPurchase = sectionActive == Section.PURCHASES,
                context = context
            )
        } else {
            // Si no hay credenciales, mostrar diálogo de login
            consultAfterLogin = true
            showCredentialsDialog = true
        }
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
        sectionActive,
        isListVisible,
        selectedStartMillis,
        selectedEndMillis,
        purchaseInvoices,
        salesInvoices
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

    // Diálogo de credenciales
    if (showCredentialsDialog) {
        var isValidating by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }
        var clientSecretVisible by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isValidating) showCredentialsDialog = false },
            title = { Text("Credenciales SUNAT") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Complete para continuar:")
                    OutlinedTextField(
                        value = rucInput,
                        onValueChange = {
                            rucInput = it.filter { char -> char.isDigit() }.take(11)
                        },
                        label = { Text("RUC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = localError != null,
                        supportingText = { Text("${rucInput.length}/11 dígitos") }
                    )
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it.uppercase().take(8) },
                        label = { Text("Usuario SOL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("${userInput.length}/8 caracteres") }
                    )
                    OutlinedTextField(
                        value = solPasswordInput,
                        onValueChange = { solPasswordInput = it.take(12) },
                        label = { Text("Clave SOL") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        label = { Text("Client ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientSecretInput,
                        onValueChange = { clientSecretInput = it },
                        label = { Text("Client Secret") },
                        visualTransformation = if (clientSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { clientSecretVisible = !clientSecretVisible }) {
                                Icon(
                                    if (clientSecretVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    TextButton(
                        onClick = { showTutorial = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1FB8B9),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "¿Cómo obtener Client ID y Client Secret?",
                            color = Color(0xFF1FB8B9),
                            fontSize = 13.sp
                        )
                    }
                    if (isValidating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1FB8B9)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Validando credenciales...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    localError?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (rucInput.length != 11) {
                                localError = "El RUC debe tener 11 dígitos"
                                return@launch
                            }
                            if (solPasswordInput.isEmpty()) {
                                localError = "La clave SOL no puede estar vacía"
                                return@launch
                            }
                            if (clientIdInput.isEmpty()) {
                                localError = "El Client ID no puede estar vacío"
                                return@launch
                            }
                            if (clientSecretInput.isEmpty()) {
                                localError = "El Client Secret no puede estar vacío"
                                return@launch
                            }

                            isValidating = true
                            localError = null

                            viewModel.validateSunatCredentials(
                                ruc = rucInput,
                                user = userInput,
                                solPassword = solPasswordInput,
                                clientId = clientIdInput,
                                clientSecret = clientSecretInput
                            ) { isValid ->
                                isValidating = false
                                if (isValid) {
                                    SunatPrefs.saveRuc(context, rucInput)
                                    SunatPrefs.saveUser(context, userInput)
                                    SunatPrefs.saveSolPassword(context, solPasswordInput)
                                    SunatPrefs.saveClientId(context, clientIdInput)
                                    SunatPrefs.saveClientSecret(context, clientSecretInput)

                                    if (consultAfterLogin) {
                                        val periodStart =
                                            convertDateToPeriod(selectedStartMillis ?: todayMillis)
                                        val periodEnd =
                                            convertDateToPeriod(selectedEndMillis ?: todayMillis)
                                        viewModel.loadInvoicesFromAPI(
                                            periodStart, periodEnd,
                                            sectionActive == Section.PURCHASES,
                                            context
                                        )
                                        isListVisible = true
                                        consultAfterLogin = false
                                    }

                                    showCredentialsDialog = false
                                    Toast.makeText(
                                        context,
                                        "✅ Credenciales guardadas",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    localError = "Credenciales incorrectas"
                                }
                            }
                        }
                    },
                    enabled = !isValidating &&
                            rucInput.length == 11 &&
                            userInput.isNotEmpty() &&
                            solPasswordInput.isNotEmpty() &&
                            clientIdInput.isNotEmpty() &&
                            clientSecretInput.isNotEmpty()
                ) {
                    Text(if (isValidating) "Validando..." else "Validar y Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isValidating) showCredentialsDialog = false },
                    enabled = !isValidating
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        SunatPrefs.clearCredentials(context)
                        viewModel.clearInvoices()
                        isListVisible = false
                        showLogoutDialog = false
                        consultAfterLogin = true
                        showCredentialsDialog = true
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Selector de fechas
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

    // Tutorial
    if (showTutorial) {
        TutorialSunatDialog(
            onDismiss = { showTutorial = false },
            onCompleted = { clientId, clientSecret ->
                clientIdInput = clientId
                clientSecretInput = clientSecret
                showTutorial = false
                Toast.makeText(context, "✅ Credenciales copiadas", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Loading Dialog
    if (showLoadingDialog) {
        InvoiceLoadingDialog(
            isLoading = true,
            statusMessage = loadingStatus,
            debugInfo = loadingDebugInfo,
            onDismiss = {
                viewModel.clearLoadingDialog()
            }
        )
    }

    // Scaffold ahora está aquí
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Registro Contable",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = { showLogoutDialog = true },
                    enabled = hasActiveSession,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = "Cerrar sesión",
                        tint = if (hasActiveSession) Color(0xFF1FB8B9) else Color.Gray
                    )
                }
            }
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

            // Botones de sección (Compras/Ventas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { sectionActive = Section.PURCHASES },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sectionActive == Section.PURCHASES) Color(0xFFFF5A00) else Color.Gray
                    )
                ) {
                    Text("Compras", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = { sectionActive = Section.SALES },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sectionActive == Section.SALES) Color(0xFFFF5A00) else Color.Gray
                    )
                ) {
                    Text("Ventas", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Selector de fechas y botón "Detallar todo"
            DateFilterSection(
                selectedStartMillis = selectedStartMillis,
                selectedEndMillis = selectedEndMillis,
                onDateRangeClick = { showCustomDatePicker = true },
                onDetailAllClick = {
                    val processable = filteredList.filter {
                        it.status !in setOf(
                            "CON DETALLE",
                            "REGISTRADO",
                            "EN PROCESO"
                        )
                    }
                    viewModel.detailAllInvoices(context, processable, sectionActive)
                },
                isDetailAllEnabled = filteredList.isNotEmpty() && !hasInvoicesInProcess,
                isDetailingAll = isDetailingAll
            )

            Spacer(modifier = Modifier.height(15.dp))

            StatusLegend()

            Spacer(modifier = Modifier.height(10.dp))

            // Tabla de facturas (nuevo componente)
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
                                Toast.makeText(
                                    context,
                                    message ?: "Error al cargar detalle",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botones inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val ruc = SunatPrefs.getRuc(context)
                        val user = SunatPrefs.getUser(context)
                        val solPassword = SunatPrefs.getSolPassword(context)
                        val clientId = SunatPrefs.getClientId(context)
                        val clientSecret = SunatPrefs.getClientSecret(context)

                        if (ruc == null || user == null || solPassword == null ||
                            clientId == null || clientSecret == null
                        ) {
                            consultAfterLogin = true
                            showCredentialsDialog = true
                            return@Button
                        }

                        val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                        val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

                        viewModel.loadInvoicesFromAPI(
                            periodStart, periodEnd,
                            sectionActive == Section.PURCHASES,
                            context
                        )
                        isListVisible = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
                ) {
                    Text("Consultar")
                }

                if (sectionActive == Section.PURCHASES) {
                    Button(
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
                    ) {
                        Text("Subir Factura")
                    }
                }
            }
        }
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