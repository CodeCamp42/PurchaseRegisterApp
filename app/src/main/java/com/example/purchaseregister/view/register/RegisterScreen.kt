package com.example.purchaseregister.view.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.purchaseregister.model.ProductoItem
import com.example.purchaseregister.view.components.ReadOnlyField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroCompraScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // --- ESTADOS DE DATOS (Vacíos por defecto) ---
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
        mutableStateListOf(ProductoItem("", "", ""))
    }

    // --- LÓGICA DE CÁMARA Y PERMISOS ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Aquí se recibirá la imagen. Próximo paso: Procesar con OCR.
            Toast.makeText(context, "Imagen capturada", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para solicitar permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flecha de retroceso a la izquierda
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
                        text = "Registrar compra",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Escanear",
                            tint = Color(0xFF1FB8B9)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
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
            // --- FILA 1: RUC, SERIE, NUMERO, FECHA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(value = rucPropio, label = "RUC", modifier = Modifier.weight(2.8f))
                ReadOnlyField(value = serie, label = "Serie", modifier = Modifier.weight(1.5f))
                ReadOnlyField(value = numero, label = "N°", modifier = Modifier.weight(1f))
                ReadOnlyField(
                    value = fecha,
                    label = "Fecha Emisión",
                    modifier = Modifier.weight(2.8f)
                )
            }

            // --- FILA 2: TIPO DOCUMENTO, IMPORTACIÓN, AÑO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = tipoDocumento,
                    label = "Tipo de Documento",
                    modifier = Modifier.weight(1.8f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("¿Importación?", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = esImportacion, onCheckedChange = { esImportacion = it })
                }

                if (esImportacion) {
                    ReadOnlyField(
                        value = anioImportacion,
                        label = "Año",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
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
                    label = "RUC Proveedor",
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                )
                ReadOnlyField(
                    value = razonSocialProveedor,
                    label = "Razón Social del Proveedor",
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight(),
                    isSingleLine = false
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
                        value = producto.descripcion,
                        label = if (index == 0) "Descripción" else "",
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        isSingleLine = false
                    )
                    ReadOnlyField(
                        value = producto.costoUnitario,
                        label = if (index == 0) "Costo Unit." else "",
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    ReadOnlyField(
                        value = producto.cantidad,
                        label = if (index == 0) "Cant." else "",
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                    )
                }
            }

            // --- FILA 5: MONEDA, T. CAMBIO (CONDICIONAL) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val monedaWeight = if (!moneda.contains("Soles")) 1.2f else 2f
                ReadOnlyField(
                    value = moneda,
                    label = "Moneda",
                    modifier = Modifier.weight(monedaWeight)
                )

                if (!moneda.contains("Soles")) {
                    ReadOnlyField(
                        value = tipoCambio,
                        label = "T.C",
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
                    label = "Costo Total",
                    modifier = Modifier.weight(1.8f)
                )
                ReadOnlyField(value = igv, label = "IGV", modifier = Modifier.weight(1.5f))
                ReadOnlyField(
                    value = importeTotal,
                    label = "IMPORTE TOTAL",
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
                    onClick = { /* Lógica Registrar */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9)),
                    shape = MaterialTheme.shapes.medium
                ) { Text("REGISTRAR", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { /* Lógica Editar */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = MaterialTheme.shapes.medium
                ) { Text("EDITAR", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistroCompraScreenPreview() {
    RegistroCompraScreen(onBack = { })
}