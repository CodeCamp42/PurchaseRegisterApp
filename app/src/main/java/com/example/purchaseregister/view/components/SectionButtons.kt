package com.example.purchaseregister.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.purchaseregister.viewmodel.Section

@Composable
fun SectionButtons(
    sectionActive: Section,
    onSectionChange: (Section) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onSectionChange(Section.PURCHASES) },
            modifier = Modifier
                .weight(1f)
                .height(45.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sectionActive == Section.PURCHASES) Color(0xFFFF5A00) else Color.Gray
            )
        ) {
            Text("Compras", style = MaterialTheme.typography.titleMedium)
        }
        Button(
            onClick = { onSectionChange(Section.SALES) },
            modifier = Modifier
                .weight(1f)
                .height(45.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sectionActive == Section.SALES) Color(0xFFFF5A00) else Color.Gray
            )
        ) {
            Text("Ventas", style = MaterialTheme.typography.titleMedium)
        }
    }
}