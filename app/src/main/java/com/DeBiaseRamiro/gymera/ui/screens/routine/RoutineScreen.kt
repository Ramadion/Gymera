package com.DeBiaseRamiro.gymera.ui.screens.routine

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoutineScreen(
    routine: Routine,
    onDaySelected: (dayId: String) -> Unit,
    onGenerateNew: () -> Unit,
    onSetDayAsRest: (dayId: String, isRest: Boolean) -> Unit,
    onClearExercises: (dayId: String) -> Unit,
    onMoveExercisesToRestDay: (fromDayId: String, toDayId: String) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    // ── Gestos de días ────────────────────────────────────────────────────
    // Igual que en DayDetail: al deslizar guardamos el día objetivo y el
    // SwipeToDismissBox rebota (confirmValueChange = false). El AlertDialog
    // pide confirmación antes de llamar al repositorio.
    var pendingSetRest    by remember { mutableStateOf<WorkoutDay?>(null) }
    var pendingActivate   by remember { mutableStateOf<WorkoutDay?>(null) }
    var pendingAction     by remember { mutableStateOf<WorkoutDay?>(null) }
    var showMoveTarget    by remember { mutableStateOf(false) }

    // Cualquier día de descanso (excepto el día sobre el que se está actuando)
    // sirve de destino para mover ejercicios. No se exige que esté vacío: si ya
    // tiene ejercicios ocultos, los movidos se agregan y se reordenan junto a ellos.
    val moveTargets = routine.workoutDays.filter {
        it.isRestDay && it.id != pendingAction?.id
    }
    val canMoveExercises = moveTargets.isNotEmpty()

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

    // ── Confirmación: poner día como descanso ─────────────────────────────
    if (pendingSetRest != null) {
        AlertDialog(
            onDismissRequest = { pendingSetRest = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "¿Poner ${pendingSetRest!!.dayName} como descanso?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sus ejercicios se guardarán y volverán si lo activás de nuevo.",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetDayAsRest(pendingSetRest!!.id, true)
                    pendingSetRest = null
                }) {
                    Text("Sí, descansar", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSetRest = null }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // ── Confirmación: activar día de descanso ─────────────────────────────
    if (pendingActivate != null) {
        AlertDialog(
            onDismissRequest = { pendingActivate = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "¿Sumar ${pendingActivate!!.dayName} a tu rutina?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Dejará de ser día de descanso y podrás modificar sus ejercicios.",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetDayAsRest(pendingActivate!!.id, false)
                    pendingActivate = null
                }) {
                    Text("Sí, activar", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActivate = null }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // ── Diálogo al toque prolongado (mover / borrar ejercicios) ──────────
    // ya no elimina el día de la semana: solo mueve los ejercicios a un día
    // de descanso o los borra, dejando siempre el día existente.
    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "¿Qué hacés con ${pendingAction!!.dayName}?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = pendingAction!!.exercises.size.run {
                        if (this == 1) "Tiene 1 ejercicio." else "Tiene $this ejercicios."
                    },
                    color = MutedGray
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (canMoveExercises) {
                        TextButton(onClick = { showMoveTarget = true }) {
                            Text(
                                "Mover ejercicios a un día de descanso…",
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TextButton(onClick = {
                        onClearExercises(pendingAction!!.id)
                        pendingAction = null
                    }) {
                        Text(
                            "Borrar todos los ejercicios",
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // ── Sub-diálogo: elegir el día de descanso destino ────────────────────
    if (showMoveTarget && pendingAction != null) {
        AlertDialog(
            onDismissRequest = { showMoveTarget = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "¿Mover ejercicios a qué día?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "El día elegido quedará activo y recibirá los ejercicios. " +
                            "${pendingAction!!.dayName} quedará como día de descanso.",
                        color = MutedGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    moveTargets.forEach { target ->
                        TextButton(
                            onClick = {
                                onMoveExercisesToRestDay(pendingAction!!.id, target.id)
                                showMoveTarget = false
                                pendingAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = target.dayName,
                                color = PurplePrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveTarget = false }) {
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
                Text(
                    text = "Deslizá: ◀ descanso • ▶ activar  •  mantené presionado para mover o borrar ejercicios",
                    color    = MutedGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            item {
                RoutineSummaryHeader(routine = routine)
            }
            itemsIndexed(routine.workoutDays, key = { _, day -> day.id }) { _, day ->
                SwipeableWorkoutDayCard(
                    day = day,
                    onClick = {
                        if (!day.isRestDay) onDaySelected(day.id)
                    },
                    onSetRest = { pendingSetRest = day },
                    onActivate = { pendingActivate = day },
                    onDelete = { if (day.exercises.isNotEmpty()) pendingAction = day }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── SwipeableWorkoutDayCard ──────────────────────────────────────────────────
// Envuelve el día en un SwipeToDismissBox direccional:
//   • día de entrenamiento → swip-left (EndToStart) propone ponerlo en descanso
//   • día de descanso      → swipe-right (StartToEnd) propone activarlo
// El toque prolongado dispara el diálogo de mover/borrar ejercicios.
//
// FIX: rememberSwipeToDismissBoxState captura su confirmValueChange en la
// primera composición (rememberSaveable). Si el día cambia entrenamiento↔descanso
// con el MISMO estado, el lambda queda apuntando al day viejo y el swipe opuesto
// muere. key(day.isRestDay) obliga a recrear el estado (Settled) cada vez que el
// tipo del día cambia, así el swipe derecha para activar SIEMPRE funciona.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableWorkoutDayCard(
    day: WorkoutDay,
    onClick: () -> Unit,
    onSetRest: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    key(day.isRestDay) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (!day.isRestDay && value == SwipeToDismissBoxValue.EndToStart) {
                    onSetRest()   // muestra el diálogo de descanso
                    false         // rebota — no confirma aún
                } else if (day.isRestDay && value == SwipeToDismissBoxValue.StartToEnd) {
                    onActivate()  // muestra el diálogo de activar
                    false
                } else false
            },
            positionalThreshold = { it * 0.4f }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = day.isRestDay,
            enableDismissFromEndToStart = !day.isRestDay,
            backgroundContent = {
                if (day.isRestDay) {
                    // Día de descanso: el swipe funciona igual, pero el fondo
                    // queda neutro y sin ícono para no superponerse a la tarjeta.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PurplePrimary),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Descanso",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        ) {
            WorkoutDayCard(
                day = day,
                onClick = onClick,
                onLongClick = onDelete
            )
        }
    }
}

@Composable
private fun RoutineSummaryHeader(routine: Routine) {
    val trainingDays = routine.workoutDays.count { !it.isRestDay }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(label = routine.level.lowercase().replaceFirstChar { it.uppercase() })
        SummaryChip(label = "$trainingDays días/sem")
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardBackground = if (day.isRestDay) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
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

                // Textos
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Columna derecha: cantidad de ejercicios ───────────────────────
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