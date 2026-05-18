package com.example.purchaseregister.view.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusLegend(
    totalInvoices: Int,
    readyInvoices: Int,
    pendingInvoices: Int,
    isDownloadButtonEnabled: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA)),
    ) {
        // Primera fila con 3 estados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                )
                Text(
                    text = "CONSULTADO",
                    fontSize = 9.sp,
                    color = Color.Black
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                Text(
                    text = "EN PROCESO",
                    fontSize = 9.sp,
                    color = Color.Black
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5A00))
                )
                Text(
                    text = "CON DETALLE",
                    fontSize = 9.sp,
                    color = Color.Black
                )
            }
        }

        // Segunda fila con REGISTRADO y botón Filtrar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    text = "REGISTRADO",
                    fontSize = 9.sp,
                    color = Color.Black
                )
            }

            Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .height(40.dp)
                    .width(180.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1FB8B9)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Descargar Facturas",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}