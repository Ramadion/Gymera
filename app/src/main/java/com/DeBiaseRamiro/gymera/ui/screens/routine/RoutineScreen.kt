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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    routine: Routine,
    onDaySelected: (dayId: String) -> Unit,
    onGenerateNew: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                    onGenerateNew()
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
                            text = routine.goal.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = MutedGray
                        )
                    }
                },
                actions = {
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                RoutineSummaryHeader(routine = routine)
            }
            itemsIndexed(routine.workoutDays) { _, day ->
                WorkoutDayCard(
                    day = day,
                    onClick = {
                        if (!day.isRestDay) onDaySelected(day.id)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RoutineSummaryHeader(routine: Routine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(label = routine.level.lowercase().replaceFirstChar { it.uppercase() })
        SummaryChip(label = "${routine.daysPerWeek} días/sem")
    }
}

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

@Composable
private fun WorkoutDayCard(
    day: WorkoutDay,
    onClick: () -> Unit
) {
    val cardBackground = if (day.isRestDay) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!day.isRestDay) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (day.isRestDay) 0.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Columna izquierda: ícono + textos ─────────────────────────────
            // FIX: weight(1f) le dice a Compose que esta Row ocupa el espacio
            // disponible MENOS lo que necesita la columna derecha.
            // Sin weight, la Row pedía todo el espacio que quisiera y el Text
            // nunca tenía un ancho máximo contra el cual aplicar el ellipsis.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ícono: pesas o luna
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
                        contentDescription = if (day.isRestDay)
                            "Día de descanso"
                        else
                            "Día de entrenamiento",
                        tint = if (day.isRestDay) MutedGray else PurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Textos — ahora sí tienen un ancho máximo definido por el weight
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = day.dayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isRestDay) MutedGray else OnBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (day.isRestDay) "Descanso" else day.muscleFocus,
                        fontSize = 13.sp,
                        color = MutedGray,
                        // maxLines = 1 + Ellipsis: ahora sí funciona porque
                        // el Column padre tiene width acotado por los dos weight(1f)
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Columna derecha: cantidad de ejercicios ───────────────────────
            // No tiene weight → toma solo el espacio que necesita.
            // La Row izquierda con weight(1f) cede el resto.
            if (!day.isRestDay) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
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