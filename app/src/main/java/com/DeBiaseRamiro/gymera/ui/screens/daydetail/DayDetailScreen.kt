package com.DeBiaseRamiro.gymera.ui.screens.daydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    workoutDay: WorkoutDay,
    onExerciseClick: (exerciseId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    val imageStates by viewModel.imageStates.collectAsState()

    // Disparamos la carga de imágenes al entrar a la pantalla
    LaunchedEffect(workoutDay.id) {
        viewModel.loadImages(workoutDay.exercises)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = OnBackground
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = workoutDay.dayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        if (workoutDay.muscleFocus.isNotBlank()) {
                            Text(
                                text = workoutDay.muscleFocus,
                                fontSize = 12.sp,
                                color = PurplePrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        if (workoutDay.exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay ejercicios para este día.",
                    color = MutedGray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${workoutDay.exercises.size} " +
                                if (workoutDay.exercises.size == 1) "ejercicio" else "ejercicios",
                        color = MutedGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(workoutDay.exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise    = exercise,
                        imageState  = imageStates[exercise.id] ?: ExerciseImageState.Loading,
                        onClick     = { onExerciseClick(exercise.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    imageState: ExerciseImageState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Imagen del ejercicio (GIF) ─────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (val state = imageState) {
                    is ExerciseImageState.Loading -> {
                        // Spinner mientras carga
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    is ExerciseImageState.Success -> {
                        // GIF cargado con Glide
                        GlideImage(
                            model = state.imageUrl,
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is ExerciseImageState.Error -> {
                        // Emoji fallback si no se pudo cargar
                        Text(
                            text = "💪",
                            fontSize = 32.sp
                        )
                    }
                }
            }

            // ── Info del ejercicio ─────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Nombre en español
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                // Músculo objetivo
                Text(
                    text = exercise.muscleGroup,
                    fontSize = 13.sp,
                    color = PurplePrimary
                )
                // Chips de series / reps / descanso
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseChip("${exercise.sets}x${exercise.reps}")
                    ExerciseChip("${exercise.restSeconds}s")
                }
            }

            // Flecha de navegación
            Text(text = "›", fontSize = 24.sp, color = MutedGray)
        }

        // Notas de la IA (si existen) — fuera del Row, debajo
        if (exercise.notes.isNotBlank()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = SurfaceVariant
            )
            Text(
                text = "💡 ${exercise.notes}",
                fontSize = 12.sp,
                color = MutedGray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ExerciseChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PurplePrimary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = PurplePrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}