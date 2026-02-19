package com.example.purchaseregister.view.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purchaseregister.viewmodel.Section

@Composable
fun BottomActionButtons(
    isAppLoggedIn: Boolean,
    hasSunatCredentials: Boolean,
    onConsultClick: () -> Unit,
    onShowProfile: () -> Unit,
    onShowCredentials: () -> Unit,
    sectionActive: Section,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current  // 👈 Contexto local del componente

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (!isAppLoggedIn) {
                    Toast.makeText(context, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
                    onShowProfile()
                } else if (!hasSunatCredentials) {
                    Toast.makeText(context, "Debes configurar tus credenciales SUNAT", Toast.LENGTH_SHORT).show()
                    onShowCredentials()
                } else {
                    onConsultClick()
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(45.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
        ) {
            Text("Consultar")
        }

        if (sectionActive == Section.PURCHASES) {
            Button(
                onClick = {
                    if (!isAppLoggedIn) {
                        Toast.makeText(context, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
                        onShowProfile()
                    } else if (!hasSunatCredentials) {
                        Toast.makeText(context, "Debes configurar tus credenciales SUNAT", Toast.LENGTH_SHORT).show()
                        onShowCredentials()
                    } else {
                        onNavigateToRegister()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
            ) {
                Text("Subir Factura")
            }
        }
    }
}