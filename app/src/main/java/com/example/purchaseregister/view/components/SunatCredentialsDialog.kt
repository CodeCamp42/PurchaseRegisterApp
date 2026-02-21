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
import kotlinx.coroutines.launch

@Composable
fun SunatCredentialsDialog(
    onDismiss: () -> Unit,
    onCredentialsSaved: () -> Unit,
    onShowTutorial: () -> Unit,
    externalClientId: String = "",
    externalClientSecret: String = "",
    onExternalCredentialsUpdated: () -> Unit = {},
    consultAfterLogin: Boolean = false,
    onConsultAfterLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var rucInput by remember { mutableStateOf("") }
    var solUsernameInput by remember { mutableStateOf("") }
    var solPasswordInput by remember { mutableStateOf("") }
    var clientIdInput by remember { mutableStateOf("") }
    var clientSecretInput by remember { mutableStateOf("") }
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

    // Precargar valores si existen - AHORA SIN isVisible
    LaunchedEffect(Unit) {
        if (clientIdInput.isEmpty()) {
            clientIdInput = SunatPrefs.getClientId(context) ?: ""
        }
        if (clientSecretInput.isEmpty()) {
            clientSecretInput = SunatPrefs.getClientSecret(context) ?: ""
        }
        if (rucInput.isEmpty()) {
            rucInput = SunatPrefs.getRuc(context) ?: ""
        }
        if (solUsernameInput.isEmpty()) {
            solUsernameInput = SunatPrefs.getSolUsername(context) ?: ""
        }
        if (solPasswordInput.isEmpty()) {
            solPasswordInput = SunatPrefs.getSolPassword(context) ?: ""
        }
        clientIdInput = SunatPrefs.getClientId(context) ?: ""
        clientSecretInput = SunatPrefs.getClientSecret(context) ?: ""
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Credenciales SUNAT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1FB8B9)
                )

                Text("Complete para continuar:")

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
                                // Validaciones
                                if (rucInput.length != 11) {
                                    localError = "El RUC debe tener 11 dígitos"
                                    return@launch
                                }
                                if (solUsernameInput.isEmpty()) {
                                    localError = "El usuario SOL no puede estar vacío"
                                    return@launch
                                }
                                if (solPasswordInput.isEmpty()) {
                                    localError = "La clave SOL no puede estar vacía"
                                    return@launch
                                }
                                if (clientIdInput.isEmpty()) {
                                    localError = "El Client ID no puede estar vacío"
                                    return@launch
                                }
                                if (clientSecretInput.isEmpty()) {
                                    localError = "El Client Secret no puede estar vacío"
                                    return@launch
                                }

                                SunatPrefs.saveRuc(context, rucInput)
                                SunatPrefs.saveSolUsername(context, solUsernameInput)
                                SunatPrefs.saveSolPassword(context, solPasswordInput)
                                SunatPrefs.saveClientId(context, clientIdInput)
                                SunatPrefs.saveClientSecret(context, clientSecretInput)

                                onCredentialsSaved()
                                onDismiss()

                                Toast.makeText(
                                    context,
                                    "✅ Credenciales SUNAT guardadas",
                                    Toast.LENGTH_SHORT
                                ).show()

                                if (consultAfterLogin) {
                                    onConsultAfterLogin()
                                }
                            }
                        },
                        enabled = rucInput.length == 11 &&
                                solUsernameInput.isNotEmpty() &&
                                solPasswordInput.isNotEmpty() &&
                                clientIdInput.isNotEmpty() &&
                                clientSecretInput.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}