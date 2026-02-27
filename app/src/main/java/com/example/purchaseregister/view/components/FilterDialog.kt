package com.example.purchaseregister.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onFilterClick: (String?, String?, String?) -> Unit
) {
    // Estados para los filtros
    var rucFilter by remember { mutableStateOf("") }
    var businessNameFilter by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    // Lista de estados disponibles
    val statusList = listOf("CONSULTADO", "EN PROCESO", "CON DETALLE", "REGISTRADO")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    text = "Filtrar Facturas",
                    fontSize = 20.sp,
                    color = Color(0xFF1FB8B9),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Filtro por RUC
                OutlinedTextField(
                    value = rucFilter,
                    onValueChange = { rucFilter = it },
                    label = { Text("RUC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )

                // Filtro por Razón Social
                OutlinedTextField(
                    value = businessNameFilter,
                    onValueChange = { businessNameFilter = it },
                    label = { Text("Razón Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )

                // Selector de Estado
                Text(
                    text = "Estado:",
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Lista de estados
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(statusList) { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedStatus == status),
                                onClick = { selectedStatus = status },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1FB8B9)
                                )
                            )
                            Text(
                                text = status,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Botones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            onFilterClick(
                                businessNameFilter.ifEmpty { null },
                                rucFilter.ifEmpty { null },
                                selectedStatus
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1FB8B9)
                        )
                    ) {
                        Text("Filtrar")
                    }
                }
            }
        }
    }
}