package com.DeBiaseRamiro.gymera.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.DeBiaseRamiro.gymera.ui.theme.MutedGray
import com.DeBiaseRamiro.gymera.ui.theme.PurplePrimary
import com.DeBiaseRamiro.gymera.ui.theme.SurfaceDark
import com.DeBiaseRamiro.gymera.ui.theme.SurfaceVariant
import com.DeBiaseRamiro.gymera.ui.theme.OnBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── Componentes compartidos entre FormScreen y ProfileScreen ──────────────────
// Extraen el DatePickerDialog y los campos peso/altura que ambos pantallas
// duplicaban de forma idéntica (mismos colores, validación de longitud y teclados).

// Paleta oscura compartida para ambos OutlinedTextField.
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = PurplePrimary,
    unfocusedBorderColor    = SurfaceVariant,
    focusedTextColor        = OnBackground,
    unfocusedTextColor      = OnBackground,
    cursorColor             = PurplePrimary,
    focusedContainerColor   = SurfaceDark,
    unfocusedContainerColor = SurfaceDark
)

// "23/08/2026" — formato compartido por ambas pantallas al confirmar fecha.
// IMPORTANTE: el DatePicker retorna midnight UTC del día seleccionado; forzamos
// timezone UTC para que no se retroceda un día en zonas como Argentina (UTC-3).
fun formatDateShort(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

// Diálogo de fecha con el look oscuro de Gymera. El estado del calendario lo
// maneja el caller; onConfirm entrega el millis seleccionado (o nada si cerró).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymeraDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: (millis: Long) -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onConfirm)
                    onDismiss()
                }
            ) { Text("Confirmar", color = PurplePrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MutedGray)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = SurfaceDark)
    ) {
        DatePicker(
            state  = state,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor   = PurplePrimary,
                todayDateBorderColor        = PurplePrimary,
                containerColor              = SurfaceDark
            )
        )
    }
}

// Campos de peso y altura con la misma validación en ambas pantallas:
// peso ≤5 caracteres (teclado decimal), altura ≤3 caracteres (teclado numérico).
// Estado hoisteado — cada pantalla guarda y parsea sus valores como prefiera.
@Composable
fun WeightHeightFields(
    weightInput: String,
    heightInput: String,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    showPlaceholders: Boolean = false,
    spacing: Dp = 12.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        OutlinedTextField(
            value         = weightInput,
            onValueChange = { if (it.length <= 5) onWeightChange(it) },
            label         = { Text("Peso", color = MutedGray) },
            suffix        = { Text("kg", color = MutedGray) },
            placeholder   = { if (showPlaceholders) Text("Ej: 75", color = MutedGray) else null },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape  = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        OutlinedTextField(
            value         = heightInput,
            onValueChange = { if (it.length <= 3) onHeightChange(it) },
            label         = { Text("Altura", color = MutedGray) },
            suffix        = { Text("cm", color = MutedGray) },
            placeholder   = { if (showPlaceholders) Text("Ej: 175", color = MutedGray) else null },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape  = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
    }
}
