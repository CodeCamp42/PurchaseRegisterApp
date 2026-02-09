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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purchaseregister.BuildConfig
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.service.GeminiService
import com.example.purchaseregister.utils.FormatoUtils
import com.example.purchaseregister.utils.MonedaUtils
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.view.components.ReadOnlyField
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroCompraScreen(
    onBack: () -> Unit,
    viewModel: InvoiceViewModel
) {
    val context = LocalContext.current

    // Verificar si está logueado en SUNAT
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

    // URI usando FileProvider
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // Estados de control
    var fotoTomada by remember { mutableStateOf(false) }
    var modoEdicion by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Estados de datos
    var rucPropio by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var esImportacion by remember { mutableStateOf(false) }
    var anioImportacion by remember { mutableStateOf("") }
    var moneda by remember { mutableStateOf("") }
    var tipoDocumento by remember { mutableStateOf("") }
    var rucProveedor by remember { mutableStateOf("") }
    var razonSocialProveedor by remember { mutableStateOf("") }
    var tipoCambio by remember { mutableStateOf("") }
    var costoTotal by remember { mutableStateOf("") }
    var igv by remember { mutableStateOf("") }
    var importeTotal by remember { mutableStateOf("") }

    val listaProductos = remember {
        mutableStateListOf(ProductItem("", "", ""))
    }

    // Lógica de cámara y permisos
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
        fotoTomada = true
        modoEdicion = false

        val prompt = """
        Analiza esta factura/boleta peruana y extrae los datos.
        La factura es de una COMPRA que YO (mi empresa) estoy haciendo a un PROVEEDOR.
        
        Identifica claramente:
        - PROVEEDOR/VENDEDOR: Quien me vende (sus datos van en ruc_provider y razon_social)
        - YO/COMPRADOR: Mi empresa (no incluir aquí, esos datos los tengo yo)
        
        INSTRUCCIONES ESPECÍFICAS:
        - Para moneda: escribir solo "Soles" o "Dólares" (sin códigos PEN/USD)
        - SI ES DÓLARES: Extraer el tipo_cambio NUMÉRICO que aparece en la factura (ej: 3.85, 3.75, 4.10)
        - El tipo_cambio NO es fijo, varía según la fecha de la factura
        
        JSON REQUERIDO (datos del PROVEEDOR/VENDEDOR):
        {
          "tipo_documento": "Factura, Boleta o Nota de Venta",
          "ruc_provider": "RUC del PROVEEDOR/VENDEDOR (11 dígitos)",
          "razon_social": "Nombre completo del PROVEEDOR/VENDEDOR",
          "serie": "Serie del documento",
          "numero": "Número del documento",
          "fecha": "DD/MM/YYYY",
          "moneda": "Dólares",  // o "Soles"
          "tipo_cambio": "3.85",  // ← EXTRAER EL VALOR REAL DE LA FACTURA
          "productos": [
            {
              "descripcion": "Nombre del producto",
              "cantidad": "Cantidad (solo números)",
              "unidad_medida": "KG, L, UN, etc",
              "costo_unitario": "Precio unitario (solo números)"
            }
          ],
          "costo_total": "Total sin IGV (solo números)",
          "igv": "IGV (18%) (solo números)",
          "importe_total": "Total con IGV (solo números)"
        }
        
        IMPORTANTE: 
        - ruc_provider es el RUC del que ME VENDE (proveedor/vendedor)
        - No incluir MI RUC (comprador) en la respuesta
        - Las cantidades y montos deben ser solo números, sin símbolos
        """.trimIndent()

        Log.d("GEMINI_APP", "📸 Foto tomada, llamando a Gemini...")

        GeminiService.analyzeInvoice(
            bitmap = bitmap,
            prompt = prompt,
            apiKey = BuildConfig.GEMINI_API_KEY,
            onSuccess = { response ->
                scope.launch {
                    try {
                        Log.d("GEMINI_APP", "✅ Respuesta recibida, procesando...")

                        val jsonText = GeminiService.extractJsonFromResponse(response)
                        Log.d("GEMINI_APP", "📄 JSON extraído: ${jsonText.take(300)}...")

                        val json = JSONObject(jsonText)
                        Log.d("GEMINI_JSON_KEYS", "Claves en JSON: ${json.keys().asSequence().toList()}")

                        // Extraer datos
                        tipoDocumento = json.optString("tipo_documento")
                        rucProveedor = json.optString("ruc_provider")
                        razonSocialProveedor = json.optString("razon_social")
                        serie = json.optString("serie")
                        numero = json.optString("numero")
                        fecha = json.optString("fecha")
                        val monedaExtraida = json.optString("moneda")
                        moneda = MonedaUtils.formatearMoneda(monedaExtraida)
                        val tipoCambioExtraido = json.optString("tipo_cambio")

                        // Procesar productos
                        json.optJSONArray("productos")?.let { productosArray ->
                            Log.d("GEMINI_PRODUCTOS", "Número de productos: ${productosArray.length()}")

                            for (i in 0 until productosArray.length()) {
                                val producto = productosArray.getJSONObject(i)
                                Log.d("GEMINI_PRODUCTOS",
                                    "Producto $i: desc=${producto.optString("descripcion")}, " +
                                            "costo=${producto.optString("costo_unitario")}, " +
                                            "cant=${producto.optString("cantidad")}")
                            }
                        }

                        // Manejar tipo de cambio para dólares
                        if (MonedaUtils.esMonedaDolares(moneda)) {
                            tipoCambio = when {
                                tipoCambioExtraido.matches(Regex("[0-9]+(\\.[0-9]+)?")) -> tipoCambioExtraido
                                tipoCambioExtraido == "null" || tipoCambioExtraido.isEmpty() -> ""
                                else -> {
                                    Regex("[0-9]+(\\.[0-9]+)?").find(tipoCambioExtraido)?.value ?: ""
                                }
                            }

                            if (tipoCambio.isEmpty()) {
                                Log.w("TIPO_CAMBIO", "No se pudo extraer tipo de cambio para dólares")
                                Toast.makeText(context, "Ingrese manualmente el tipo de cambio", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            tipoCambio = ""
                        }

                        costoTotal = FormatoUtils.limpiarMonto(json.optString("costo_total"))
                        igv = FormatoUtils.limpiarMonto(json.optString("igv"))
                        importeTotal = FormatoUtils.limpiarMonto(json.optString("importe_total"))
                        rucPropio = SunatPrefs.getRuc(context) ?: ""

                        Log.d("GEMINI_MONTOS",
                            "CostoTotal: $costoTotal, IGV: $igv, ImporteTotal: $importeTotal")

                        // Procesar productos
                        listaProductos.clear()
                        json.optJSONArray("productos")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val p = arr.getJSONObject(i)
                                listaProductos.add(
                                    ProductItem(
                                        p.optString("descripcion"),
                                        p.optString("costo_unitario"),
                                        p.optString("cantidad"),
                                        unidadMedida = p.optString("unidad_medida")
                                    )
                                )
                            }
                            Log.d("GEMINI_PRODUCTOS", "Productos agregados a lista: ${listaProductos.size}")
                        }

                        if (listaProductos.isEmpty()) {
                            Log.w("GEMINI_PRODUCTOS", "⚠️ Lista de productos vacía, agregando uno vacío")
                            listaProductos.add(ProductItem("", "", ""))
                        }

                        Toast.makeText(context, "✅ Factura analizada!", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Log.e("GEMINI_APP", "❌ Error procesando: ${e.message}")
                        Log.e("GEMINI_APP", "Stack: ${e.stackTraceToString()}")
                        Toast.makeText(context, "Error: ${e.message?.take(50)}...", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            onError = { errorMsg ->
                scope.launch {
                    isLoading = false
                    Log.e("GEMINI_APP", "❌ Error: $errorMsg")
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Lanzador para permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_LONG).show()
        }
    }

    val esDolares = MonedaUtils.esMonedaDolares(moneda)
    val monedaWeight = if (esDolares) 1.2f else 2f

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flecha de retroceso
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
                    }

                    // Título con Icono de Cámara
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

                // --- FILA 1: RUC, SERIE, NUMERO---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = rucPropio,
                        onValueChange = { rucPropio = it },
                        label = "RUC",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(2.8f)
                    )
                    ReadOnlyField(
                        value = serie,
                        onValueChange = { serie = it },
                        label = "Serie",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = numero,
                        onValueChange = { numero = it },
                        label = "N°",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(2f)
                    )
                }

                // --- FILA 2: FECHA, TIPO DOCUMENTO, IMPORTACIÓN, AÑO ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = fecha,
                        onValueChange = { fecha = it },
                        label = "Fecha Emisión",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = tipoDocumento,
                        onValueChange = { tipoDocumento = it },
                        label = "Tipo de Documento",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = anioImportacion,
                        onValueChange = { anioImportacion = it },
                        label = "Año",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1f)
                    )
                }

                // --- FILA 3: RUC Y RAZÓN SOCIAL EN UNA SOLA LÍNEA ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = rucProveedor,
                        onValueChange = { rucProveedor = it },
                        label = "RUC Proveedor",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    )
                    ReadOnlyField(
                        value = razonSocialProveedor,
                        onValueChange = { razonSocialProveedor = it },
                        label = "Razón Social del Proveedor",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        isSingleLine = false,
                        textAlign = TextAlign.Center
                    )
                }

                // --- FILA 4: DESCRIPCIÓN, COSTO UNIT, CANTIDAD ---
                listaProductos.forEachIndexed { index, producto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReadOnlyField(
                            value = FormatoUtils.formatearUnidadMedida(producto.cantidad, producto.unidadMedida),
                            onValueChange = { nuevoValor ->
                                val partes = nuevoValor.split(" ")
                                val cantidad = partes.firstOrNull() ?: ""
                                val unidadRaw = if (partes.size > 1) partes.drop(1).joinToString(" ") else ""

                                val unidadParaAlmacenar = when (unidadRaw.uppercase().trim()) {
                                    "KG", "KGS", "KILO", "KILOS" -> "KILOGRAMOS"
                                    "L", "LT", "LTS", "LITRO", "LITROS" -> "LITROS"
                                    "UN", "UND", "UNDS", "UNIDAD", "UNIDADES" -> "UNIDADES"
                                    "GR", "GRS", "GRAMO", "GRAMOS" -> "GRAMOS"
                                    "M", "MT", "MTS", "METRO", "METROS" -> "METROS"
                                    "CM", "CMS", "CENTIMETRO", "CENTIMETROS" -> "CENTIMETROS"
                                    "MM", "MMS", "MILIMETRO", "MILIMETROS" -> "MILIMETROS"
                                    else -> unidadRaw
                                }

                                listaProductos[index] = producto.copy(
                                    cantidad = cantidad,
                                    unidadMedida = unidadParaAlmacenar
                                )
                            },
                            label = if (index == 0) "Cant" else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                        ReadOnlyField(
                            value = producto.descripcion,
                            onValueChange = { nuevaDesc ->
                                listaProductos[index] = producto.copy(descripcion = nuevaDesc)
                            },
                            label = if (index == 0) "Descripción" else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(2.5f)
                                .fillMaxHeight(),
                            isSingleLine = false
                        )
                        ReadOnlyField(
                            value = producto.costoUnitario,
                            onValueChange = { nuevoCosto ->
                                listaProductos[index] = producto.copy(costoUnitario = nuevoCosto)
                            },
                            label = if (index == 0) "Costo Unit." else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                    }
                }

                // --- FILA 5: MONEDA, T. CAMBIO (CONDICIONAL) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = moneda,
                        onValueChange = { moneda = it },
                        label = "Moneda",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(monedaWeight)
                    )
                    if (esDolares) {
                        ReadOnlyField(
                            value = tipoCambio,
                            onValueChange = { tipoCambio = it },
                            label = "T. Cambio",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                // --- FILA 6: COSTO TOTAL, IGV e IMPORTE TOTAL ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = costoTotal,
                        onValueChange = { costoTotal = it },
                        label = "Costo Total",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = igv,
                        onValueChange = { igv = it },
                        label = "IGV",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = importeTotal,
                        onValueChange = { importeTotal = it },
                        label = "Importe Total",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(2f),
                        isHighlight = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- BOTONES FINALES ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (rucProveedor.isEmpty() || serie.isEmpty() || numero.isEmpty()) {
                                Toast.makeText(context, "Complete los datos de la factura", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            listaProductos.forEachIndexed { index, producto ->
                                Log.d("RegistroScreen",
                                    "Producto $index: ${producto.descripcion}, " +
                                            "Cantidad=${producto.cantidad}, " +
                                            "Unidad=${producto.unidadMedida}, " +
                                            "Costo=${producto.costoUnitario}")
                            }

                            viewModel.agregarNuevaFacturaCompra(
                                ruc = rucProveedor,
                                razonSocial = razonSocialProveedor,
                                serie = serie,
                                numero = numero,
                                fechaEmision = fecha,
                                tipoDocumento = tipoDocumento,
                                moneda = moneda,
                                costoTotal = costoTotal,
                                igv = igv,
                                importeTotal = importeTotal,
                                anio = if (esImportacion) anioImportacion else "",
                                tipoCambio = tipoCambio,
                                productos = listaProductos.filter {
                                    it.descripcion.isNotBlank() && it.cantidad.isNotBlank()
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
                        enabled = fotoTomada && !isLoading
                    ) {
                        Text(text = "REGISTRAR", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { modoEdicion = !modoEdicion },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (modoEdicion) Color(0xFF1FB8B9) else Color.Gray
                        ),
                        shape = MaterialTheme.shapes.medium,
                        enabled = fotoTomada && !isLoading
                    ) { Text("EDITAR", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        // --- CAPA DE LOADING
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
fun RegistroCompraScreenPreview() {
    val viewModel: InvoiceViewModel = viewModel()
    RegistroCompraScreen(onBack = { }, viewModel = viewModel)
}