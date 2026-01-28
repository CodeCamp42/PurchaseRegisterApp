package com.example.purchaseregister.view.puchase

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.purchaseregister.model.Invoice
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.example.purchaseregister.navigation.DetailRoute

// --- 1. PERSISTENCIA: FUNCIONES PARA EL HASH ---
//fun guardarSesionSunat(context: Context, hash: String) {
//    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
//    prefs.edit().putString("sunat_hash", hash).apply()
//}
//
//fun obtenerSesionSunat(context: Context): String? {
//    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
//    return prefs.getString("sunat_hash", null)
//}

fun guardarRucSunat(context: Context, ruc: String) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("sunat_ruc", ruc).apply()
}

fun guardarUsuarioSunat(context: Context, usuario: String) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("sunat_usuario", usuario).apply()
}

fun guardarTokenSunat(context: Context, token: String) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("sunat_token", token).apply()
}

fun obtenerTokenSunat(context: Context): String? {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return prefs.getString("sunat_token", null)
}

fun obtenerRucSunat(context: Context): String? {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return prefs.getString("sunat_ruc", null)
}

fun obtenerUsuarioSunat(context: Context): String? {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return prefs.getString("sunat_usuario", null)
}

// --- 2. DIÁLOGO CON WEBVIEW PARA SUNAT ---
@Composable
fun SunatLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Red)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                AndroidView(factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        webViewClient = object : WebViewClient() {

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                // 1) OBTENER COOKIES (AQUÍ ES EL PASO 1)
                                val cookieManager = CookieManager.getInstance()
                                val cookies = cookieManager.getCookie(url ?: "")

                                val token = cookies?.split(";")
                                    ?.map { it.trim() }
                                    ?.firstOrNull { it.startsWith("ITMENUSESSION=") }
                                    ?.split("=")
                                    ?.get(1)

                                println("TOKEN ITMENUSESSION: $token")

                                if (!token.isNullOrEmpty()) {
                                    guardarTokenSunat(context, token)
                                }

                                // 2) EJECUTAMOS JS PARA EXTRAER RUC Y USUARIO DEL DROPDOWN
                                view?.evaluateJavascript(
                                    """
                                    (function () {
                                        // 1) Abrir dropdown "Bienvenido..."
                                        const menuButton = [...document.querySelectorAll('*')]
                                            .find(el => el.innerText && el.innerText.includes('Bienvenido'));

                                        if (menuButton) {
                                            menuButton.click();
                                        }

                                        // 2) Extraer RUC y Usuario del menú desplegable
                                        const items = document.querySelectorAll("ul.dropdown-menu li.dropdown-header strong");
                                        let ruc = null;
                                        let usuario = null;

                                        items.forEach(el => {
                                            const text = el.innerText.trim();

                                            if (text.startsWith("RUC:")) {
                                                ruc = text.replace("RUC:", "").trim();
                                            }

                                            if (text.startsWith("Usuario:")) {
                                                usuario = text.replace("Usuario:", "").trim();
                                            }
                                        });

                                        return JSON.stringify({ ruc, usuario });
                                    })();
                                    """
                                ) { result ->

                                    if (result != null && result != "null") {
                                        try {
                                            val clean = result
                                                .removePrefix("\"")
                                                .removeSuffix("\"")
                                                .replace("\\\"", "\"")

                                            val json = JSONObject(clean)

                                            val ruc = json.optString("ruc", null)
                                            val usuario = json.optString("usuario", null)

                                            println("RUC OBTENIDO: $ruc")
                                            println("USUARIO OBTENIDO: $usuario")

                                            // Guardamos solo si hay RUC y Usuario reales
                                            if (!ruc.isNullOrEmpty() && ruc.length == 11 && !usuario.isNullOrEmpty()) {
                                                guardarRucSunat(context, ruc)
                                                guardarUsuarioSunat(context, usuario)

                                                Handler(Looper.getMainLooper())
                                                    .postDelayed({
                                                        onLoginSuccess()
                                                    }, 500)
                                            }

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }

                        loadUrl(
                            "https://api-seguridad.sunat.gob.pe/v1/clientessol/4f3b88b3-d9d6-402a-b85d-6a0bc857746a/oauth2/loginMenuSol?lang=es-PE&showDni=true&showLanguages=false&originalUrl=https://e-menu.sunat.gob.pe/cl-ti-itmenu/AutenticaMenuInternet.htm&state=rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAADZXhlcHQABnBhcmFtc3QASyomKiYvY2wtdGktaXRtZW51L01lbnVJbnRlcm5ldC5odG0mYjY0ZDI2YThiNWFmMDkxOTIzYjIzYjY0MDdhMWMxZGI0MWU3MzNhNnQABGV4ZWNweA=="
                        )
                    }
                })
            }
        }
    )
}

// --- FUNCIÓN AUXILIAR PARA FORMATEAR FECHA ---
fun Long?.toFormattedDate(): String {
    if (this == null) return ""
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = this
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(calendar.time)
}

enum class Section { COMPRAS, VENTAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    onComprasClick: () -> Unit,
    onVentasClick: () -> Unit,
    onNavigateToRegistrar: () -> Unit,
    onNavigateToDetalle: (DetailRoute) -> Unit
) {
    val context = LocalContext.current
    var sectionActive by remember { mutableStateOf(Section.COMPRAS) }
    var isListVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // --- NUEVO ESTADO PARA EL LOGIN ---
    var showSunatLogin by remember { mutableStateOf(false) }

    // --- ESTADOS PARA EL CALENDARIO ---
    var showDatePicker by remember { mutableStateOf(false) }
    val hoyMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var selectedDateRangeText by remember {
        val fechaFormateada = hoyMillis.toFormattedDate()
        // Como al inicio start y end son hoyMillis, solo mostramos una vez
        mutableStateOf(fechaFormateada)
    }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = hoyMillis, initialSelectedEndDateMillis = hoyMillis
    )

    // 1. LISTA DE DATOS (Fake API)
    val facturasCompras = remember {
        mutableStateListOf<Invoice>().apply {
            for (i in 1..10) {
                add(
                    Invoice(
                        id = i,
                        ruc = "2060123456$i",
                        serie = "F001",
                        numero = "$i",
                        fechaEmision = "22/01/2026",
                        razonSocial = "PROV COMPRAS $i",
                        tipoDocumento = "FACTURA",
                        moneda = "Dòlares (USD)",
                        costoTotal = "100.00",
                        igv = "18.00",
                        importeTotal = "118.00",
                        isSelected = false
                    )
                )
            }
        }
    }

    val facturasVentas = remember {
        mutableStateListOf<Invoice>().apply {
            for (i in 1..8) {
                add(
                    Invoice(
                        id = i,
                        ruc = "1040987654$i",
                        serie = "V001",
                        numero = "$i",
                        fechaEmision = "22/01/2026",
                        razonSocial = "CLIENTE VENTAS $i",
                        tipoDocumento = "FACTURA",
                        moneda = "Soles (PEN)",
                        costoTotal = "200.00",
                        igv = "36.00",
                        importeTotal = "236.00",
                        isSelected = false
                    )
                )
            }
        }
    }

    // --- LÓGICA DE FILTRADO ---
    val listaActualBase = if (sectionActive == Section.COMPRAS) facturasCompras else facturasVentas

    val listaFiltrada by remember(
        sectionActive,
        isListVisible,
        listaActualBase.map { it.isSelected },
        dateRangePickerState.selectedStartDateMillis,
        dateRangePickerState.selectedEndDateMillis
    ) {
        derivedStateOf {
            if (!isListVisible) return@derivedStateOf emptyList<Invoice>()
            val start = dateRangePickerState.selectedStartDateMillis
            // Si el usuario solo marcó una fecha, 'end' será igual a 'start' para el filtro
            val end = dateRangePickerState.selectedEndDateMillis ?: start

            if (start != null && end != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                listaActualBase.filter { factura ->
                    val fechaFacturaTime = sdf.parse(factura.fechaEmision)?.time ?: 0L
                    // Ahora el rango siempre es válido (ej. del 15 al 15)
                    fechaFacturaTime in start..end
                }
            } else {
                listaActualBase
            }
        }
    }

    val isSelectAllChecked by remember(sectionActive, listaFiltrada) {
        derivedStateOf {
            listaFiltrada.isNotEmpty() && listaFiltrada.all { it.isSelected }
        }
    }

    // --- DIÁLOGO DEL WEBVIEW ---
    if (showSunatLogin) {
        SunatLoginDialog(
            onDismiss = { showSunatLogin = false },
            onLoginSuccess = {
                showSunatLogin = false
                isLoading = true

                // Simulamos que la App está bajando las facturas de SUNAT
                Handler(Looper.getMainLooper()).postDelayed({
                    isListVisible = true
                    isLoading = false // Quita el círculo y muestra la tabla
                    Toast.makeText(context, "Sincronización completa", Toast.LENGTH_SHORT).show()
                }, 2000)
            }
        )
    }

    // --- LÓGICA DEL DIÁLOGO DEL CALENDARIO ---
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                val startMillis = dateRangePickerState.selectedStartDateMillis
                val endMillis = dateRangePickerState.selectedEndDateMillis

                if (startMillis != null) {
                    val startStr = startMillis.toFormattedDate()
                    // Si el fin es nulo, asumimos que es el mismo día que el inicio
                    val endStr = endMillis?.toFormattedDate() ?: startStr

                    selectedDateRangeText = if (startStr == endStr) {
                        startStr
                    } else {
                        "$startStr - $endStr"
                    }
                }
                showDatePicker = false
            }) {
                Text(
                    text = "Aceptar", color = Color(0xFF1FB8B9),
                    fontWeight = FontWeight.Bold
                )
            }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text(
                    text = "Cancelar", color = Color(0xFFFF5A00),
                    fontWeight = FontWeight.Bold
                )
            }
        }) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecciona el rango", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false, // Esto quita el icono de edición de texto que a veces causa bugs de entrada
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Título Provisional
        Text(
            text = "Registro Contable", // Aquí puedes cambiar el nombre
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Espacio entre botones
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Compras
            Button(
                onClick = {
                    sectionActive = Section.COMPRAS
                    onComprasClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    // Cambiamos el color si está seleccionado o no
                    containerColor = if (sectionActive == Section.COMPRAS) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Compras", style = MaterialTheme.typography.titleMedium)
            }

            // Botón Ventas
            Button(
                onClick = {
                    sectionActive = Section.VENTAS
                    onVentasClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sectionActive == Section.VENTAS) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Ventas", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(15.dp))

        // NUEVA FILA: Seleccionar Todos y Rango de Fechas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Grupo: Checkbox + Texto
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(checked = isSelectAllChecked, onCheckedChange = { checked ->
                        listaFiltrada.forEach { factura ->
                            val index = listaActualBase.indexOfFirst { it.id == factura.id }
                            if (index != -1) listaActualBase[index] =
                                listaActualBase[index].copy(isSelected = checked)
                        }
                    })
                }

                Text(
                    text = "Seleccionar todos",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Rectángulo para Rango de Fechas
            Surface(
                modifier = Modifier
                    .width(200.dp)
                    .height(45.dp)
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                    .clickable { showDatePicker = true },
                shape = MaterialTheme.shapes.medium,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = selectedDateRangeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // 2. --- TABLA DINÁMICA ---
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
                        text = "Sincronizando con SUNAT...",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val totalWidth = 650.dp

                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    // CABECERA
                    Row(
                        modifier = Modifier
                            .width(totalWidth)
                            .background(Color.LightGray)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(50.dp))
                        HeaderCell("RUC", 120.dp)
                        HeaderCell("Serie", 70.dp)
                        HeaderCell("Número", 90.dp)
                        HeaderCell("Fecha", 100.dp)
                        HeaderCell("Estado", 100.dp)
                        Row(
                            modifier = Modifier.width(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    // 1. Obtenemos solo las facturas seleccionadas de la lista que se ve en pantalla
                                    val facturasParaDescargar =
                                        listaFiltrada.filter { it.isSelected }

                                    if (facturasParaDescargar.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Generando archivo Excel",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        generarExcelEjemplo(
                                            context,
                                            facturasParaDescargar,
                                            isShareMode = false
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Seleccione al menos una factura",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Descargar",
                                    tint = Color(0xFF1FB8B9),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    // 1. Filtramos solo lo seleccionado
                                    val facturasParaCompartir =
                                        listaFiltrada.filter { it.isSelected }

                                    if (facturasParaCompartir.isNotEmpty()) {
                                        // 2. Mostramos el mensaje de que está procesando
                                        Toast.makeText(
                                            context,
                                            "Preparando archivo para compartir",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // 3. Llamamos a la función con el modo compartir ACTIVADO (true)
                                        generarExcelEjemplo(
                                            context,
                                            facturasParaCompartir,
                                            isShareMode = true
                                        )
                                    } else {
                                        // 4. Si no seleccionó nada, avisamos
                                        Toast.makeText(
                                            context,
                                            "Seleccione al menos una factura",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir",
                                    tint = Color(0xFF1FB8B9),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // CUERPO (Listado)
                    Column(
                        modifier = Modifier
                            .width(totalWidth)
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (!isListVisible) {
                            Text(
                                "Presione Consultar para ver registros",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else if (listaFiltrada.isEmpty()) {
                            Text(
                                "No hay resultados",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else {
                            listaFiltrada.forEach { factura ->
                                Row(
                                    modifier = Modifier
                                        .width(totalWidth)
                                        .background(if (factura.isSelected) Color(0xFFF5F5F5) else Color.Transparent)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.width(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkbox(
                                            checked = factura.isSelected,
                                            onCheckedChange = { checked ->
                                                val index =
                                                    listaActualBase.indexOfFirst { it.id == factura.id }
                                                if (index != -1) listaActualBase[index] =
                                                    listaActualBase[index].copy(isSelected = checked)
                                            })
                                    }
                                    SimpleTableCell(factura.ruc, 120.dp)
                                    SimpleTableCell(factura.serie, 70.dp)
                                    SimpleTableCell(factura.numero, 90.dp)
                                    SimpleTableCell(factura.fechaEmision, 100.dp)
                                    Box(
                                        modifier = Modifier.width(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "REGISTRADO",
                                            fontSize = 10.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.width(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = {
                                            onNavigateToDetalle(
                                                DetailRoute(
                                                    rucProveedor = factura.ruc,
                                                    serie = factura.serie,
                                                    numero = factura.numero,
                                                    fecha = factura.fechaEmision,
                                                    razonSocial = factura.razonSocial,
                                                    tipoDocumento = factura.tipoDocumento,
                                                    moneda = factura.moneda,
                                                    costoTotal = factura.costoTotal,
                                                    igv = factura.igv,
                                                    importeTotal = factura.importeTotal,
                                                    anio = factura.anio,
                                                    tipoCambio = factura.tipoCambio
                                                )
                                            )
                                        }) {
                                            Text(
                                                "Detalle",
                                                fontSize = 12.sp,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        }
                                    }
                                }
                                Divider(
                                    modifier = Modifier.width(totalWidth),
                                    thickness = 0.5.dp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                    // --- FOOTER
                    if (isListVisible) {
                        val seleccionados = listaFiltrada.count { it.isSelected }
                        Row(
                            modifier = Modifier
                                .width(totalWidth)
                                .background(Color.LightGray)
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = when {
                                    seleccionados == 1 -> "1 factura seleccionada de ${listaFiltrada.size}"
                                    seleccionados > 1 -> "$seleccionados facturas seleccionadas de ${listaFiltrada.size}"
                                    else -> "Facturas registradas: ${listaFiltrada.size}"
                                },
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. BOTONES INFERIORES: CONSULTAR Y REGISTRAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val token = obtenerTokenSunat(context)
                    val ruc = obtenerRucSunat(context)
                    val usuario = obtenerUsuarioSunat(context)

                    println("SUNAT DATA → RUC: $ruc | USUARIO: $usuario | TOKEN: $token")
                    if (token == null) {
                        showSunatLogin = true // Abre el WebView si no hay hash
                    } else {
                        isLoading = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            isListVisible = true
                            isLoading = false
                        }, 1000)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
            ) {
                Text("Consultar", style = MaterialTheme.typography.titleMedium)
            }
            if (sectionActive == Section.COMPRAS) {
                Button(
                    onClick = onNavigateToRegistrar,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
                ) { Text("Registrar", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
fun HeaderCell(text: String, width: Dp) {
    Text(
        text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

// Componente auxiliar para las celdas
@Composable
fun SimpleTableCell(text: String, width: Dp) {

    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PurchaseScreenPreview() {
    // Usamos funciones vacías { } para el preview porque no necesitamos lógica aquí
    PurchaseDetailScreen(
        onComprasClick = { },
        onVentasClick = { },
        onNavigateToRegistrar = { },
        onNavigateToDetalle = { })
}

fun generarExcelEjemplo(
    context: Context,
    facturas: List<Invoice>,
    isShareMode: Boolean = false
) {
    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Reporte")

        // 1. Cabecera y Datos
        val columnas = listOf("RUC", "Serie", "Número", "Fecha", "Razón Social")
        val headerRow = sheet.createRow(0)
        columnas.forEachIndexed { i, title -> headerRow.createCell(i).setCellValue(title) }
        facturas.forEachIndexed { index, factura ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(factura.ruc)
            row.createCell(1).setCellValue(factura.serie)
            row.createCell(2).setCellValue(factura.numero)
            row.createCell(3).setCellValue(factura.fechaEmision)
            row.createCell(4).setCellValue(factura.razonSocial)
        }

        val fileName = "Reporte_${System.currentTimeMillis()}.xlsx"
        val resolver = context.contentResolver

        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/RegistroContable")
            }
        }

        val uri = resolver.insert(contentUri, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { workbook.write(it) }
            workbook.close()

            Toast.makeText(context, "Excel generado exitosamente", Toast.LENGTH_SHORT).show()

            // 1. Creamos el Intent base para ver el archivo
            if (isShareMode) {
                // --- LÓGICA PARA COMPARTIR (WhatsApp, Gmail, etc.) ---
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de Facturas")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Adjunto envío el reporte de facturas generado desde la App."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Compartir reporte vía:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                // --- LÓGICA PARA VER/ABRIR (Sheets, Excel) ---
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        uri,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(viewIntent, "Abrir reporte con:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}