package com.example.purchaseregister.view.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purchaseregister.utils.DateRangeSelector

@Composable
fun DateFilterSection(
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    onDateRangeClick: () -> Unit,
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
        }

        DateRangeSelector(
            selectedStartMillis = selectedStartMillis,
            selectedEndMillis = selectedEndMillis,
            onDateRangeClick = onDateRangeClick,
            modifier = Modifier
        )
    }
}