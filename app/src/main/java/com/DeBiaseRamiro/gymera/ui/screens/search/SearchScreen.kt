package com.DeBiaseRamiro.gymera.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.ui.screens.daydetail.encodeForNav
import com.DeBiaseRamiro.gymera.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onExerciseClick: (route: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val selectedMuscle  by viewModel.selectedMuscle.collectAsState()
    val muscleGroups    by viewModel.muscleGroups.collectAsState()
    val exercises       by viewModel.filteredExercises.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()

    // Controla si el desplegable de músculos está abierto
    var muscleDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explorar ejercicios",
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            // ── Barra de búsqueda ─────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar ejercicio...", color = MutedGray)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MutedGray
                    )
                },
                trailingIcon = {
                    // Botón para limpiar el texto solo si hay algo escrito
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MutedGray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = PurplePrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Botón desplegable de grupo muscular ───────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { muscleDropdownExpanded = !muscleDropdownExpanded },
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMuscle ?: "Grupo muscular — Todos",
                        color = if (selectedMuscle != null) PurplePrimary else MutedGray,
                        fontWeight = if (selectedMuscle != null) FontWeight.SemiBold
                        else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = if (muscleDropdownExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MutedGray
                    )
                }
            }

            // ── Lista de músculos (aparece/desaparece con animación) ──────
            AnimatedVisibility(
                visible = muscleDropdownExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark
                ) {
                    // LazyRow horizontal para los chips de músculos
                    // Más cómodo que un dropdown vertical con 20 opciones
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Opción "Todos" al principio
                        item {
                            MuscleChip(
                                label = "Todos",
                                isSelected = selectedMuscle == null,
                                onClick = {
                                    viewModel.onMuscleSelected(null)
                                    muscleDropdownExpanded = false
                                }
                            )
                        }
                        items(muscleGroups) { muscle ->
                            MuscleChip(
                                label = muscle,
                                isSelected = selectedMuscle == muscle,
                                onClick = {
                                    viewModel.onMuscleSelected(muscle)
                                    muscleDropdownExpanded = false // cierra al seleccionar
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Contador de resultados ────────────────────────────────────
            if (!isLoading) {
                Text(
                    text = "${exercises.size} ejercicios",
                    color = MutedGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── Contenido principal ───────────────────────────────────────
            when {
                isLoading -> {
                    // Mientras carga el JSON por primera vez
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = PurplePrimary)
                            Text(
                                text = "Cargando ejercicios...",
                                color = MutedGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                exercises.isEmpty() -> {
                    // Sin resultados para la búsqueda actual
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🔍", fontSize = 48.sp)
                            Text(
                                text = "No se encontraron ejercicios",
                                color = OnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Probá con otro nombre o músculo",
                                color = MutedGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || selectedMuscle != null) {
                                TextButton(onClick = viewModel::clearFilters) {
                                    Text("Limpiar filtros", color = PurplePrimary)
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Lista de ejercicios filtrados
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(exercises, key = { it.id }) { exercise ->
                            SearchExerciseCard(
                                exercise = exercise,
                                onClick = {
                                    // Construimos la ruta igual que en DayDetailScreen
                                    // pero sin sets/reps/descanso (no vienen del JSON base)
                                    // pasamos 0/defaults para que ExerciseDetailScreen
                                    // muestre solo la info del repositorio
                                    val route = "exercise_detail" +
                                            "?nameEn=${exercise.name.encodeForNav()}" +
                                            "&nameEs=${exercise.name.encodeForNav()}" +
                                            "&sets=0" +
                                            "&reps=${"-".encodeForNav()}" +
                                            "&restSeconds=0" +
                                            "&notes=${"-".encodeForNav()}"
                                    onExerciseClick(route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Card de ejercicio para el buscador ────────────────────────────────────────
// Igual a ExerciseCard de DayDetailScreen pero sin las notas de la IA
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SearchExerciseCard(
    exercise: FreeExerciseDto,
    onClick: () -> Unit
) {
    // Construimos la URL de imagen directamente desde el DTO
    val imageUrl = exercise.images.firstOrNull()
        ?.let { ExerciseImageRepository.IMAGE_BASE_URL + it }

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
            // ── Imagen del ejercicio ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    GlideImage(
                        model = imageUrl,
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "💪", fontSize = 32.sp)
                }
            }

            // ── Info del ejercicio ─────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Nombre del ejercicio
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    maxLines = 2
                )

                // Músculo principal
                if (exercise.primaryMuscles.isNotEmpty()) {
                    Text(
                        text = exercise.primaryMuscles.first()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        color = PurplePrimary
                    )
                }

                // Chips de nivel y equipamiento
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SearchChip(translateLevel(exercise.level))
                    exercise.equipment?.let { equip ->
                        if (equip.isNotBlank()) SearchChip(equip)
                    }
                }
            }

            // Flecha indicadora
            Text(text = "›", fontSize = 24.sp, color = MutedGray)
        }
    }
}

// ── Chip para nivel/equipamiento en la card ───────────────────────────────────
@Composable
private fun SearchChip(label: String) {
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

// ── Chip seleccionable para el filtro de músculo ──────────────────────────────
@Composable
private fun MuscleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) PurplePrimary
                else PurplePrimary.copy(alpha = 0.15f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) OnBackground else PurplePrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Traducción de niveles ─────────────────────────────────────────────────────
private fun translateLevel(level: String): String = when (level.lowercase()) {
    "beginner"     -> "Principiante"
    "intermediate" -> "Intermedio"
    "advanced"     -> "Avanzado"
    else           -> level.replaceFirstChar { it.uppercase() }
}