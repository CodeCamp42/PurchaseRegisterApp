package com.example.purchaseregister.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Componente para celdas de cabecera
@Composable
fun HeaderCell(text: String, width: Dp) {
    Text(
        text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

// Componente para celdas simples
@Composable
fun SimpleTableCell(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun InvoiceStatusCell(
    estado: String,
    modifier: Modifier = Modifier
) {
    Text(
        estado,
        modifier = modifier,
        fontSize = 10.sp,
        color = when (estado) {
            "CONSULTADO" -> Color(0xFF2196F3)
            "CON DETALLE" -> Color(0xFFFF5A00)
            "REGISTRADO" -> Color(0xFF4CAF50)
            else -> Color.Gray
        },
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun InvoiceStatusCircle(
    estado: String,
    tamano: Dp = 20.dp
) {
    val color = when (estado.uppercase()) {
        "CONSULTADO" -> Color(0xFF2196F3) // Azul/verde
        "CON DETALLE" -> Color(0xFFFF5A00) // Naranja
        "REGISTRADO" -> Color(0xFF4CAF50) // Verde
        else -> Color.Gray // Gris
    }

    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(color)
    )
}