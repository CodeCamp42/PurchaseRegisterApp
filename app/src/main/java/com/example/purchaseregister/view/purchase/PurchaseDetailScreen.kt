package com.example.purchaseregister.view.purchase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.navigation.DetailRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.components.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PowerSettingsNew

enum class Section { PURCHASES, SALES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    purchaseViewModel: PurchaseViewModel,
    invoiceViewModel: InvoiceViewModel,
    onPurchasesClick: () -> Unit,
    onSalesClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (DetailRoute) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados
    var sectionActive by remember { mutableStateOf(Section.PURCHASES) }
    var isListVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Fechas
    val todayMillis = remember { getTodayMillisPeru() }
    val firstDayOfMonth = remember { getFirstDayOfMonthPeru(todayMillis) }
    val lastDayOfMonth = remember { getLastDayOfMonthPeru(todayMillis) }

    var selectedStartMillis by rememberSaveable { mutableStateOf<Long?>(firstDayOfMonth) }
    var selectedEndMillis by rememberSaveable { mutableStateOf<Long?>(lastDayOfMonth) }

    var showLoadingDialog by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf("Obteniendo detalle de factura...") }
    var loadingDebugInfo by remember { mutableStateOf<String?>(null) }
    var loadingInvoiceId by remember { mutableStateOf<Int?>(null) }
    var isLoadingPurchase by remember { mutableStateOf(false) }
    var invoicesWithActiveTimer by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isDetailingAll by remember { mutableStateOf(false) }

    // Variables para el diálogo de credenciales
    var rucInput by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    var solPasswordInput by remember { mutableStateOf("") }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var consultAfterLogin by remember { mutableStateOf(false) }

    // Variables para el diálogo de logout
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isInitialLoadDone by rememberSaveable { mutableStateOf(false) }

    // ViewModel states
    val isLoadingViewModel by purchaseViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by purchaseViewModel.errorMessage.collectAsStateWithLifecycle()
    val purchaseInvoices by purchaseViewModel.purchaseInvoices.collectAsStateWithLifecycle()
    val salesInvoices by purchaseViewModel.salesInvoices.collectAsStateWithLifecycle()

    // Usar funciones extraídas
    handleAutoRegisterInvoices(
        purchaseInvoices = purchaseInvoices,
        salesInvoices = salesInvoices,
        invoicesWithActiveTimer = invoicesWithActiveTimer,
        purchaseViewModel = purchaseViewModel,
        invoiceViewModel = invoiceViewModel,
        context = context,
        onTimerUpdate = { newTimers ->
            invoicesWithActiveTimer = newTimers
        }
    )

    setupCommonEffects(
        isLoadingViewModel = isLoadingViewModel,
        errorMessage = errorMessage,
        showLoadingDialog = showLoadingDialog,
        loadingInvoiceId = loadingInvoiceId,
        isLoadingPurchase = isLoadingPurchase,
        viewModel = purchaseViewModel,
        onIsLoadingChange = { isLoading = it },
        onLoadingDialogChange = { showLoadingDialog = it },
        onNavigateToDetail = { id, isPurchase ->
            onNavigateToDetail(DetailRoute(id = id, isPurchase = isPurchase))
        },
        onLoadingStatusChange = { loadingStatus = it },
        onLoadingDebugInfoChange = { loadingDebugInfo = it },
        onLoadingInvoiceIdChange = { loadingInvoiceId = it }
    )

    // Efecto inicial
    LaunchedEffect(Unit) {
        if (!isInitialLoadDone) {
            delay(500)
            val ruc = SunatPrefs.getRuc(context)
            val user = SunatPrefs.getUser(context)
            val solPassword = SunatPrefs.getSolPassword(context)

            // 🔴 FUERZA CARGA DE BD PRIMERO PARA VER SI FUNCIONA
            println("🔴🔴🔴 MODO DEBUG: CARGANDO SOLO BD PRIMERO")
            purchaseViewModel.loadInvoicesFromDB(
                isPurchase = (sectionActive == Section.PURCHASES)
            )

            delay(200) // Esperar 2 segundos

            val invoicesAfterDB = purchaseInvoices
            println("🔍 Facturas después de solo BD: ${invoicesAfterDB.size}")
            invoicesAfterDB.forEach {
                println("   - ${it.series}-${it.number} (Origen: ${if (it.id != null) "BD" else "API"})")
            }

            // Luego si hay credenciales, cargar API
            if (ruc != null && user != null && solPassword != null) {
                val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

                println("🟢 Cargando desde API SUNAT/SIRE...")
                purchaseViewModel.loadInvoicesFromAPI(
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    isPurchase = (sectionActive == Section.PURCHASES),
                    ruc = ruc,
                    user = user,
                    solPassword = solPassword
                )
            }

            isListVisible = true
            isInitialLoadDone = true
        }
    }

    // Calcular lista filtrada
    val filteredList = calculateFilteredList(
        sectionActive = sectionActive,
        isListVisible = isListVisible,
        selectedStartMillis = selectedStartMillis,
        selectedEndMillis = selectedEndMillis,
        hasLoadedSunatData = true,
        purchaseInvoices = purchaseInvoices,
        salesInvoices = salesInvoices,
        todayMillis = todayMillis
    )

    val hasInvoicesInProcess by remember {
        derivedStateOf {
            filteredList.any { it.status == "EN PROCESO" }
        }
    }

    // Diálogo de credenciales
    if (showCredentialsDialog) {
        var isValidating by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isValidating) {
                }
            },
            title = { Text("Credenciales SUNAT") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Complete sus credenciales SUNAT para continuar:")

                    // RUC
                    OutlinedTextField(
                        value = rucInput,
                        onValueChange = {
                            val newValue = it.filter { char -> char.isDigit() }
                            if (newValue.length <= 11) {
                                rucInput = newValue
                                errorMessage = null
                            }
                        },
                        label = { Text("RUC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage != null,
                        supportingText = if (errorMessage != null) {
                            { Text(errorMessage!!, color = Color.Red) }
                        } else {
                            { Text("${rucInput.length}/11 dígitos") }
                        }
                    )

                    // Usuario (siempre mayúsculas)
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            if (it.length <= 8) {
                                userInput = it.uppercase()
                                errorMessage = null
                            }
                        },
                        label = { Text("Usuario SOL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage != null,
                        supportingText = {
                            Text("${userInput.length}/8 caracteres")
                        }
                    )

                    // Clave SOL
                    OutlinedTextField(
                        value = solPasswordInput,
                        onValueChange = {
                            if (it.length <= 12) {
                                solPasswordInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Clave SOL") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage != null,
                        supportingText = {
                            Text("${solPasswordInput.length}/12 caracteres")
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        }
                    )

                    if (isValidating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1FB8B9)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Validando credenciales con SUNAT...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (rucInput.length != 11) {
                                errorMessage = "El RUC debe tener 11 dígitos"
                                return@launch
                            }

                            if (solPasswordInput.isEmpty()) {
                                errorMessage = "La clave SOL no puede estar vacía"
                                return@launch
                            }

                            isValidating = true
                            errorMessage = null

                            val isValid = purchaseViewModel.validateSunatCredentials(
                                ruc = rucInput,
                                user = userInput,
                                solPassword = solPasswordInput,
                            )

                            isValidating = false

                            if (isValid) {
                                SunatPrefs.saveRuc(context, rucInput)
                                SunatPrefs.saveUser(context, userInput)
                                SunatPrefs.saveSolPassword(context, solPasswordInput)

                                if (consultAfterLogin) {
                                    val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                                    val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

                                    purchaseViewModel.loadInvoicesFromAPI(
                                        periodStart = periodStart,
                                        periodEnd = periodEnd,
                                        isPurchase = (sectionActive == Section.PURCHASES),
                                        ruc = rucInput,
                                        user = userInput,
                                        solPassword = solPasswordInput
                                    )

                                    isListVisible = true
                                    consultAfterLogin = false
                                }

                                showCredentialsDialog = false
                                rucInput = ""
                                userInput = ""
                                solPasswordInput = ""

                                Toast.makeText(
                                    context,
                                    "✅ Credenciales válidas. Guardadas correctamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                errorMessage = "Credenciales incorrectas. Verifique RUC, Usuario y Clave SOL."
                            }
                        }
                    },
                    enabled = !isValidating &&
                            rucInput.length == 11 &&
                            userInput.isNotEmpty() &&
                            solPasswordInput.isNotEmpty() &&
                            solPasswordInput.length <= 12
                ) {
                    if (isValidating) {
                        Text("Validando...")
                    } else {
                        Text("Validar y Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isValidating) {
                            showCredentialsDialog = false
                            rucInput = ""
                            userInput = ""
                            solPasswordInput = ""
                            consultAfterLogin = false
                            errorMessage = null
                        }
                    },
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
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Limpiar credenciales guardadas
                        SunatPrefs.clearCredentials(context)

                        // Limpiar lista de facturas
                        purchaseViewModel.clearInvoices()

                        // Resetear estados
                        isListVisible = false
                        showLogoutDialog = false

                        // Volver a mostrar el diálogo de credenciales
                        consultAfterLogin = true
                        showCredentialsDialog = true

                        Toast.makeText(
                            context,
                            "Sesión cerrada. Ingrese nuevas credenciales.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        InvoiceLoadingDialog(
            isLoading = showLoadingDialog,
            statusMessage = loadingStatus,
            debugInfo = loadingDebugInfo,
            onDismiss = {
                showLoadingDialog = false
                loadingDebugInfo = null
                loadingInvoiceId = null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TÍTULO CON ICONO DE LOGOUT A LA DERECHA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Registro Contable",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFF1FB8B9),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    sectionActive = Section.PURCHASES
                    onPurchasesClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sectionActive == Section.PURCHASES) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Compras", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = {
                    sectionActive = Section.SALES
                    onSalesClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sectionActive == Section.SALES) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Ventas", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (filteredList.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No hay facturas en lista para detallar",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@IconButton
                        }

                        val processableInvoices = filteredList.filter { invoice ->
                            invoice.status != "CON DETALLE" &&
                                    invoice.status != "REGISTRADO" &&
                                    invoice.status != "EN PROCESO"
                        }

                        if (processableInvoices.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Todas las facturas ya tienen detalle o están en proceso",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@IconButton
                        }

                        val ruc = SunatPrefs.getRuc(context)
                        val user = SunatPrefs.getUser(context)
                        val solPassword = SunatPrefs.getSolPassword(context)

                        if (ruc == null || user == null || solPassword == null) {
                            Toast.makeText(
                                context,
                                "⚠️ Primero configure sus credenciales SUNAT en el botón CONSULTAR",
                                Toast.LENGTH_LONG
                            ).show()
                            return@IconButton
                        }

                        coroutineScope.launch {
                            var successful = 0
                            var failed = 0
                            val total = processableInvoices.size

                            isDetailingAll = true
                            loadingStatus = "Procesando 0/$total facturas..."

                            processableInvoices.forEach { invoice ->
                                val currentIsPurchase = (sectionActive == Section.PURCHASES)
                                val issuerRuc = purchaseViewModel.getIssuerRuc(invoice.id) ?: invoice.ruc

                                val result = suspendCoroutine { continuation ->
                                    purchaseViewModel.loadInvoiceDetailXmlWithUser(
                                        invoiceId = invoice.id,
                                        isPurchase = currentIsPurchase,
                                        issuerRuc = issuerRuc,
                                        context = context
                                    ) { success, _ ->
                                        continuation.resume(success)
                                    }
                                }

                                if (result) successful++ else failed++

                                withContext(Dispatchers.Main) {
                                    loadingStatus = "Procesando ${successful + failed}/$total facturas...\n✅ Exitosas: $successful\n❌ Fallidas: $failed"
                                }

                                delay(300)
                            }

                            withContext(Dispatchers.Main) {
                                isDetailingAll = false
                                Toast.makeText(
                                    context,
                                    "✅ Proceso completado: $successful exitosas, $failed fallidas",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = !hasInvoicesInProcess && !isDetailingAll
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Detallar todas las facturas",
                        tint = if (hasInvoicesInProcess || isDetailingAll)
                            Color.Gray
                        else
                            Color(0xFF1FB8B9),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Detallar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1FB8B9),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "todas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1FB8B9),
                        lineHeight = 16.sp
                    )
                }
            }

            DateRangeSelector(
                selectedStartMillis = selectedStartMillis,
                selectedEndMillis = selectedEndMillis,
                onDateRangeClick = { showCustomDatePicker = true },
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        StatusLegend(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .border(1.dp, Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1FB8B9))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando facturas desde SUNAT...",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val totalWidth = 470.dp

                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    Row(
                        modifier = Modifier
                            .width(totalWidth)
                            .background(Color.LightGray)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCell("Estado", 100.dp)
                        HeaderCell("RUC", 110.dp)
                        HeaderCell("Serie - Número", 160.dp)
                        HeaderCell("Fecha", 100.dp)
                    }

                    Column(
                        modifier = Modifier
                            .width(totalWidth)
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (!isListVisible) {
                            Text(
                                "Presione CONSULTAR para iniciar sesion",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else if (filteredList.isEmpty()) {
                            Text(
                                "No hay facturas para mostrar",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else {
                            filteredList.forEachIndexed { index, invoice ->
                                Column(
                                    modifier = Modifier.width(totalWidth)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .background(Color(0xFFB0C4DE))
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = invoice.businessName ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (invoice.status == "EN PROCESO") {
                                                            return@IconButton
                                                        }

                                                        val currentIsPurchase = (sectionActive == Section.PURCHASES)

                                                        val ruc = SunatPrefs.getRuc(context)
                                                        val user = SunatPrefs.getUser(context)
                                                        val solPassword = SunatPrefs.getSolPassword(context)

                                                        if (ruc == null || user == null || solPassword == null) {
                                                            Toast.makeText(
                                                                context,
                                                                "⚠️ Primero configure sus credenciales SUNAT en el botón CONSULTAR",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@IconButton
                                                        }

                                                        if (invoice.status == "CON DETALLE" || invoice.status == "REGISTRADO") {
                                                            onNavigateToDetail(
                                                                DetailRoute(
                                                                    id = invoice.id,
                                                                    isPurchase = currentIsPurchase
                                                                )
                                                            )
                                                            return@IconButton
                                                        }

                                                        val issuerRuc = purchaseViewModel.getIssuerRuc(invoice.id) ?: invoice.ruc

                                                        purchaseViewModel.loadInvoiceDetailXmlWithUser(
                                                            invoiceId = invoice.id,
                                                            isPurchase = currentIsPurchase,
                                                            issuerRuc = issuerRuc,
                                                            context = context
                                                        ) { success, message ->
                                                            if (success) {
                                                                Toast.makeText(context, "✅ $message", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "❌ $message", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp),
                                                    enabled = invoice.status != "EN PROCESO"
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = "Ver detalle",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (invoice.status == "EN PROCESO")
                                                            Color.Gray
                                                        else
                                                            Color.Black
                                                    )
                                                }
                                                InvoiceStatusCircle(invoice.status, size = 14.dp)
                                            }
                                        }
                                        SimpleTableCell(invoice.ruc, 110.dp)
                                        Box(
                                            modifier = Modifier.width(160.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${invoice.series} - ${invoice.number}",
                                                fontSize = 13.sp,
                                                color = Color.Black
                                            )
                                        }
                                        SimpleTableCell(invoice.issueDate, 100.dp)
                                    }
                                    if (index < filteredList.size - 1) {
                                        Divider(
                                            modifier = Modifier.width(totalWidth),
                                            thickness = 0.5.dp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isListVisible) {
                        Row(
                            modifier = Modifier
                                .width(totalWidth)
                                .background(Color.LightGray)
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Facturas registradas: ${filteredList.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val ruc = SunatPrefs.getRuc(context)
                    val user = SunatPrefs.getUser(context)
                    val solPassword = SunatPrefs.getSolPassword(context)

                    if (ruc == null || user == null || solPassword == null) {
                        consultAfterLogin = true
                        showCredentialsDialog = true
                        return@Button
                    }

                    val periodStart = convertDateToPeriod(selectedStartMillis ?: todayMillis)
                    val periodEnd = convertDateToPeriod(selectedEndMillis ?: todayMillis)

                    purchaseViewModel.loadInvoicesFromAPI(
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        isPurchase = (sectionActive == Section.PURCHASES),
                        ruc = ruc,
                        user = user,
                        solPassword = solPassword
                    )
                    isListVisible = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
            ) {
                Text("Consultar", style = MaterialTheme.typography.titleMedium)
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
                    Text("Subir Factura", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PurchaseScreenPreview() {
    val purchaseViewModel: PurchaseViewModel = viewModel()
    val invoiceViewModel: InvoiceViewModel = viewModel()
    PurchaseDetailScreen(
        purchaseViewModel = purchaseViewModel,
        invoiceViewModel = invoiceViewModel,
        onPurchasesClick = { },
        onSalesClick = { },
        onNavigateToRegister = { },
        onNavigateToDetail = { }
    )
}