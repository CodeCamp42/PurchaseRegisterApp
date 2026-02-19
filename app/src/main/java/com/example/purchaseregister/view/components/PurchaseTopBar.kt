package com.example.purchaseregister.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PurchaseTopBar(
    onProfileClick: () -> Unit,
    isLogoutEnabled: Boolean,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color(0xFF1FB8B9),
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "Registro Contable",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp),
            enabled = isLogoutEnabled
        ) {
            Icon(
                imageVector = Icons.Outlined.PowerSettingsNew,
                contentDescription = "Cerrar sesión",
                tint = if (isLogoutEnabled) Color(0xFF1FB8B9) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}