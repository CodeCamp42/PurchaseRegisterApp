package com.example.purchaseregister.view.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.utils.CurrencyUtils.formatCurrency
import com.example.purchaseregister.utils.FormatUtils.formatUnitOfMeasure
import com.example.purchaseregister.view.components.ReadOnlyField
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse
import com.example.purchaseregister.utils.formatDateFromISO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    invoiceId: Int,
    isPurchase: Boolean,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val myRuc = remember { SunatPrefs.getRuc(context) ?: "" }
    var showDocumentsDialog by remember { mutableStateOf(false) }

    // Observar estados del ViewModel
    val invoiceDetails by viewModel.invoiceDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Cargar detalles cuando se abre la pantalla
    LaunchedEffect(invoiceId) {
        viewModel.loadInvoiceDetails(invoiceId)
    }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearDetails()
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Detalle de factura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error al cargar detalles",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = errorMessage ?: "")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadInvoiceDetails(invoiceId) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                invoiceDetails != null -> {
                    DetailContent(
                        invoiceDetails = invoiceDetails!!,
                        myRuc = myRuc,
                        isPurchase = isPurchase,
                        onDocumentsClick = { showDocumentsDialog = true },
                        onBack = onBack
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontraron datos")
                    }
                }
            }
        }
    }

    if (showDocumentsDialog && invoiceDetails != null) {
        DocumentModal(
            invoiceId = invoiceId,
            documents = createDocumentsForInvoice(invoiceDetails!!),
            onDismiss = { showDocumentsDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun DetailContent(
    invoiceDetails: InvoiceDetailsResponse,
    myRuc: String,
    isPurchase: Boolean,
    onDocumentsClick: () -> Unit,
    onBack: () -> Unit
) {
    val products = invoiceDetails.invoiceDetails.map { detail ->
        ProductItem(
            description = detail.description ?: "Sin descripción",
            quantity = detail.quantity ?: "0",
            unitCost = detail.unitPrice ?: "0",
            unitOfMeasure = detail.unitOfMeasure ?: "UNID"
        )
    }

    val documentNumber = "${invoiceDetails.series}-${invoiceDetails.number}"
    val formattedDate = formatDateFromISO(invoiceDetails.issueDate)

    val isUnaffectedOperation = invoiceDetails.igv.toDoubleOrNull() == 0.0 &&
            invoiceDetails.taxableAmount.toDoubleOrNull() == 0.0
    val unaffectedSaleValue = if (isUnaffectedOperation) {
        invoiceDetails.totalAmount
    } else {
        invoiceDetails.taxableAmount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Fila 1: RUC Propio, Serie, Número
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReadOnlyField(
                value = myRuc,
                onValueChange = { },
                label = "RUC Propio",
                modifier = Modifier.weight(2.8f),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = invoiceDetails.series,
                onValueChange = { },
                label = "Serie",
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = invoiceDetails.number,
                onValueChange = { },
                label = "N°",
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center
            )
        }

        // Fila 2: Fecha, Tipo Documento, Año
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadOnlyField(
                value = formattedDate,
                onValueChange = { },
                label = "Fecha Emisión",
                modifier = Modifier.weight(1.8f),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = when (invoiceDetails.docType) {
                    "01" -> "FACTURA"
                    "03" -> "BOLETA"
                    else -> invoiceDetails.docType
                },
                onValueChange = { },
                label = "Tipo de Documento",
                modifier = Modifier.weight(1.8f),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = invoiceDetails.period.take(4),
                onValueChange = { },
                label = "Año",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        // Fila 3: RUC Proveedor y Razón Social
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReadOnlyField(
                value = if (isPurchase) invoiceDetails.issuerRuc else invoiceDetails.receiverDocNumber,
                onValueChange = { },
                label = "RUC ${if (isPurchase) "Proveedor" else "Cliente"}",
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = if (isPurchase) invoiceDetails.issuerName else invoiceDetails.receiverName,
                onValueChange = { },
                label = "Razón Social",
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                isSingleLine = false,
                textAlign = TextAlign.Center
            )
        }

        // Productos
        if (products.isNotEmpty()) {
            products.forEachIndexed { index, product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = formatUnitOfMeasure(product.quantity, product.unitOfMeasure),
                        onValueChange = { },
                        label = if (index == 0) "Cant" else "",
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        textAlign = TextAlign.Center
                    )
                    ReadOnlyField(
                        value = product.description,
                        onValueChange = { },
                        label = if (index == 0) "Descripción" else "",
                        modifier = Modifier
                            .weight(2.5f)
                            .fillMaxHeight(),
                        isSingleLine = false,
                        textAlign = TextAlign.Center
                    )
                    ReadOnlyField(
                        value = product.unitCost,
                        onValueChange = { },
                        label = if (index == 0) "Costo Unit." else "",
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Fila vacía para mantener el diseño
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(
                    value = "",
                    onValueChange = { },
                    label = "Cant",
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = "",
                    onValueChange = { },
                    label = "Descripción",
                    modifier = Modifier
                        .weight(2.5f)
                        .fillMaxHeight(),
                    isSingleLine = false,
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = "",
                    onValueChange = { },
                    label = "Costo Unit.",
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Fila: Moneda y Tipo de Cambio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadOnlyField(
                value = formatCurrency(invoiceDetails.currency),
                onValueChange = { },
                label = "Moneda",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            ReadOnlyField(
                value = invoiceDetails.exchangeRate,
                onValueChange = { },
                label = "T. Cambio",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        // Fila: Montos
        if (isUnaffectedOperation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = unaffectedSaleValue,
                    onValueChange = { },
                    label = "VALOR VENTA INAFECTO",
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = invoiceDetails.totalAmount,
                    onValueChange = { },
                    label = "IMPORTE TOTAL",
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = invoiceDetails.taxableAmount,
                    onValueChange = { },
                    label = "Costo Total",
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = invoiceDetails.igv,
                    onValueChange = { },
                    label = "IGV",
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = invoiceDetails.totalAmount,
                    onValueChange = { },
                    label = "IMPORTE TOTAL",
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onDocumentsClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A00)),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Documentos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9)),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Regresar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailScreenPreview() {
    Text("Preview no disponible - requiere ViewModel")
}