package com.example.purchaseregister.view.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.purchaseregister.viewmodel.ForgotPasswordState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onBackToLogin: () -> Unit,
    forgotPasswordState: ForgotPasswordState,
    onSendResetEmail: (String) -> Unit,
    onResetState: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Efecto para manejar el estado
    LaunchedEffect(forgotPasswordState) {
        when (forgotPasswordState) {
            is ForgotPasswordState.Success -> {
                Toast.makeText(context, forgotPasswordState.message, Toast.LENGTH_LONG).show()
                onBackToLogin()
                onResetState()
            }
            is ForgotPasswordState.Error -> {
                Toast.makeText(context, forgotPasswordState.message, Toast.LENGTH_SHORT).show()
                onResetState()
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF1FB8B9)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Título
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1FB8B9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción
                Text(
                    text = "Te enviaremos un enlace para restablecer tu contraseña",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF1FB8B9)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1FB8B9),
                        focusedLabelColor = Color(0xFF1FB8B9)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de enviar
                Button(
                    onClick = {
                        if (email.isNotEmpty()) {
                            onSendResetEmail(email)
                        } else {
                            Toast.makeText(context, "Ingresa tu correo electrónico", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    ),
                    enabled = forgotPasswordState !is ForgotPasswordState.Loading
                ) {
                    if (forgotPasswordState is ForgotPasswordState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar enlace", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Link para volver al login
                TextButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Volver a iniciar sesión",
                        color = Color(0xFF1FB8B9),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}