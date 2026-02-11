package com.example.purchaseregister.view.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.view.components.ReadOnlyField
import com.example.purchaseregister.utils.SunatPrefs
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.view.detail.DocumentItem
import com.example.purchaseregister.view.detail.DocumentModal
import com.example.purchaseregister.view.detail.crearDocumentosParaFactura  // ¡NUEVO IMPORT!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    id: Int,
    onBack: () -> Unit,
    rucProveedor: String?,
    serie: String?,
    numero: String?,
    fecha: String?,
    razonSocial: String?,
    tipoDocumento: String?,
    anio: String?,
    moneda: String?,
    costoTotal: String?,
    igv: String?,
    tipoCambio: String?,
    importeTotal: String?,
    esCompra: Boolean = true,
    productos: List<ProductItem> = emptyList(),
    onAceptar: () -> Unit = {},
    documentos: List<DocumentItem> = emptyList()
) {
    var showDocumentosDialog by remember { mutableStateOf(false) }

    val numeroComprobante = if (serie != null && numero != null) "$serie-$numero" else ""
    val fechaFormateada = fecha ?: ""
    val serieValida = serie ?: ""

    val documentosAutomaticos = remember(serieValida, numeroComprobante, fechaFormateada) {
        if (serieValida.isNotBlank() && numeroComprobante.isNotBlank() && fechaFormateada.isNotBlank()) {
            crearDocumentosParaFactura(serieValida, numeroComprobante, fechaFormateada)
        } else {
            emptyList()
        }
    }

    val documentosAMostrar = if (documentos.isNotEmpty()) documentos else documentosAutomaticos

    fun formatearUnidadMedida(cantidad: String, unidad: String): String {
        val unidadFormateada = when (unidad.uppercase()) {
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
            else -> if (unidad.isNotBlank()) unidad else ""
        }

        return if (unidadFormateada.isNotBlank()) {
            "$cantidad $unidadFormateada"
        } else {
            cantidad
        }
    }

    val context = LocalContext.current
    val rucPropio = remember { SunatPrefs.getRuc(context) ?: "" }
    val esOperacionInafecta = igv?.toDoubleOrNull() == 0.0 && costoTotal?.toDoubleOrNull() == 0.0
    val valorVentaInafecto = if (esOperacionInafecta) {
        importeTotal ?: "0.00"
    } else {
        costoTotal ?: "0.00"
    }

    if (showDocumentosDialog) {
        DocumentModal(
            documentos = documentosAMostrar,
            onDismiss = { showDocumentosDialog = false }
        )
    }

    BackHandler {
        onBack()
    }

    println("🎯 [DetailScreen] ID recibido: $id")
    println("🎯 [DetailScreen] esCompra: $esCompra")
    println("🎯 [DetailScreen] Número de productos recibidos: ${productos.size}")
    println("🎯 [DetailScreen] Documentos automáticos creados: ${documentosAutomaticos.size}")

    productos.forEachIndexed { index, producto ->
        println("🎯 [DetailScreen] Producto $index: ${producto.descripcion}, ${producto.costoUnitario}, ${producto.cantidad}")
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
                IconButton(
                    onClick = {
                        println("◀️ [DetailScreen] Icono flecha presionado")
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.Black
                    )
                }

                // Título
                Text(
                    text = "Detalle de factura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Spacer para equilibrar el espacio de la flecha y mantener el título centrado
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
            Spacer(modifier = Modifier.height(8.dp))
            // --- FILA 1: RUC, SERIE, NUMERO, FECHA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(
                    value = rucPropio,
                    onValueChange = { },
                    label = "RUC Propio",
                    modifier = Modifier.weight(2.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = serie ?: "",
                    onValueChange = { },
                    label = "Serie",
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = numero ?: "",
                    onValueChange = { },
                    label = "N°",
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
            }

            // --- FILA 2: TIPO DOCUMENTO, IMPORTACIÓN, AÑO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = fecha ?: "",
                    onValueChange = { },
                    label = "Fecha Emisión",
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = tipoDocumento ?: "",
                    onValueChange = { },
                    label = "Tipo de Documento",
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = anio ?: "",
                    onValueChange = { },
                    label = "Año",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
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
                    value = rucProveedor ?: "",
                    onValueChange = { },
                    label = "RUC Proveedor",
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = razonSocial ?: "",
                    onValueChange = { },
                    label = "Razón Social del Proveedor",
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight(),
                    isSingleLine = false,
                    textAlign = TextAlign.Center
                )
            }

            // --- FILA 4: DESCRIPCIÓN GENERAL (DESC + UNIDAD + CANTIDAD), COSTO UNITARIO ---
            if (productos.isNotEmpty()) {
                productos.forEachIndexed { index, producto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReadOnlyField(
                            value = formatearUnidadMedida(producto.cantidad, producto.unidadMedida),
                            onValueChange = { },
                            label = if (index == 0) "Cant" else "",
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            textAlign = TextAlign.Center
                        )
                        ReadOnlyField(
                            value = producto.descripcion,
                            onValueChange = { },
                            label = if (index == 0) "Descripción" else "",
                            modifier = Modifier
                                .weight(2.5f)
                                .fillMaxHeight(),
                            isSingleLine = false,
                            textAlign = TextAlign.Center
                        )
                        ReadOnlyField(
                            value = producto.costoUnitario,
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
                // Si no hay productos, mostrar un campo vacío
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

            // --- FILA 5: MONEDA Y TIPO DE CAMBIO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = moneda ?: "",
                    onValueChange = { },
                    label = "Moneda",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                ReadOnlyField(
                    value = tipoCambio ?: "",
                    onValueChange = { },
                    label = "T. Cambio",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            // --- FILA 6: COSTO TOTAL, IGV, IMPORTE TOTAL---
            if (esOperacionInafecta) {
                // Para operaciones inafectas (sin IGV)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // VALOR VENTA INAFECTO
                    ReadOnlyField(
                        value = valorVentaInafecto,
                        onValueChange = { },
                        label = "VALOR VENTA INAFECTO",
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center
                    )
                    // IMPORTE TOTAL
                    ReadOnlyField(
                        value = importeTotal ?: "",
                        onValueChange = { },
                        label = "IMPORTE TOTAL",
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Para operaciones normales (con IGV)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = costoTotal ?: "",
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
                        value = importeTotal ?: "",
                        onValueChange = { },
                        label = "IMPORTE TOTAL",
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BOTONES---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        println("📄 [DetailScreen] Presionando DOCUMENTACIÓN")
                        println("📄 [DetailScreen] Documentos a mostrar: ${documentosAMostrar.size}")
                        documentosAMostrar.forEachIndexed { index, doc ->
                            println("📄 [DetailScreen] Documento $index: ${doc.nombre} - ${doc.tipo}")
                        }
                        showDocumentosDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5A00)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Documentos",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Documentos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        println("🎯 [DetailScreen] Presionando REGRESAR")
                        onBack()
                    },
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailScreenPreview() {
    DetailScreen(
        id = 1,
        onBack = { },
        rucProveedor = "20551234891",
        serie = "F001",
        numero = "10",
        fecha = "28/01/2026",
        razonSocial = "PRODUCTOS TECNOLOGICOS S.A.",
        tipoDocumento = "FACTURA",
        anio = "2026",
        moneda = "Soles (PEN)",
        costoTotal = "100.00",
        igv = "18.00",
        tipoCambio = "3.75",
        importeTotal = "118.00",
        esCompra = true,
        productos = listOf(
            ProductItem("Laptop Dell", "850.00", "3", "UN"),
            ProductItem("Arroz Extra", "15.00", "30", "KG"),
            ProductItem("Aceite Vegetal", "8.50", "5", "L"),
            ProductItem("Tornillos", "0.50", "500", "GR"),
            ProductItem("Cable HDMI", "12.00", "10", "UN")
        )
    )
}