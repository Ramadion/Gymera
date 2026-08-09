package com.DeBiaseRamiro.gymera.ui.screens.exercisedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ExerciseDetailScreen(
    nameEn: String,
    nameEs: String,
    sets: Int,
    reps: String,
    restSeconds: Int,
    notes: String,
    onBack: () -> Unit,
    onNextAction: (() -> Unit)? = null,
    isLastExercise: Boolean = false,
    dayId: String = "",
    exerciseId: String = "",
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(nameEn) {
        viewModel.loadExercise(
            nameEn = nameEn,
            nameEs = nameEs,
            sets = sets,
            reps = reps,
            restSeconds = restSeconds,
            notes = notes
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = nameEs,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (onNextAction != null) {
                FloatingActionButton(
                    onClick = onNextAction,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isLastExercise) Icons.Default.Check else Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = if (isLastExercise) "Finalizar" else "Siguiente"
                    )
                }
            }
        }
    ) { paddingValues ->

        when (val state = uiState) {

            is ExerciseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is ExerciseDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(onClick = onBack) { Text("Volver") }
                    }
                }
            }

            is ExerciseDetailUiState.Success -> {

                // true  = mostrar instrucciones en español (instructionsEs del asset)
                // false = mostrar instrucciones en inglés  (dto.instructions original)
                // Arranca en español si la traducción está disponible.
                var showSpanish by remember {
                    mutableStateOf(state.instructionsEs.isNotEmpty())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {

                    // ── IMAGEN ANIMADA ────────────────────────────────────────
                    AnimatedExerciseImage(
                        imageUrls = state.imageUrls,
                        exerciseName = state.nameEs
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // ── TÍTULO ────────────────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = state.nameEs,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            // Nombre en inglés como subtítulo discreto
                            Text(
                                text = state.dto.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // ── CHIPS SERIES / REPS / DESCANSO ───────────────────
                        // Solo si el ejercicio viene de una rutina (sets > 0).
                        // Si viene de Search, sets = 0 y no mostramos estos chips.
                        if (state.sets > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExerciseChip(label = "${state.sets} series")
                                ExerciseChip(label = "${state.reps} reps")
                                ExerciseChip(label = "${state.restSeconds}s descanso")
                            }
                        }

                        // ── INFO DEL EJERCICIO (COLAPSABLE) ───────────────────
                        // Empieza cerrada para una vista más limpia.
                        // El usuario la abre si quiere ver músculos, nivel, etc.
                        CollapsibleInfoCard(
                            primaryMuscles   = state.dto.primaryMuscles.map { translateMuscle(it) },
                            secondaryMuscles = state.dto.secondaryMuscles.map { translateMuscle(it) },
                            equipment        = translateEquipment(state.dto.equipment),
                            level            = translateLevel(state.dto.level),
                            category         = translateCategory(state.dto.category)
                        )

                        // ── NOTAS DE LA IA (COLAPSABLE) ───────────────────────
                        if (state.sets > 0 && state.notes.isNotBlank() && state.notes != "-") {
                            NoteCard(notes = state.notes)
                        }

                        // ── INSTRUCCIONES PASO A PASO ─────────────────────────
                        // BUG CORREGIDO: antes usaba state.dto.instructions (inglés siempre).
                        // Ahora usa state.instructionsEs por defecto (del asset traducido),
                        // con fallback a inglés si instructionsEs está vacío.
                        // El TextButton permite alternar entre idiomas.
                        val instructionsToShow = if (showSpanish && state.instructionsEs.isNotEmpty()) {
                            state.instructionsEs
                        } else {
                            state.dto.instructions
                        }

                        if (instructionsToShow.isNotEmpty()) {
                            InstructionsSection(
                                instructions     = instructionsToShow,
                                showSpanish      = showSpanish,
                                hasTranslation   = state.instructionsEs.isNotEmpty(),
                                onToggleLanguage = { showSpanish = !showSpanish }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ── COMPONENTES AUXILIARES ────────────────────────────────────────────────────

@Composable
private fun ExerciseChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Card colapsable con músculos, equipamiento, nivel y categoría.
 * Empieza cerrada — el usuario la abre tocando el encabezado.
 * Mismo patrón visual que NoteCard para consistencia.
 */
@Composable
private fun CollapsibleInfoCard(
    primaryMuscles: List<String>,
    secondaryMuscles: List<String>,
    equipment: String,
    level: String,
    category: String
) {
    // Arranca cerrada para una vista más limpia al entrar a la pantalla
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Encabezado siempre visible ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ℹ️ Información del ejercicio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ── Contenido expandible ──────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                    InfoRow(
                        label = "Músculo principal",
                        value = primaryMuscles.joinToString(", ")
                            .replaceFirstChar { it.uppercase() }
                    )
                    if (secondaryMuscles.isNotEmpty()) {
                        InfoRow(
                            label = "Músculos secundarios",
                            value = secondaryMuscles.joinToString(", ")
                                .replaceFirstChar { it.uppercase() }
                        )
                    }
                    InfoRow(label = "Equipamiento", value = equipment)
                    InfoRow(label = "Nivel",        value = level)
                    InfoRow(label = "Categoría",    value = category)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * Card colapsable con las notas que generó Gemini.
 * Mismo patrón que CollapsibleInfoCard.
 */
@Composable
private fun NoteCard(notes: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Consejo del entrenador IA",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * Instrucciones paso a paso con toggle ES / EN.
 *
 * @param instructions      lista ya resuelta (español o inglés según toggle)
 * @param showSpanish       estado actual del toggle
 * @param hasTranslation    false = no hay traducción → no se muestra el toggle
 * @param onToggleLanguage  callback para cambiar el idioma
 */
@Composable
private fun InstructionsSection(
    instructions: List<String>,
    showSpanish: Boolean,
    hasTranslation: Boolean,
    onToggleLanguage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Encabezado + toggle en la misma fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instrucciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // El toggle solo aparece si el asset tiene la traducción
            if (hasTranslation) {
                TextButton(onClick = onToggleLanguage) {
                    Text(
                        text = if (showSpanish) "Ver en inglés" else "Ver en español",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        instructions.forEachIndexed { index, instruction ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(top = 4.dp)
                )
            }
        }
    }
}

// ── FUNCIONES DE TRADUCCIÓN ───────────────────────────────────────────────────
// Definidas como `fun` (no private) para poder reutilizarlas en SearchScreen
// (filtros de músculo, nivel y equipamiento).

/** Traduce grupos musculares de inglés a español. */
fun translateMuscle(muscle: String): String = when (muscle.lowercase().trim()) {
    "abdominals"             -> "Abdominales"
    "abductors"              -> "Abductores"
    "adductors"              -> "Aductores"
    "biceps"                 -> "Bíceps"
    "calves"                 -> "Gemelos"
    "chest"                  -> "Pecho"
    "forearms"               -> "Antebrazos"
    "glutes"                 -> "Glúteos"
    "hamstrings"             -> "Isquiotibiales"
    "hip flexors"            -> "Flexores de cadera"
    "it band"                -> "Banda iliotibial"
    "lats"                   -> "Dorsales"
    "lower back"             -> "Lumbar"
    "middle back"            -> "Espalda media"
    "neck"                   -> "Cuello"
    "quadriceps"             -> "Cuádriceps"
    "shoulders"              -> "Hombros"
    "triceps"                -> "Tríceps"
    "traps"                  -> "Trapecios"
    "upper back"             -> "Espalda alta"
    "soleus"                 -> "Sóleo"
    else                     -> muscle.replaceFirstChar { it.uppercase() }
}

/** Traduce equipamiento de inglés a español. */
fun translateEquipment(equipment: String?): String = when (equipment?.lowercase()?.trim()) {
    null, "", "body only", "none" -> "Sin equipamiento"
    "barbell"                     -> "Barra"
    "dumbbell"                    -> "Mancuernas"
    "cable"                       -> "Cable"
    "machine"                     -> "Máquina"
    "kettlebells"                 -> "Kettlebells"
    "bands"                       -> "Bandas elásticas"
    "medicine ball"               -> "Balón medicinal"
    "exercise ball"               -> "Pelota de ejercicio"
    "foam roll"                   -> "Rodillo de espuma"
    "e-z curl bar"                -> "Barra EZ"
    "pullup bar"                  -> "Barra de dominadas"
    "other"                       -> "Otro"
    else                          -> equipment.replaceFirstChar { it.uppercase() }
}

/** Traduce categorías de ejercicio de inglés a español. */
fun translateCategory(category: String): String = when (category.lowercase().trim()) {
    "strength"                  -> "Fuerza"
    "cardio"                    -> "Cardio"
    "stretching"                -> "Estiramiento"
    "plyometrics"               -> "Pliometría"
    "powerlifting"              -> "Powerlifting"
    "olympic weightlifting"     -> "Halterofilia"
    "strongman"                 -> "Strongman"
    else                        -> category.replaceFirstChar { it.uppercase() }
}

/** Traduce niveles de dificultad de inglés a español. */
fun translateLevel(level: String): String = when (level.lowercase().trim()) {
    "beginner"      -> "Principiante"
    "intermediate"  -> "Intermedio"
    "advanced"      -> "Avanzado"
    else            -> level.replaceFirstChar { it.uppercase() }
}

/**
 * Imagen animada que alterna entre las fotos del ejercicio cada 1.5 segundos.
 * Si solo hay una imagen la muestra estática.
 * Si no hay imágenes muestra un placeholder con ícono.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AnimatedExerciseImage(
    imageUrls: List<String>,
    exerciseName: String
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    if (imageUrls.size >= 2) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1500L)
                currentIndex = (currentIndex + 1) % imageUrls.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrls.isEmpty()) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        } else {
            key(currentIndex) {
                GlideImage(
                    model = imageUrls[currentIndex],
                    contentDescription = "Demostración de $exerciseName — paso ${currentIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}