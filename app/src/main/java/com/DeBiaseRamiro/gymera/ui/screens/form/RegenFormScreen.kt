package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.ui.theme.*

@Composable
fun RegenFormScreen(
    onRegenCompleted: (UserProfile) -> Unit,
    onBack: () -> Unit,
    viewModel: RegenFormViewModel = hiltViewModel()
) {
    val goal        by viewModel.goal.collectAsState()
    val days        by viewModel.daysPerWeek.collectAsState()
    val duration    by viewModel.sessionDuration.collectAsState()
    val level       by viewModel.level.collectAsState()
    val limitations by viewModel.limitations.collectAsState()
    val canGenerate by viewModel.canGenerate.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text       = "Generar nueva rutina",
                color      = OnBackground,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = "Los datos físicos (peso, altura, edad, género) se toman de tu perfil.",
                color    = MutedGray,
                fontSize = 14.sp
            )
            Text(
                text     = "Si querés actualizarlos, editá tu perfil en la pestaña Perfil.",
                color    = MutedGray,
                fontSize = 14.sp
            )

            RegenOptionGroup(
                label   = "Objetivo",
                options = listOf(
                    "Pérdida de peso"   to "WEIGHT_LOSS",
                    "Ganancia muscular" to "MUSCLE_GAIN",
                    "Resistencia"       to "ENDURANCE",
                    "Tonificación"      to "TONING"
                ),
                selected = goal,
                onSelect = viewModel::setGoal
            )

            RegenOptionGroup(
                label   = "Días por semana",
                options = listOf(
                    3 to "3 días",
                    4 to "4 días",
                    5 to "5 días",
                    6 to "6 días"
                ),
                selected = days,
                onSelect = viewModel::setDaysPerWeek
            )

            RegenOptionGroup(
                label   = "Tiempo por sesión",
                options = listOf(
                    30 to "30 minutos",
                    45 to "45 minutos",
                    60 to "60 minutos",
                    90 to "90 minutos"
                ),
                selected = duration,
                onSelect = viewModel::setSessionDuration
            )

            RegenOptionGroup(
                label   = "Nivel de experiencia",
                options = listOf(
                    "Principiante" to "BEGINNER",
                    "Intermedio"   to "INTERMEDIATE",
                    "Avanzado"     to "ADVANCED"
                ),
                selected = level,
                onSelect = viewModel::setLevel
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text       = "Indicaciones, limitaciones o molestias para la IA",
                    color      = OnBackground,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value         = limitations,
                    onValueChange = viewModel::setLimitations,
                    placeholder   = { Text("Ej: dolor de rodilla, hernia de disco...", color = MutedGray) },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PurplePrimary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedTextColor     = OnBackground,
                        unfocusedTextColor   = OnBackground
                    )
                )
                Text(
                    text     = "Si no tenés ninguna, dejalo vacío.",
                    color    = MutedGray,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick  = { onRegenCompleted(viewModel.buildProfile()) },
                enabled  = canGenerate,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary,
                    contentColor   = OnBackground
                )
            ) {
                Text("Generar con IA 💪", fontSize = 16.sp)
            }

            TextButton(
                onClick  = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver", color = MutedGray)
            }
        }
    }
}

@Composable
private fun <T> RegenOptionGroup(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = label,
            color      = OnBackground,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        options.forEach { (value, display) ->
            val isSelected = selected == value
            OutlinedButton(
                onClick  = { onSelect(value) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) PurplePrimary else SurfaceVariant
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) PurplePrimary.copy(alpha = 0.2f) else Color.Transparent,
                    contentColor   = OnBackground
                )
            ) {
                Text(
                    text  = display,
                    color = if (isSelected) PurplePrimary else MutedGray
                )
            }
        }
    }
}