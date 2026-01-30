package com.example.purchaseregister.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.*

// Función de extensión para formatear fechas (igual que en tu código original)
fun Long?.toFormattedDate(): String {
    if (this == null) return ""
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = this
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(calendar.time)
}

// Función auxiliar para obtener la fecha de hoy (mismo que en tu código)
private fun getHoyMillis(): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

// Clase que maneja el estado del diálogo de fecha

class DateRangePickerState(
    initialSelectedStartDateMillis: Long? = null,
    initialSelectedEndDateMillis: Long? = null
) {
    var selectedStartMillis by mutableStateOf<Long?>(initialSelectedStartDateMillis)
    var selectedEndMillis by mutableStateOf<Long?>(initialSelectedEndDateMillis)
    var showDatePicker by mutableStateOf(false)

    // Solo guardamos las fechas iniciales
    private val _initialStart = initialSelectedStartDateMillis ?: getHoyMillis()
    private val _initialEnd = initialSelectedEndDateMillis ?: getHoyMillis()

    fun getInitialStartMillis(): Long = _initialStart
    fun getInitialEndMillis(): Long = _initialEnd

    fun getSelectedDateRangeText(): String {
        return if (selectedStartMillis != null) {
            val startStr = selectedStartMillis!!.toFormattedDate()
            val endStr = selectedEndMillis?.toFormattedDate() ?: startStr
            if (startStr == endStr) startStr else "$startStr - $endStr"
        } else {
            getHoyMillis().toFormattedDate()
        }
    }

    fun onConfirmSelection(
        startMillisFromPicker: Long?,
        endMillisFromPicker: Long?
    ) {
        if (startMillisFromPicker != null) {
            selectedStartMillis = startMillisFromPicker
            selectedEndMillis = endMillisFromPicker
        }
        showDatePicker = false
    }

    fun reset() {
        val hoyMillis = getHoyMillis()
        selectedStartMillis = hoyMillis
        selectedEndMillis = hoyMillis
    }
}

//Función para recordar el estado del DateRangePicker
@Composable
fun rememberCustomDateRangePickerState(
    initialSelectedStartDateMillis: Long? = null,
    initialSelectedEndDateMillis: Long? = null
): DateRangePickerState {
    return remember {
        DateRangePickerState(initialSelectedStartDateMillis, initialSelectedEndDateMillis)
    }
}

// Diálogo de selección de rango de fechas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    state: DateRangePickerState,
    onDismissRequest: () -> Unit = { state.showDatePicker = false },
    title: @Composable () -> Unit = { Text("Selecciona el rango", modifier = Modifier.padding(16.dp)) }
) {
    if (state.showDatePicker) {
        // Creamos el estado del DateRangePicker AQUÍ DENTRO del composable
        val materialDateRangePickerState = androidx.compose.material3.rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.getInitialStartMillis(),
            initialSelectedEndDateMillis = state.getInitialEndMillis()
        )

        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        state.onConfirmSelection(
                            materialDateRangePickerState.selectedStartDateMillis,
                            materialDateRangePickerState.selectedEndDateMillis
                        )
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = "Aceptar",
                        color = Color(0xFF1FB8B9),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "Cancelar",
                        color = Color(0xFFFF5A00),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            DateRangePicker(
                state = materialDateRangePickerState,
                title = title,
                showModeToggle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Componente visual para mostrar el rango de fechas seleccionado
@Composable
fun DateRangeSelector(
    state: DateRangePickerState,
    onDateRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(200.dp)
            .height(45.dp)
            .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
            .clickable { onDateRangeClick() },
        shape = MaterialTheme.shapes.medium,
        color = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = state.getSelectedDateRangeText(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}