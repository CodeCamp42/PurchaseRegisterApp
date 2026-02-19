package com.example.purchaseregister.view.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.purchaseregister.view.components.ReadOnlyField
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    id: Int,
    onBack: () -> Unit,
    providerRuc: String?,
    series: String?,
    number: String?,
    date: String?,
    businessName: String?,
    documentType: String?,
    year: String?,
    currency: String?,
    totalCost: String?,
    igv: String?,
    exchangeRate: String?,
    totalAmount: String?,
    isPurchase: Boolean = true,
    products: List<ProductItem> = emptyList(),
    viewModel: DetailViewModel = viewModel() // Recibir el ViewModel
) {
    var showDocumentsDialog by remember { mutableStateOf(false) }

    val documentNumber = if (series != null && number != null) "$series-$number" else ""
    val formattedDate = date ?: ""
    val validSeries = series ?: ""

    val autoDocuments = remember(validSeries, documentNumber, formattedDate) {
        if (validSeries.isNotBlank() && documentNumber.isNotBlank() && formattedDate.isNotBlank()) {
            createDocumentsForInvoice(validSeries, documentNumber, formattedDate)
        } else {
            emptyList()
        }
    }

    // Ya no necesitamos pasar documents como parámetro, usamos autoDocuments
    val documentsToShow = autoDocuments

    fun formatUnitOfMeasure(quantity: String, unit: String): String {
        val formattedUnit = when (unit.uppercase()) {
            "KILO", "KILOS", "KILOGRAMO", "KILOGRAMOS", "KG", "KGS" -> "Kg"
            "GRAMO", "GRAMOS", "GR", "GRS", "G" -> "Gr"
            "LITRO", "LITROS", "L", "LT", "LTS" -> "Lt"
            "UNIDAD", "UNIDADES", "UN", "UND", "UNDS" -> "UN"
            "METRO", "METROS", "M", "MT", "MTS" -> "M"
            "CENTIMETRO", "CENTIMETROS", "CM", "CMS" -> "Cm"
            "MILIMETRO", "MILIMETROS", "MM", "MMS" -> "Mm"
            "PAQUETE", "PAQUETES", "PQ", "PQT", "PQTS" -> "Pq"
            "CAJA", "CAJAS", "CJ", "CJA", "CJAS" -> "Bx"
            "GALON", "US GALON", "GALONES", "GAL", "GALS" -> "Gal"
            "CASE", "CS" -> "Cs"
            else -> if (unit.isNotBlank()) unit else ""
        }
        return if (formattedUnit.isNotBlank()) "$quantity $formattedUnit" else quantity
    }

    val context = LocalContext.current
    val myRuc = remember { SunatPrefs.getRuc(context) ?: "" }
    val isUnaffectedOperation = igv?.toDoubleOrNull() == 0.0 && totalCost?.toDoubleOrNull() == 0.0
    val unaffectedSaleValue = if (isUnaffectedOperation) {
        totalAmount ?: "0.00"
    } else {
        totalCost ?: "0.00"
    }

    if (showDocumentsDialog) {
        DocumentModal(
            documents = documentsToShow,
            onDismiss = { showDocumentsDialog = false },
            viewModel = viewModel // Pasar el ViewModel al modal
        )
    }

    BackHandler {
        onBack()
    }

    // Scaffold ahora está aquí, en la pantalla
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
                        imageVector = Icons.Default.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ... (resto del contenido de la pantalla, igual que antes) ...
            Spacer(modifier = Modifier.height(8.dp))

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
                    value = series ?: "",
                    onValueChange = { },
                    label = "Serie",
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = number ?: "",
                    onValueChange = { },
                    label = "N°",
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = date ?: "",
                    onValueChange = { },
                    label = "Fecha Emisión",
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = documentType ?: "",
                    onValueChange = { },
                    label = "Tipo de Documento",
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = year ?: "",
                    onValueChange = { },
                    label = "Año",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(
                    value = providerRuc ?: "",
                    onValueChange = { },
                    label = "RUC Proveedor",
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = businessName ?: "",
                    onValueChange = { },
                    label = "Razón Social del Proveedor",
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight(),
                    isSingleLine = false,
                    textAlign = TextAlign.Center
                )
            }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = currency ?: "",
                    onValueChange = { },
                    label = "Moneda",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = exchangeRate ?: "",
                    onValueChange = { },
                    label = "T. Cambio",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

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
                        value = totalAmount ?: "",
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
                        value = totalCost ?: "",
                        onValueChange = { },
                        label = "Costo Total",
                        modifier = Modifier.weight(1.8f),
                        textAlign = TextAlign.Center
                    )
                    ReadOnlyField(
                        value = igv ?: "",
                        onValueChange = { },
                        label = "IGV",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center
                    )
                    ReadOnlyField(
                        value = totalAmount ?: "",
                        onValueChange = { },
                        label = "IMPORTE TOTAL",
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showDocumentsDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A00)),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
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
}

// Preview sin ViewModel, usando datos de ejemplo
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailScreenPreview() {
    DetailScreen(
        id = 1,
        onBack = { },
        providerRuc = "20551234891",
        series = "F001",
        number = "10",
        date = "28/01/2026",
        businessName = "PRODUCTOS TECNOLOGICOS S.A.",
        documentType = "FACTURA",
        year = "2026",
        currency = "Soles (PEN)",
        totalCost = "100.00",
        igv = "18.00",
        exchangeRate = "3.75",
        totalAmount = "118.00",
        isPurchase = true,
        products = listOf(
            ProductItem("Laptop Dell", "850.00", "3", "UN"),
            ProductItem("Arroz Extra", "15.00", "30", "KG"),
            ProductItem("Aceite Vegetal", "8.50", "5", "L"),
            ProductItem("Tornillos", "0.50", "500", "GR"),
            ProductItem("Cable HDMI", "12.00", "10", "UN")
        )
    )
}