package com.example.purchaseregister.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CredentialErrorDialog(
    errorMessage: String,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
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
                // Icono de error
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(48.dp)
                )

                // Título
                Text(
                    text = "Error de Autenticación",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Mensaje de error
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Botón Editar
                Button(
                    onClick = {
                        onEditClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    )
                ) {
                    Text("Editar Credenciales", fontSize = 16.sp)
                }
            }
        }
    }
}