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
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.purchaseregister.utils.formatDateFromISO
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.purchaseregister.api.responses.InvoiceDetailsResponse

data class DocumentItem(
    val name: String,
    val date: String,
    val time: String? = null,
    val status: String? = null,
    val url: String? = null,
    val type: String,
    val documentNumber: String,
    val fileId: String
)

fun createDocumentsForInvoice(
    invoiceDetails: InvoiceDetailsResponse
): List<DocumentItem> {
    return invoiceDetails.invoiceFiles.map { file ->
        DocumentItem(
            name = when (file.sunatFileType.lowercase()) {
                "pdf" -> "Documento PDF"
                "xml" -> "Archivo XML"
                "cdr" -> "Constancia CDR"
                else -> "Documento ${file.sunatFileType}"
            },
            date = file.uploadedAt,
            status = "DISPONIBLE",
            type = file.sunatFileType.lowercase(),
            documentNumber = "${invoiceDetails.series}-${invoiceDetails.number}",
            fileId = file.id
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentModal(
    invoiceId: String,
    documents: List<DocumentItem>,
    onDismiss: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val downloadingDocument by viewModel.downloadingDocument.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()

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

                if (documents.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                        items(documents) { document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.8f)
                                ) {
                                    Text(
                                        text = document.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (document.date.isNotBlank()) {
                                        Text(
                                            text = "Fecha: ${formatDateFromISO(document.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = when (document.type.uppercase()) {
                                            "PDF" -> Color(0xFFF44336)
                                            "XML" -> Color(0xFF2196F3)
                                            "CDR" -> Color(0xFF4CAF50)
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = document.type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            softWrap = false
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .padding(start = 2.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (downloadingDocument == "${document.documentNumber}-${document.type}") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF1FB8B9)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    viewModel.downloadDocument(
                                                        context = context,
                                                        invoiceId = invoiceId,
                                                        fileId = document.fileId,
                                                        documentType = document.type,
                                                        onStart = {
                                                            println("📥 Iniciando descarga: ${document.documentNumber}-${document.type}")
                                                        },
                                                        onSuccess = { filePath ->
                                                            println("✅ Archivo guardado en: $filePath")
                                                            Toast.makeText(
                                                                context,
                                                                "✅ ${document.type.uppercase()} descargado",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        onError = { error ->
                                                            println("❌ Error: $error")
                                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
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

                            if (documents.indexOf(document) < documents.size - 1) {
                                Divider(modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                } else {
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