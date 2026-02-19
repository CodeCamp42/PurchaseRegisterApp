package com.example.purchaseregister.view.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.utils.DateRangeSelector

@Composable
fun DateFilterSection(
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    onDateRangeClick: () -> Unit,
    onDetailAllClick: () -> Unit,
    hasInvoicesInProcess: Boolean,
    isDetailingAll: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDetailAllClick,
                modifier = Modifier.size(40.dp),
                enabled = true
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Detallar todas las facturas",
                    tint = if (hasInvoicesInProcess || isDetailingAll)
                        Color.Gray  // GRIS si hay procesos
                    else
                        Color(0xFF1FB8B9),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Detallar",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = Color(0xFF1FB8B9),
                    lineHeight = 16.sp
                )
                Text(
                    text = "todas",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = Color(0xFF1FB8B9),
                    lineHeight = 16.sp
                )
            }
        }

        DateRangeSelector(
            selectedStartMillis = selectedStartMillis,
            selectedEndMillis = selectedEndMillis,
            onDateRangeClick = onDateRangeClick,
            modifier = Modifier
        )
    }
}