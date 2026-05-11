package com.DeBiaseRamiro.gymera.ui.screens.exercisedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ExerciseDetailScreen(
    // Datos del ejercicio que vienen por argumentos de navegación desde DayDetailScreen
    nameEn: String,
    nameEs: String,
    sets: Int,
    reps: String,
    restSeconds: Int,
    notes: String,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    // Cargamos el ejercicio al entrar a la pantalla
    // LaunchedEffect con nameEn como key garantiza que solo se ejecuta una vez
    // (o si cambia el ejercicio, lo cual no pasa en este flujo)
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
                    // Mostramos el nombre en español en el header
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
        }
    ) { paddingValues ->

        when (val state = uiState) {

            // Estado de carga
            is ExerciseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Estado de error (ejercicio no encontrado en free-exercise-db)
            is ExerciseDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                        Button(onClick = onBack) {
                            Text("Volver")
                        }
                    }
                }
            }

            // Estado success — pantalla principal
            is ExerciseDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()) // scroll completo de la pantalla
                ) {

                    // ── IMAGEN DEL EJERCICIO ──────────────────────────────────────
                    // Ocupa el ancho completo con altura fija, como una hero image
                    AnimatedExerciseImage(
                        imageUrls = state.imageUrls,
                        exerciseName = state.nameEs
                    )

                    // ── CONTENIDO PRINCIPAL ───────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // Nombre en español (título grande) + nombre en inglés (subtítulo)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = state.nameEs,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.dto.name, // nombre oficial de free-exercise-db en inglés
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // ── CHIPS DE SERIES / REPS / DESCANSO ────────────────────
                        // Mismos chips que en DayDetailScreen para consistencia visual

                        // Solo mostramos los chips de rutina si el ejercicio viene de una rutina
                        // (sets > 0 significa que viene de DayDetailScreen, no del buscador)
                        if (state.sets > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExerciseChip(label = "${state.sets} series")
                                ExerciseChip(label = "${state.reps} reps")
                                ExerciseChip(label = "${state.restSeconds}s descanso")
                            }
                        }

                        // ── INFORMACIÓN DEL EJERCICIO ───────────────────────────── lo saque

                        // ── NOTAS DE LA IA ────────────────────────────────────────
                        // Solo mostramos si Gemini generó alguna nota para este ejercicio
                        if (state.notes.isNotBlank()) {
                            NoteCard(notes = state.notes)
                        }



                        // ── INSTRUCCIONES PASO A PASO ─────────────────────────────
                        // Vienen del JSON de free-exercise-db como lista de strings
                        if (state.dto.instructions.isNotEmpty()) {
                            InstructionsSection(instructions = state.dto.instructions)
                        }

                        // Espaciado al final para que el último elemento no quede pegado
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ── COMPONENTES AUXILIARES ────────────────────────────────────────────────────

/**
 * Chip reutilizable para series, reps y descanso.
 * Mismo estilo que en DayDetailScreen para consistencia visual.
 */
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
 * Card con la info de músculos, equipamiento, nivel y categoría.
 * Usa un layout de filas para que quede ordenado y legible.
 */
@Composable
private fun ExerciseInfoCard(
    primaryMuscles: List<String>,
    secondaryMuscles: List<String>,
    equipment: String?,
    level: String,
    category: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Información del ejercicio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Músculo principal
            InfoRow(
                label = "Músculo principal",
                value = primaryMuscles.joinToString(", ")
                    .replaceFirstChar { it.uppercase() }
            )

            // Músculos secundarios (solo si hay)
            if (secondaryMuscles.isNotEmpty()) {
                InfoRow(
                    label = "Músculos secundarios",
                    value = secondaryMuscles.joinToString(", ")
                        .replaceFirstChar { it.uppercase() }
                )
            }

            // Equipamiento (puede ser null en el DTO)
            InfoRow(
                label = "Equipamiento",
                value = equipment?.replaceFirstChar { it.uppercase() } ?: "Sin equipamiento"
            )

            // Nivel de dificultad
            InfoRow(
                label = "Nivel",
                value = translateLevel(level) // traducimos beginner/intermediate/advanced
            )

            // Categoría
            InfoRow(
                label = "Categoría",
                value = category.replaceFirstChar { it.uppercase() }
            )
        }
    }
}

/**
 * Fila de información: etiqueta en gris + valor en negro.
 * Patrón simple y legible para los metadatos del ejercicio.
 */
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
 * Card con las notas que generó Gemini para este ejercicio específico.
 * Se muestra solo cuando notes no está vacío.
 */
@Composable
private fun NoteCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 Consejo del entrenador IA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Sección de instrucciones paso a paso.
 * Cada instrucción tiene un número de paso y el texto de free-exercise-db.
 */
@Composable
private fun InstructionsSection(instructions: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Instrucciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        instructions.forEachIndexed { index, instruction ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Círculo con número de paso
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

                // Texto de la instrucción
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp) // alineación visual con el círculo
                )
            }
        }
    }
}

/**
 * Traduce los niveles de inglés a español.
 * Los valores vienen directamente del JSON de free-exercise-db.
 */
private fun translateLevel(level: String): String = when (level.lowercase()) {
    "beginner" -> "Principiante"
    "intermediate" -> "Intermedio"
    "advanced" -> "Avanzado"
    else -> level.replaceFirstChar { it.uppercase() }
}

/**
 * Muestra las imágenes del ejercicio alternando entre ellas cada 1.5 segundos,
 * simulando un GIF. Si solo hay una imagen, la muestra estática.
 * Si no hay imágenes, muestra el placeholder con ícono.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AnimatedExerciseImage(
    imageUrls: List<String>,
    exerciseName: String
) {
    // Índice de la imagen actualmente visible
    var currentIndex by remember { mutableIntStateOf(0) }

    // Solo iniciamos el timer si hay 2 o más imágenes para alternar
    if (imageUrls.size >= 2) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1500L) // 1.5 segundos entre cada foto
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
            // Placeholder si free-exercise-db no tiene imágenes para este ejercicio
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        } else {
            // Crossfade entre imágenes para una transición suave
            // key = currentIndex fuerza a Glide a cargar la nueva imagen cuando cambia el índice
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