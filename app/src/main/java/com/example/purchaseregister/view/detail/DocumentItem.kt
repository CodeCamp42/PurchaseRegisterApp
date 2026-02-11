package com.example.purchaseregister.view.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

// Modelo para los documentos
data class DocumentItem(
    val nombre: String,
    val fecha: String,
    val hora: String? = null,
    val estado: String? = null,
    val url: String? = null,
    val tipo: String,
    val numeroComprobante: String
)

// Función helper para crear documentos según el tipo de factura
fun crearDocumentosParaFactura(
    serie: String,
    numeroComprobante: String,
    fecha: String
): List<DocumentItem> {
    val documentos = mutableListOf<DocumentItem>()

    documentos.add(
        DocumentItem(
            nombre = "Documento PDF",
            fecha = fecha,
            estado = "DISPONIBLE",
            tipo = "pdf",
            numeroComprobante = numeroComprobante
        )
    )

    documentos.add(
        DocumentItem(
            nombre = "Archivo XML",
            fecha = fecha,
            estado = "DISPONIBLE",
            tipo = "xml",
            numeroComprobante = numeroComprobante
        )
    )

    // Agregar CDR solo si la serie empieza con 'F'
    if (serie.startsWith("F", ignoreCase = true)) {
        documentos.add(
            DocumentItem(
                nombre = "Constancia CDR",
                fecha = fecha,
                estado = "DISPONIBLE",
                tipo = "cdr",
                numeroComprobante = numeroComprobante
            )
        )
    }

    return documentos
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentModal(
    documentos: List<DocumentItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: InvoiceViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // Estado para controlar descargas
    var descargandoDocumento by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Encabezado del modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Documentos Disponibles",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de documentos
                if (documentos.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Títulos de columnas
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Documento",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2.7f)
                                )
                                Text(
                                    text = "Tipo",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(modifier = Modifier.padding(top = 6.dp))
                        }

                        // Items de documentos
                        items(documentos) { documento ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nombre del documento
                                Column(
                                    modifier = Modifier.weight(1.8f)
                                ) {
                                    Text(
                                        text = documento.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (documento.fecha.isNotBlank()) {
                                        Text(
                                            text = "Fecha: ${documento.fecha}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Tipo de documento con badge
                                Box(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = when (documento.tipo.uppercase()) {
                                            "PDF" -> Color(0xFFF44336)
                                            "XML" -> Color(0xFF2196F3)
                                            "CDR" -> Color(0xFF4CAF50)
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = documento.tipo.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            softWrap = false  // ← Evita que se divida el texto
                                        )
                                    }
                                }

                                // Botón de descarga o loading
                                Box(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .padding(start = 2.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (descargandoDocumento == "${documento.numeroComprobante}-${documento.tipo}") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF1FB8B9)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                println("📥 [DocumentModal] === INICIANDO DESCARGA ===")
                                                println("📥 [DocumentModal] Número comprobante: ${documento.numeroComprobante}")
                                                println("📥 [DocumentModal] Tipo: ${documento.tipo}")
                                                println("📥 [DocumentModal] Context es nulo?: ${context == null}")
                                                descargandoDocumento = "${documento.numeroComprobante}-${documento.tipo}"
                                                scope.launch {
                                                    try {
                                                        println("🚀 [DocumentModal] Llamando a viewModel.descargarConDownloadManager...")
                                                        viewModel.descargarConDownloadManager(
                                                            context = context,
                                                            numeroComprobante = documento.numeroComprobante,
                                                            tipo = documento.tipo,
                                                            baseUrl = "http://192.168.1.85:3043"
                                                        )

                                                        println("✅ [DocumentModal] Llamada a descargarConDownloadManager completada")
                                                        Toast.makeText(
                                                            context,
                                                            "Descargando ${documento.tipo.uppercase()}...",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    } catch (e: Exception) {
                                                        println("❌ [DocumentModal] ERROR CAPTURADO: ${e.message}")
                                                        e.printStackTrace()
                                                        Toast.makeText(
                                                            context,
                                                            "Error: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } finally {
                                                        println("⏳ [DocumentModal] Delay de 500ms antes de resetear estado")
                                                        kotlinx.coroutines.delay(500)
                                                        descargandoDocumento = null
                                                        println("🔄 [DocumentModal] Estado resetado: descargandoDocumento = null")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)  // ← Tamaño reducido
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Descargar",
                                                tint = Color(0xFF1FB8B9)
                                            )
                                        }
                                    }
                                }
                            }

                            if (documentos.indexOf(documento) < documentos.size - 1) {
                                Divider(modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                } else {
                    // Mensaje cuando no hay documentos
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = "Sin documentos",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay documentos disponibles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón para cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    )
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}