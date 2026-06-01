package com.example.purchaseregister.view.register

import android.Manifest
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.service.GeminiService
import com.example.purchaseregister.service.MLKitOCRService
import com.example.purchaseregister.utils.FormatUtils
import com.example.purchaseregister.utils.CurrencyUtils
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.view.components.ReadOnlyField
import com.rctm.rctm.BuildConfig
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPurchaseScreen(
    onBack: () -> Unit,
    viewModel: PurchaseRegistrationViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val ruc = SunatPrefs.getRuc(context)
        if (ruc.isNullOrEmpty()) {
            Toast.makeText(context, "Primero inicie sesión en SUNAT", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    val scope = rememberCoroutineScope()
    val photoFile = remember {
        File.createTempFile("factura_", ".jpg", context.cacheDir)
    }

    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    var photoTaken by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var myRuc by remember { mutableStateOf("") }
    var series by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var isImport by remember { mutableStateOf(false) }
    var importYear by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("") }
    var providerRuc by remember { mutableStateOf("") }
    var providerBusinessName by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var igv by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }

    val productList = remember {
        mutableStateListOf(ProductItem("", "", ""))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            Toast.makeText(context, "Error al tomar foto", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap == null) {
            Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        isLoading = true
        photoTaken = true
        editMode = false

        Log.d("MLKIT_APP", "📸 Foto tomada, procesando con ML Kit...")

        MLKitOCRService.analyzeInvoice(
            bitmap = bitmap,
            onSuccess = { response ->
                scope.launch {
                    try {
                        Log.d("MLKIT_APP", "✅ OCR completado, procesando JSON...")

                        val json = JSONObject(response)
                        Log.d("MLKIT_JSON", "Claves en JSON: ${json.keys().asSequence().toList()}")

                        // Extraer todos los campos igual que antes
                        documentType = json.optString("tipo_documento")
                        providerRuc = json.optString("ruc_provider")
                        providerBusinessName = json.optString("razon_social")
                        series = json.optString("serie")
                        number = json.optString("numero")
                        date = json.optString("fecha")
                        val extractedCurrency = json.optString("moneda")
                        currency = CurrencyUtils.formatCurrency(extractedCurrency)
                        val extractedExchangeRate = json.optString("tipo_cambio")

                        // Procesar productos
                        json.optJSONArray("productos")?.let { productsArray ->
                            Log.d("MLKIT_PRODUCTOS", "Número de productos: ${productsArray.length()}")

                            productList.clear()
                            for (i in 0 until productsArray.length()) {
                                val p = productsArray.getJSONObject(i)
                                productList.add(
                                    ProductItem(
                                        p.optString("descripcion"),
                                        p.optString("costo_unitario"),
                                        p.optString("cantidad"),
                                        unitOfMeasure = p.optString("unidad_medida")
                                    )
                                )
                            }

                            if (productList.isEmpty()) {
                                productList.add(ProductItem("", "", ""))
                            }
                        }

                        // Procesar tipo de cambio
                        if (CurrencyUtils.isDollarCurrency(currency)) {
                            exchangeRate = when {
                                extractedExchangeRate.matches(Regex("[0-9]+(\\.[0-9]+)?")) -> extractedExchangeRate
                                extractedExchangeRate == "null" || extractedExchangeRate.isEmpty() -> ""
                                else -> {
                                    Regex("[0-9]+(\\.[0-9]+)?").find(extractedExchangeRate)?.value ?: ""
                                }
                            }

                            if (exchangeRate.isEmpty()) {
                                Log.w("TIPO_CAMBIO", "No se pudo extraer tipo de cambio para dólares")
                                Toast.makeText(context, "Ingrese manualmente el tipo de cambio", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            exchangeRate = ""
                        }

                        // Totales
                        totalCost = FormatUtils.cleanAmount(json.optString("costo_total"))
                        igv = FormatUtils.cleanAmount(json.optString("igv"))
                        totalAmount = FormatUtils.cleanAmount(json.optString("importe_total"))
                        myRuc = SunatPrefs.getRuc(context) ?: ""

                        Toast.makeText(context, "✅ Factura analizada con ML Kit!", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Log.e("MLKIT_APP", "❌ Error procesando: ${e.message}")
                        Toast.makeText(context, "Error al procesar OCR: ${e.message?.take(50)}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            onError = { errorMsg ->
                scope.launch {
                    isLoading = false
                    Log.e("MLKIT_APP", "❌ Error ML Kit: $errorMsg")
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_LONG).show()
        }
    }

    val isDollar = CurrencyUtils.isDollarCurrency(currency)
    val currencyWeight = if (isDollar) 1.2f else 2f

    Box(modifier = Modifier.fillMaxSize()) {
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

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Registrar factura",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Escanear",
                                tint = if (isLoading) Color.Gray else Color(0xFF1FB8B9)
                            )
                        }
                    }
                }
            }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = myRuc,
                        onValueChange = { myRuc = it },
                        label = "RUC",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(2.8f)
                    )
                    ReadOnlyField(
                        value = series,
                        onValueChange = { series = it },
                        label = "Serie",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = number,
                        onValueChange = { number = it },
                        label = "N°",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(2f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Fecha Emisión",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = documentType,
                        onValueChange = { documentType = it },
                        label = "Tipo de Documento",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = importYear,
                        onValueChange = { importYear = it },
                        label = "Año",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = providerRuc,
                        onValueChange = { providerRuc = it },
                        label = "RUC Proveedor",
                        isReadOnly = !editMode,
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    )
                    ReadOnlyField(
                        value = providerBusinessName,
                        onValueChange = { providerBusinessName = it },
                        label = "Razón Social del Proveedor",
                        isReadOnly = !editMode,
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        isSingleLine = false,
                        textAlign = TextAlign.Center
                    )
                }

                productList.forEachIndexed { index, product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReadOnlyField(
                            value = FormatUtils.formatUnitOfMeasure(product.quantity, product.unitOfMeasure),
                            onValueChange = { newValue ->
                                val parts = newValue.split(" ")
                                val quantity = parts.firstOrNull() ?: ""
                                val unitRaw = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

                                val unitToStore = when (unitRaw.uppercase().trim()) {
                                    "KG", "KGS", "KILO", "KILOS" -> "KILOGRAMOS"
                                    "L", "LT", "LTS", "LITRO", "LITROS" -> "LITROS"
                                    "UN", "UND", "UNDS", "UNIDAD", "UNIDADES" -> "UNIDADES"
                                    "GR", "GRS", "GRAMO", "GRAMOS" -> "GRAMOS"
                                    "M", "MT", "MTS", "METRO", "METROS" -> "METROS"
                                    "CM", "CMS", "CENTIMETRO", "CENTIMETROS" -> "CENTIMETROS"
                                    "MM", "MMS", "MILIMETRO", "MILIMETROS" -> "MILIMETROS"
                                    else -> unitRaw
                                }

                                productList[index] = product.copy(
                                    quantity = quantity,
                                    unitOfMeasure = unitToStore
                                )
                            },
                            label = if (index == 0) "Cant" else "",
                            isReadOnly = !editMode,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                        ReadOnlyField(
                            value = product.description,
                            onValueChange = { newDesc ->
                                productList[index] = product.copy(description = newDesc)
                            },
                            label = if (index == 0) "Descripción" else "",
                            isReadOnly = !editMode,
                            modifier = Modifier
                                .weight(2.5f)
                                .fillMaxHeight(),
                            isSingleLine = false
                        )
                        ReadOnlyField(
                            value = product.unitCost,
                            onValueChange = { newCost ->
                                productList[index] = product.copy(unitCost = newCost)
                            },
                            label = if (index == 0) "Costo Unit." else "",
                            isReadOnly = !editMode,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = "Moneda",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(currencyWeight)
                    )
                    if (isDollar) {
                        ReadOnlyField(
                            value = exchangeRate,
                            onValueChange = { exchangeRate = it },
                            label = "T. Cambio",
                            isReadOnly = !editMode,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = totalCost,
                        onValueChange = { totalCost = it },
                        label = "Costo Total",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = igv,
                        onValueChange = { igv = it },
                        label = "IGV",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = "Importe Total",
                        isReadOnly = !editMode,
                        modifier = Modifier.weight(2f),
                        isHighlight = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (providerRuc.isEmpty() || series.isEmpty() || number.isEmpty()) {
                                Toast.makeText(context, "Complete los datos de la factura", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            productList.forEachIndexed { index, product ->
                                Log.d("RegistroScreen",
                                    "Producto $index: ${product.description}, " +
                                            "Cantidad=${product.quantity}, " +
                                            "Unidad=${product.unitOfMeasure}, " +
                                            "Costo=${product.unitCost}")
                            }

                            viewModel.addNewPurchaseInvoice(
                                ruc = providerRuc,
                                businessName = providerBusinessName,
                                series = series,
                                number = number,
                                issueDate = date,
                                documentType = documentType,
                                currency = currency,
                                totalCost = totalCost,
                                igv = igv,
                                totalAmount = totalAmount,
                                year = if (isImport) importYear else "",
                                exchangeRate = exchangeRate,
                                products = productList.filter {
                                    it.description.isNotBlank() && it.quantity.isNotBlank()
                                }
                            )

                            Toast.makeText(context, "✅ Factura listada exitosamente", Toast.LENGTH_LONG).show()
                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9)),
                        shape = MaterialTheme.shapes.medium,
                        enabled = photoTaken && !isLoading
                    ) {
                        Text(text = "REGISTRAR", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { editMode = !editMode },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editMode) Color(0xFF1FB8B9) else Color.Gray
                        ),
                        shape = MaterialTheme.shapes.medium,
                        enabled = photoTaken && !isLoading
                    ) { Text("EDITAR", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Analizando factura...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterPurchaseScreenPreview() {
    val viewModel: PurchaseRegistrationViewModel = viewModel()
    RegisterPurchaseScreen(onBack = { }, viewModel = viewModel)
}