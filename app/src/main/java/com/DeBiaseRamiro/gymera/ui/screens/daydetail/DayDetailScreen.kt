package com.DeBiaseRamiro.gymera.ui.screens.daydetail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.ui.screens.exercisedetail.translateMuscle
import com.DeBiaseRamiro.gymera.ui.theme.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.encodeForNav(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    workoutDay: WorkoutDay,
    onExerciseClick: (route: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(workoutDay.id) {
        viewModel.initializeDay(workoutDay)
    }

    val exercises     by viewModel.exercises.collectAsState()
    val imageStates   by viewModel.imageStates.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    // ── Confirmación de borrado ───────────────────────────────────────────
    // Al deslizar, guardamos el ejercicio aquí en vez de borrarlo directo.
    // El AlertDialog le pide confirmación al usuario antes de llamar a removeExercise.
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            containerColor   = SurfaceDark,
            title = {
                Text(
                    text = "¿Eliminar ejercicio?",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "\"${exerciseToDelete!!.name}\" se eliminará de este día.",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeExercise(exerciseToDelete!!.id)
                    exerciseToDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveExercise(from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = OnBackground)
                    }
                },
                title = {
                    Column {
                        Text(text = workoutDay.dayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                        if (workoutDay.muscleFocus.isNotBlank()) {
                            Text(text = workoutDay.muscleFocus, fontSize = 12.sp, color = PurplePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadAllExercisesForSearch()
                    showAddSheet = true
                },
                containerColor = PurplePrimary,
                contentColor   = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar ejercicio")
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Text(
                text = "${exercises.size} ${if (exercises.size == 1) "ejercicio" else "ejercicios"}" +
                        "  •  mantené ≡ para reordenar  •  deslizá para eliminar",
                color    = MutedGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (exercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "No hay ejercicios para este día.", color = MutedGray, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Text(text = "Usá el botón + para agregar uno.", color = MutedGray.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    state   = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->

                        ReorderableItem(reorderableState, key = exercise.id) { isDragging ->

                            // longPressDraggableHandle es una extension de ReorderableItemScope,
                            // DEBE crearse dentro de este lambda. Se pasa como Modifier listo
                            // a ExerciseCard para evitar errores de scope en composables hijos.
                            val dragModifier = Modifier.longPressDraggableHandle(
                                onDragStopped = { viewModel.saveOrder() }
                            )

                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 16.dp else 0.dp,
                                label = "dragElevation"
                            )

                            // ── FIX: swipe MUESTRA diálogo, no borra directo ──────
                            // confirmValueChange retorna false → SwipeToDismissBox
                            // vuelve a su posición original. El AlertDialog aparece
                            // encima y espera la decisión del usuario.
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        exerciseToDelete = exercise   // guarda el ejercicio
                                        false                          // NO borra todavía
                                    } else false
                                },
                                positionalThreshold = { it * 0.4f }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    // Fondo rojo visible mientras se desliza
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(end = 20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Eliminar",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            ) {
                                ExerciseCard(
                                    exercise           = exercise,
                                    imageState         = imageStates[exercise.id] ?: ExerciseImageState.Loading,
                                    isDragging         = isDragging,
                                    elevation          = elevation,
                                    dragHandleModifier = dragModifier,
                                    onClick = {
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
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddExerciseBottomSheet(
            searchQuery   = searchQuery,
            searchResults = searchResults,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onAdd         = { nameEn, nameEs, muscleGroup, sets, reps, restSeconds ->
                viewModel.addExercise(nameEn, nameEs, muscleGroup, sets, reps, restSeconds)
                showAddSheet = false
                viewModel.resetSearch()
            },
            onDismiss = {
                showAddSheet = false
                viewModel.resetSearch()
            }
        )
    }
}

// ── ExerciseCard ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ExerciseCard(
    exercise           : Exercise,
    imageState         : ExerciseImageState,
    isDragging         : Boolean,
    elevation          : androidx.compose.ui.unit.Dp,
    dragHandleModifier : Modifier,
    onClick            : () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDragging) SurfaceDark.copy(alpha = 0.95f) else SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Mantené presionado para reordenar",
                tint = MutedGray,
                modifier = dragHandleModifier.size(24.dp)
            )

            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (val state = imageState) {
                    is ExerciseImageState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PurplePrimary, strokeWidth = 2.dp)
                    is ExerciseImageState.Success -> GlideImage(model = state.imageUrl, contentDescription = exercise.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    is ExerciseImageState.Error   -> Text("💪", fontSize = 28.sp)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = exercise.muscleGroup, fontSize = 12.sp, color = PurplePrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseChip("${exercise.sets}x${exercise.reps}")
                    ExerciseChip("${exercise.restSeconds}s")
                }
            }

            Text(text = "›", fontSize = 22.sp, color = MutedGray)
        }
    }
}

@Composable
private fun ExerciseChip(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(PurplePrimary.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ── AddExerciseBottomSheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseBottomSheet(
    searchQuery   : String,
    searchResults : List<FreeExerciseDto>,
    onQueryChange : (String) -> Unit,
    onAdd         : (nameEn: String, nameEs: String, muscleGroup: String, sets: Int, reps: String, restSeconds: Int) -> Unit,
    onDismiss     : () -> Unit
) {
    var selectedExercise by remember { mutableStateOf<FreeExerciseDto?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceDark,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = MutedGray) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {

            if (selectedExercise == null) {
                Text(text = "Agregar ejercicio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = searchQuery, onValueChange = onQueryChange,
                    placeholder = { Text("Buscar ejercicio...", color = MutedGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedGray) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MutedGray)
                            }
                        }
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PurplePrimary,
                        unfocusedBorderColor = MutedGray.copy(alpha = 0.3f),
                        focusedTextColor     = OnBackground,
                        unfocusedTextColor   = OnBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(searchResults, key = { it.id }) { dto ->
                        ExerciseSearchRow(dto = dto, onClick = { selectedExercise = dto })
                    }
                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = if (searchQuery.isBlank()) "Escribí el nombre de un ejercicio" else "Sin resultados para \"$searchQuery\"",
                                color = MutedGray, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                AddExerciseForm(
                    dto      = selectedExercise!!,
                    onBack   = { selectedExercise = null },
                    onConfirm = { sets, reps, rest ->
                        val dto = selectedExercise!!
                        onAdd(dto.name, dto.name, dto.primaryMuscles.firstOrNull()?.let { translateMuscle(it) } ?: "General", sets, reps, rest)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseSearchRow(dto: FreeExerciseDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BackgroundDark).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dto.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val muscles = dto.primaryMuscles.take(2).joinToString(", ") { translateMuscle(it) }
            if (muscles.isNotBlank()) Text(text = muscles, fontSize = 12.sp, color = PurplePrimary)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MutedGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AddExerciseForm(
    dto      : FreeExerciseDto,
    onBack   : () -> Unit,
    onConfirm: (sets: Int, reps: String, restSeconds: Int) -> Unit
) {
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var rest by remember { mutableStateOf("60") }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MutedGray)
        }
        Text(text = dto.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnBackground, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }

    Text(text = "Configurá las series para este ejercicio:", color = MutedGray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(label = "Series",       value = sets, onChange = { sets = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
        FormField(label = "Reps",         value = reps, onChange = { reps = it },                       modifier = Modifier.weight(1f))
        FormField(label = "Descanso (s)", value = rest, onChange = { rest = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            onConfirm(sets.toIntOrNull()?.coerceAtLeast(1) ?: 3, reps.ifBlank { "10" }, rest.toIntOrNull()?.coerceAtLeast(0) ?: 60)
        },
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
    ) {
        Text("Agregar al día", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = MutedGray, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = PurplePrimary,
                unfocusedBorderColor = MutedGray.copy(alpha = 0.3f),
                focusedTextColor     = OnBackground,
                unfocusedTextColor   = OnBackground
            )
        )
    }
}