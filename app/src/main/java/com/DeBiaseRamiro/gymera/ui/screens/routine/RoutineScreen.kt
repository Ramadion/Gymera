package com.DeBiaseRamiro.gymera.ui.screens.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.theme.*

/**
 * RoutineScreen — Pantalla de Rutina Semanal (CU-04)
 *
 * Muestra los 7 días de la semana en un LazyColumn.
 * Cada día es una Card clickeable que navega al Detalle del Día.
 * Los días de descanso se muestran con estilo diferenciado.
 *
 * @param routine         La rutina generada por Gemini (viene del SharedRoutineViewModel)
 * @param onDaySelected   Callback cuando el usuario toca un día → navega a DayDetail
 * @param onGenerateNew   Callback cuando el usuario quiere generar una nueva rutina
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    routine: Routine,
    onDaySelected: (dayId: String) -> Unit,
    onGenerateNew: () -> Unit
) {
    // Estado para el diálogo de confirmación de "Generar nueva rutina" (CU-07)
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación antes de borrar la rutina actual
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "¿Generar nueva rutina?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tu rutina actual se reemplazará con una nueva generada por IA.",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onGenerateNew()   // Navega al formulario
                }) {
                    Text("Sí, generar nueva", color = PurplePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    Scaffold(
        // TopAppBar con el título y el botón de nueva rutina
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tu Plan Semanal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Text(
                            // Muestra el objetivo de la rutina como subtítulo
                            text = routine.goal.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = MutedGray
                        )
                    }
                },
                actions = {
                    // Botón para generar nueva rutina — muestra diálogo de confirmación
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generar nueva rutina",
                            tint = PurplePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Espaciado arriba y abajo de la lista
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Encabezado con resumen rápido de la rutina
            item {
                RoutineSummaryHeader(routine = routine)
            }

            // Una Card por cada día de la semana
            itemsIndexed(routine.workoutDays) { _, day ->
                WorkoutDayCard(
                    day = day,
                    onClick = {
                        // Solo los días de entrenamiento son navegables
                        if (!day.isRestDay) {
                            onDaySelected(day.id)
                        }
                    }
                )
            }

            // Espaciado inferior para no quedar pegado al borde
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * RoutineSummaryHeader
 *
 * Chip-row con información resumida de la rutina:
 * nivel, días por semana, objetivo.
 * Se muestra arriba de la lista de días.
 */
@Composable
private fun RoutineSummaryHeader(routine: Routine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip: nivel
        SummaryChip(
            label = routine.level.lowercase().replaceFirstChar { it.uppercase() }
        )
        // Chip: días de entrenamiento
        SummaryChip(
            label = "${routine.daysPerWeek} días/sem"
        )
    }
}

/**
 * SummaryChip
 *
 * Pequeño chip de información — Material Design 3 AssistChip estilo custom.
 */
@Composable
private fun SummaryChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = PurplePrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * WorkoutDayCard
 *
 * Card que representa un día de la semana.
 * - Día de entrenamiento: muestra ícono de pesas, músculo foco, cantidad de ejercicios.
 * - Día de descanso: muestra ícono de luna, texto "Descanso", fondo más oscuro.
 *
 * @param day     Datos del día (WorkoutDay)
 * @param onClick Callback al tocar la card (solo días de entrenamiento)
 */
@Composable
private fun WorkoutDayCard(
    day: WorkoutDay,
    onClick: () -> Unit
) {
    // El color de fondo cambia según si es descanso o entrenamiento
    val cardBackground = if (day.isRestDay) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Solo hacemos clickeable si es día de entrenamiento
            .then(
                if (!day.isRestDay) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (day.isRestDay) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Columna izquierda: ícono + nombre del día ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ícono: pesas o luna según el tipo de día
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (day.isRestDay) MutedGray.copy(alpha = 0.15f)
                            else PurplePrimary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (day.isRestDay)
                            Icons.Default.NightlightRound
                        else
                            Icons.Default.FitnessCenter,
                        contentDescription = if (day.isRestDay) "Día de descanso" else "Día de entrenamiento",
                        tint = if (day.isRestDay) MutedGray else PurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    // Nombre del día (Lunes, Martes, etc.)
                    Text(
                        text = day.dayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isRestDay) MutedGray else OnBackground
                    )
                    // Subtítulo: músculo foco o "Descanso activo"
                    Text(
                        text = if (day.isRestDay) "Descanso" else day.muscleFocus,
                        fontSize = 13.sp,
                        color = MutedGray
                    )
                }
            }

            // --- Columna derecha: cantidad de ejercicios o texto Descanso ---
            if (!day.isRestDay) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${day.exercises.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                    Text(
                        text = if (day.exercises.size == 1) "ejercicio" else "ejercicios",
                        fontSize = 11.sp,
                        color = MutedGray
                    )
                }
            }
        }
    }
}