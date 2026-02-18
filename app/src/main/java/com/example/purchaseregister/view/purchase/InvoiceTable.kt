package com.example.purchaseregister.view.purchase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.components.HeaderCell
import com.example.purchaseregister.components.InvoiceStatusCircle
import com.example.purchaseregister.components.SimpleTableCell
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.utils.SunatPrefs
import com.example.purchaseregister.viewmodel.Section

@Composable
fun InvoiceTable(
    invoices: List<Invoice>,
    sectionActive: Section,
    isListVisible: Boolean,
    onInvoiceClick: (Invoice, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val horizontalScrollState = rememberScrollState()
    val totalWidth = 470.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
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
            Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                // Cabecera
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

                // Cuerpo de la tabla
                Column(
                    modifier = Modifier
                        .width(totalWidth)
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!isListVisible) {
                        Text(
                            "Presione CONSULTAR para iniciar sesión",
                            modifier = Modifier
                                .width(totalWidth)
                                .padding(20.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    } else if (invoices.isEmpty()) {
                        Text(
                            "No hay facturas para mostrar",
                            modifier = Modifier
                                .width(totalWidth)
                                .padding(20.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    } else {
                        invoices.forEachIndexed { index, invoice ->
                            Column(
                                modifier = Modifier.width(totalWidth)
                            ) {
                                // Fila de razón social
                                Row(
                                    modifier = Modifier
                                        .width(totalWidth)
                                        .background(Color(0xFFB0C4DE))
                                        .padding(vertical = 8.dp, horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = invoice.businessName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                }

                                // Fila de factura
                                Row(
                                    modifier = Modifier
                                        .width(totalWidth)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Columna Estado (con ícono y círculo)
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

                                                    if (invoice.status == "EN PROCESO") {
                                                        Toast.makeText(context, "Factura en proceso", Toast.LENGTH_SHORT).show()
                                                        return@IconButton
                                                    }

                                                    onInvoiceClick(invoice, sectionActive == Section.PURCHASES)
                                                },
                                                modifier = Modifier.size(24.dp),
                                                enabled = invoice.status != "EN PROCESO"
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Visibility,
                                                    contentDescription = "Ver detalle",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (invoice.status == "EN PROCESO") Color.Gray else Color.Black
                                                )
                                            }
                                            InvoiceStatusCircle(invoice.status, size = 14.dp)
                                        }
                                    }

                                    // RUC
                                    SimpleTableCell(invoice.ruc, 110.dp)

                                    // Serie - Número
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

                                    // Fecha
                                    SimpleTableCell(invoice.issueDate, 100.dp)
                                }

                                // Divisor entre facturas
                                if (index < invoices.size - 1) {
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

                // Pie de tabla
                if (isListVisible && invoices.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .width(totalWidth)
                            .background(Color.LightGray)
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Facturas registradas: ${invoices.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}