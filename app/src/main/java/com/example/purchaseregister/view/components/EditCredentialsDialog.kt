package com.example.purchaseregister.view.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.purchaseregister.utils.SunatPrefs
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

@Composable
fun EditCredentialsDialog(
    onDismiss: () -> Unit,
    onCredentialsSaved: () -> Unit,
    onShowTutorial: () -> Unit,
    onSaveToBackend: (
        ruc: String?,
        solUsername: String?,
        solPassword: String?,
        clientId: String?,
        clientSecret: String?,
        onResult: (Boolean, String?) -> Unit
    ) -> Unit,
    externalClientId: String = "",
    externalClientSecret: String = "",
    onExternalCredentialsUpdated: () -> Unit = {},
    consultAfterLogin: Boolean = false,
    onConsultAfterLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val originalRuc = remember { SunatPrefs.getRuc(context) ?: "" }
    val originalSolUsername = remember { SunatPrefs.getSolUsername(context) ?: "" }
    val originalSolPassword = remember { SunatPrefs.getSolPassword(context) ?: "" }
    val originalClientId = remember { SunatPrefs.getClientId(context) ?: "" }
    val originalClientSecret = remember { SunatPrefs.getClientSecret(context) ?: "" }

    var rucInput by remember { mutableStateOf(originalRuc) }
    var solUsernameInput by remember { mutableStateOf(originalSolUsername) }
    var solPasswordInput by remember { mutableStateOf(originalSolPassword) }
    var clientIdInput by remember { mutableStateOf(originalClientId) }
    var clientSecretInput by remember { mutableStateOf(originalClientSecret) }

    var passwordVisible by remember { mutableStateOf(false) }
    var clientSecretVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(externalClientId, externalClientSecret) {
        if (externalClientId.isNotEmpty()) {
            clientIdInput = externalClientId
        }
        if (externalClientSecret.isNotEmpty()) {
            clientSecretInput = externalClientSecret
        }
        if (externalClientId.isNotEmpty() || externalClientSecret.isNotEmpty()) {
            onExternalCredentialsUpdated()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Editar Credenciales SUNAT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1FB8B9)
                )

                Text("Modifica los datos que deseas actualizar:")

                OutlinedTextField(
                    value = rucInput,
                    onValueChange = { rucInput = it.filter { char -> char.isDigit() }.take(11) },
                    label = { Text("RUC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = localError != null,
                    supportingText = { Text("${rucInput.length}/11 dígitos") },
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = solUsernameInput,
                    onValueChange = { solUsernameInput = it.uppercase().take(8) },
                    label = { Text("Usuario SOL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${solUsernameInput.length}/8 caracteres") },
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = solPasswordInput,
                    onValueChange = { solPasswordInput = it.take(12) },
                    label = { Text("Clave SOL") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${solPasswordInput.length}/12 caracteres") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = clientSecretInput,
                    onValueChange = { clientSecretInput = it },
                    label = { Text("Client Secret") },
                    visualTransformation = if (clientSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { clientSecretVisible = !clientSecretVisible }) {
                            Icon(
                                if (clientSecretVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                )

                TextButton(
                    onClick = onShowTutorial,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1FB8B9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "¿Cómo obtener Client ID y Client Secret?",
                        color = Color(0xFF1FB8B9),
                        fontSize = 13.sp
                    )
                }

                localError?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                // Solo enviar campos modificados
                                val updatedRuc = if (rucInput != originalRuc) rucInput else null
                                val updatedSolUsername = if (solUsernameInput != originalSolUsername) solUsernameInput else null
                                val updatedSolPassword = if (solPasswordInput != originalSolPassword) solPasswordInput else null
                                val updatedClientId = if (clientIdInput != originalClientId) clientIdInput else null
                                val updatedClientSecret = if (clientSecretInput != originalClientSecret) clientSecretInput else null

                                // Verificar si hay al menos un campo modificado
                                if (updatedRuc == null && updatedSolUsername == null &&
                                    updatedSolPassword == null && updatedClientId == null &&
                                    updatedClientSecret == null) {
                                    localError = "No hay cambios para guardar"
                                    return@launch
                                }

                                onSaveToBackend(
                                    updatedRuc,
                                    updatedSolUsername,
                                    updatedSolPassword,
                                    updatedClientId,
                                    updatedClientSecret
                                ) { success, errorMessage ->
                                    if (success) {
                                        // Guardar solo los campos que se modificaron
                                        if (updatedRuc != null) SunatPrefs.saveRuc(context, rucInput)
                                        if (updatedSolUsername != null) SunatPrefs.saveSolUsername(context, solUsernameInput)
                                        if (updatedSolPassword != null) SunatPrefs.saveSolPassword(context, solPasswordInput)
                                        if (updatedClientId != null) SunatPrefs.saveClientId(context, clientIdInput)
                                        if (updatedClientSecret != null) SunatPrefs.saveClientSecret(context, clientSecretInput)

                                        onCredentialsSaved()
                                        onDismiss()

                                        Toast.makeText(
                                            context,
                                            "✅ Credenciales SUNAT actualizadas",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        if (consultAfterLogin) {
                                            onConsultAfterLogin()
                                        }
                                    } else {
                                        localError = errorMessage
                                    }
                                }
                            }
                        },
                        // El botón se habilita si hay al menos un campo válido
                        enabled = (rucInput.length == 11 || rucInput == originalRuc) &&
                                (solUsernameInput.isNotEmpty() || solUsernameInput == originalSolUsername) &&
                                (solPasswordInput.isNotEmpty() || solPasswordInput == originalSolPassword) &&
                                (clientIdInput.isNotEmpty() || clientIdInput == originalClientId) &&
                                (clientSecretInput.isNotEmpty() || clientSecretInput == originalClientSecret) &&
                                // Y al menos un campo tiene cambios
                                (rucInput != originalRuc || solUsernameInput != originalSolUsername ||
                                        solPasswordInput != originalSolPassword || clientIdInput != originalClientId ||
                                        clientSecretInput != originalClientSecret),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar y Guardar")
                    }
                }
            }
        }
    }
}