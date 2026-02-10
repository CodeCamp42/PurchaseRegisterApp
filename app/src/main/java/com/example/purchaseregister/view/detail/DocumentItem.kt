package com.example.purchaseregister.view.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// Modelo para los documentos
data class DocumentItem(
    val nombre: String,
    val fecha: String,
    val hora: String? = null,
    val estado: String? = null, // "OK", "PENDIENTE", etc.
    val url: String? = null // URL para descargar (opcional)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentModal(
    documentos: List<DocumentItem>,
    onDismiss: () -> Unit
) {
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

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de documentos
                if (documentos.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "Fecha",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = "Estado",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(modifier = Modifier.padding(top = 4.dp))
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
                                Text(
                                    text = documento.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(2f)
                                )

                                // Fecha y hora
                                Column(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = documento.fecha,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (documento.hora != null) {
                                        Text(
                                            text = documento.hora,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Estado
                                if (documento.estado != null) {
                                    val estadoColor = when (documento.estado.uppercase()) {
                                        "OK", "COMPLETO", "APROBADO" -> Color(0xFF4CAF50)
                                        "PENDIENTE" -> Color(0xFFFF9800)
                                        "RECHAZADO" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    }

                                    Badge(
                                        containerColor = estadoColor,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = documento.estado,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            // Botón de descarga si hay URL
                            if (documento.url != null) {
                                TextButton(
                                    onClick = {
                                        // Aquí puedes agregar la lógica para descargar/abrir el documento
                                        println("📥 Descargando: ${documento.url}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Descargar")
                                }
                            }

                            if (documentos.indexOf(documento) < documentos.size - 1) {
                                Divider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                } else {
                    // Mensaje cuando no hay documentos
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = "Sin documentos",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay documentos disponibles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    )
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}
