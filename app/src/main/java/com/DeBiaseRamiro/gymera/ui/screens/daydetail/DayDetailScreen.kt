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
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ── Extensión para encodear strings de forma segura antes de meterlos en una URL de navegación
// Se define a nivel de archivo para que tanto DayDetailScreen como cualquier composable del
// archivo puedan usarla sin necesidad de importar nada extra
// Reemplazá la extensión encodeForNav() existente por esta versión
fun String.encodeForNav(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
        .replace("+", "%20") // los espacios deben ser %20, no + en path/query de Navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    workoutDay: WorkoutDay,
    // Recibe la ruta completa ya construida — el NavGraph es quien llama a navController.navigate()
    // De esta forma DayDetailScreen no necesita saber nada sobre el NavController
    onExerciseClick: (route: String) -> Unit,
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
                        exercise   = exercise,
                        imageState = imageStates[exercise.id] ?: ExerciseImageState.Loading,
                        onClick    = {
                            // La ruta se construye ACÁ, donde tenemos acceso al objeto exercise
                            // encodeForNav() está disponible porque está definida en este mismo archivo
                            val route = "exercise_detail" +
                                    "?nameEn=${exercise.nameEn.encodeForNav()}" +
                                    "&nameEs=${exercise.name.encodeForNav()}" +
                                    "&sets=${exercise.sets}" +
                                    "&reps=${exercise.reps.encodeForNav()}" +
                                    "&restSeconds=${exercise.restSeconds}" +
                                    "&notes=${exercise.notes.encodeForNav()}"
                            onExerciseClick(route)
                        }
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
    // onClick es un lambda simple — no sabe nada de navegación ni de NavController
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // limpio: delega al caller
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

            // ── Imagen del ejercicio ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (val state = imageState) {
                    is ExerciseImageState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    is ExerciseImageState.Success -> {
                        GlideImage(
                            model = state.imageUrl,
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is ExerciseImageState.Error -> {
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
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                Text(
                    text = exercise.muscleGroup,
                    fontSize = 13.sp,
                    color = PurplePrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseChip("${exercise.sets}x${exercise.reps}")
                    ExerciseChip("${exercise.restSeconds}s")
                }
            }

            // Flecha indicadora de navegación
            Text(text = "›", fontSize = 24.sp, color = MutedGray)
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