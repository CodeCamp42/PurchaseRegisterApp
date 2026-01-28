package com.example.purchaseregister.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReadOnlyField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    isSingleLine: Boolean = true
) {
    Column(modifier = modifier.height(IntrinsicSize.Min)) {
        if (label.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
                    .border(
                        border = BorderStroke(1.dp, Color.LightGray),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 10.sp,
                textAlign = if (isSingleLine) TextAlign.Center else TextAlign.Start
            ),
            shape = if (label.isNotEmpty())
                RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            else RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = if (isHighlight) Color(0xFFFF5A00) else Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            singleLine = isSingleLine,
            maxLines = if (isSingleLine) 1 else 3
        )
    }
}