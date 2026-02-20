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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.purchaseregister.viewmodel.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isLoggedIn: Boolean,
    currentUsername: String? = null,
    currentEmail: String? = null,
    loginState: AuthState,
    registerState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onResetStates: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isValidating by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configurar propiedades del diálogo según el estado
    val dialogProperties = if (isLoggedIn) {
        // Usuario logueado - puede cerrar haciendo clic fuera
        DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    } else {
        // Login/Registro - NO puede cerrar haciendo clic fuera
        DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = dialogProperties
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    max = if (isLoggedIn) 280.dp else 600.dp
                ),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (isLoggedIn) {
                // VISTA DE PERFIL INFORMATIVO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono de perfil
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF1FB8B9)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Bienvenido!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1FB8B9)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información del usuario
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Nombre de usuario
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF1FB8B9),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentUsername ?: "Usuario",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                // VISTA DE LOGIN/REGISTRO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedTab = 0
                                localError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) Color(0xFF1FB8B9) else Color.LightGray,
                                contentColor = if (selectedTab == 0) Color.White else Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Iniciar Sesión", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                selectedTab = 1
                                localError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 1) Color(0xFF1FB8B9) else Color.LightGray,
                                contentColor = if (selectedTab == 1) Color.White else Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Registrarse", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTab == 0) {
                        // LOGIN
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localError = null
                            },
                            label = { Text("Correo electrónico", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localError = null
                            },
                            label = { Text("Contraseña", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onForgotPasswordClick,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    "¿Olvidaste tu contraseña?",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1FB8B9)
                                )
                            }
                        }
                    } else {
                        // REGISTER
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Nombre de usuario", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar contraseña", fontSize = 12.sp) },
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
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }

                    localError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (isValidating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF1FB8B9),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validando credenciales...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BOTONES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedTab == 0) {
                                    // Validar login
                                    if (email.isEmpty() || password.isEmpty()) {
                                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // LLAMAR AL VIEWMODEL - NO al mock
                                    onLogin(email, password)
                                } else {
                                    // Validar registro
                                    if (username.isEmpty() || email.isEmpty() ||
                                        password.isEmpty() || confirmPassword.isEmpty()) {
                                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // LLAMAR AL VIEWMODEL - NO al mock
                                    onRegister(username, email, password)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1FB8B9)
                            ),
                            enabled = !(loginState is AuthState.Loading || registerState is AuthState.Loading)
                        ) {
                            if (loginState is AuthState.Loading || registerState is AuthState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (selectedTab == 0) "Iniciar Sesión" else "Registrarse",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                contentColor = Color.Black
                            ),
                            enabled = !(loginState is AuthState.Loading || registerState is AuthState.Loading)
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}