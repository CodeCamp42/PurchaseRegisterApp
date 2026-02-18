package com.example.purchaseregister.components

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
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
                // Icono y título
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF1FB8B9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bienvenido",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1FB8B9)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs de Login/Register - CORREGIDO: weight dentro de Modifier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)  // ← CORREGIDO: así se usa weight
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFF1FB8B9) else Color.LightGray,
                            contentColor = if (selectedTab == 0) Color.White else Color.Black
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Iniciar Sesión", fontSize = 14.sp)
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)  // ← CORREGIDO: así se usa weight
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) Color(0xFF1FB8B9) else Color.LightGray,
                            contentColor = if (selectedTab == 1) Color.White else Color.Black
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Registrarse", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Campos según la pestaña seleccionada
                if (selectedTab == 0) {
                    // LOGIN - Solo email y contraseña
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF1FB8B9)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF1FB8B9)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1FB8B9),
                            focusedLabelColor = Color(0xFF1FB8B9)
                        )
                    )
                } else {
                    // REGISTER - Todos los campos
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF1FB8B9)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1FB8B9),
                            focusedLabelColor = Color(0xFF1FB8B9)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF1FB8B9)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF1FB8B9)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1FB8B9),
                            focusedLabelColor = Color(0xFF1FB8B9)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF1FB8B9)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF1FB8B9)
                                )
                            }
                        },
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            { Text("Las contraseñas no coinciden", color = Color.Red, fontSize = 12.sp) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1FB8B9),
                            focusedLabelColor = Color(0xFF1FB8B9),
                            errorBorderColor = Color.Red,
                            errorLabelColor = Color.Red
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón principal según la pestaña
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            // Validar login
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                // Aquí iría la lógica de login
                                Toast.makeText(context, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Validar registro
                            if (username.isNotEmpty() && email.isNotEmpty() &&
                                password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                                if (password == confirmPassword) {
                                    // Aquí iría la lógica de registro
                                    Toast.makeText(context, "Registrando usuario...", Toast.LENGTH_SHORT).show()
                                    onRegisterSuccess()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FB8B9)
                    )
                ) {
                    Text(if (selectedTab == 0) "Iniciar Sesión" else "Registrarse")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        }
    }
}