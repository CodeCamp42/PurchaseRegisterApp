package com.example.purchaseregister.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialSunatDialog(
    onDismiss: () -> Unit,
    onCredentialsObtained: (clientId: String, clientSecret: String) -> Unit,
    prefillClientId: String = "",
    prefillClientSecret: String = ""
) {
    var currentStep by remember { mutableStateOf(0) }
    var clientId by remember { mutableStateOf(prefillClientId) }
    var clientSecret by remember { mutableStateOf(prefillClientSecret) }
    var showCopiedMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paso ${currentStep + 1} de 4",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = (currentStep + 1) / 4f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF1FB8B9),
                    trackColor = Color.LightGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (currentStep) {
                    0 -> Step1EnterPortal()
                    1 -> Step2NavigateMenu()
                    2 -> Step3ConfigureApp()
                    3 -> Step4CopyCredentials(
                        clientId = clientId,
                        clientSecret = clientSecret,
                        onClientIdChange = { clientId = it },
                        onClientSecretChange = { clientSecret = it }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        TextButton(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF1FB8B9)
                            )
                        ) {
                            Text("← Anterior", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier)
                    }

                    if (currentStep < 3) {
                        TextButton(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF1FB8B9)
                            )
                        ) {
                            Text("Siguiente →", fontSize = 12.sp)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                if (clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
                                    onCredentialsObtained(clientId, clientSecret)
                                    onDismiss()
                                }
                            },
                            enabled = clientId.isNotEmpty() && clientSecret.isNotEmpty(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (clientId.isNotEmpty() && clientSecret.isNotEmpty())
                                    Color(0xFF1FB8B9)
                                else
                                    Color.Gray
                            )
                        ) {
                            Text("✓ Guardar Credenciales", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step1EnterPortal() {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Paso 1: Ingresa al Portal SOL",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(24.dp))

        InstructionCard(
            number = "1",
            text = "Abre el Portal SOL de SUNAT haciendo clic en el botón de abajo"
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstructionCard(
            number = "2",
            text = "Inicia sesión con tu RUC, Usuario SOL y Clave SOL"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://e-menu.sunat.gob.pe/cl-ti-itmenu/MenuInternet.htm")
                )
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("ABRIR PORTAL SOL")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "⚠️ Mantén esta ventana abierta, volverás aquí después de iniciar sesión",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Step2NavigateMenu() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Paso 2: Sigue esta ruta exacta",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                RouteItem(
                    number = "1",
                    prefix = "Haz clic en ",
                    highlighted = "EMPRESAS",
                    icon = Icons.Default.Business
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF1FB8B9), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(0xFF1FB8B9),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Luego en ",
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Credenciales de API SUNAT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1FB8B9),
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF1FB8B9), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "3",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF1FB8B9),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finalmente en ",
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Gestión Credenciales de API SUNAT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1FB8B9),
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¿No encuentras la opción?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Busca en el menú de la izquierda después de hacer clic en EMPRESAS",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Step3ConfigureApp() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Paso 3: Configura tu aplicación",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📝 Completa así:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Nombre: CasaMarket",
                    fontSize = 14.sp
                )

                Text(
                    text = "• URL: https://casamarket.com",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✅ PERMISO OBLIGATORIO:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1FB8B9)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MIGE RCE y RVIE – SIRE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GRE Emison de Comprobantes",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Alcance: Web (o Desktop)",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "⚠️ IMPORTANTE: Después de guardar, SUNAT generará tus credenciales",
            fontSize = 12.sp,
            color = Color(0xFFF57C00),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Step4CopyCredentials(
    clientId: String,
    clientSecret: String,
    onClientIdChange: (String) -> Unit,
    onClientSecretChange: (String) -> Unit
) {
    var showClientSecret by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Paso 4: Copia tus credenciales",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1FB8B9)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Después de guardar, SUNAT te mostrará dos códigos.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Cópialos aquí:",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = clientId,
            onValueChange = onClientIdChange,
            label = { Text("Client ID") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = clientSecret,
            onValueChange = onClientSecretChange,
            label = { Text("Client Secret") },
            placeholder = { Text("Tu clave secreta") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showClientSecret) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showClientSecret = !showClientSecret }) {
                    Icon(
                        imageVector = if (showClientSecret) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showClientSecret) "Ocultar" else "Mostrar"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Text(
                text = "🔐 Estos códigos son únicos y secretos. No los compartas con nadie.",
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp),
                color = Color(0xFFE65100)
            )
        }
    }
}

@Composable
fun InstructionCard(
    number: String,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF1FB8B9), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun RouteItem(
    number: String,
    prefix: String,
    highlighted: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF1FB8B9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1FB8B9),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = prefix,
            fontSize = 14.sp
        )
        Text(
            text = highlighted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1FB8B9)
        )
    }
}