package com.DeBiaseRamiro.gymera.ui.screens.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ExerciseImageState {
    object Loading : ExerciseImageState()
    data class Success(val imageUrl: String) : ExerciseImageState()
    object Error : ExerciseImageState()
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val exerciseImageRepository: ExerciseImageRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    // ── Estados de imagen (id → estado) ──────────────────────────────────
    private val _imageStates = MutableStateFlow<Map<String, ExerciseImageState>>(emptyMap())
    val imageStates: StateFlow<Map<String, ExerciseImageState>> = _imageStates

    // ── Lista local de ejercicios ─────────────────────────────────────────
    // Se inicializa desde workoutDay.exercises y se modifica localmente.
    // Cada modificación (reordenar, eliminar, agregar) persiste en Room
    // inmediatamente, lo que dispara el Flow del SharedRoutineViewModel.
    // El guard 'initialized' evita que el recompose externo pise los cambios locales.
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private var currentDayId = ""
    private var initialized  = false

    // ── Buscador para agregar ejercicios ──────────────────────────────────
    private val _allExercises  = MutableStateFlow<List<FreeExerciseDto>>(emptyList())
    private val _searchQuery   = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Filtra la lista completa con debounce de 300ms para no filtrar en cada letra
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<FreeExerciseDto>> = combine(
        _allExercises,
        _searchQuery.debounce(300L)
    ) { all, query ->
        if (query.isBlank()) all.take(50)   // primeras 50 cuando no hay query
        else all.filter { it.name.lowercase().contains(query.lowercase()) }.take(50)
    }.stateIn(
        scope   = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ── initializeDay ─────────────────────────────────────────────────────
    // Llamado desde DayDetailScreen via LaunchedEffect(workoutDay.id).
    // El guard evita resetear la lista si el día no cambió
    // (ej: Room emitió un nuevo valor por nuestros propios cambios).
    fun initializeDay(workoutDay: WorkoutDay) {
        if (initialized && currentDayId == workoutDay.id) return
        currentDayId = workoutDay.id
        initialized  = true
        _exercises.value = workoutDay.exercises
        loadImages(workoutDay.exercises)
    }

    // ── moveExercise ──────────────────────────────────────────────────────
    // Llamado por rememberReorderableLazyListState en cada posición durante
    // el drag. Actualiza la lista en memoria — rápido, sin IO.
    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val list = _exercises.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        list.add(toIndex, list.removeAt(fromIndex))
        _exercises.value = list
    }

    // ── saveOrder ─────────────────────────────────────────────────────────
    // Llamado en onDragStopped — persiste el nuevo orden en Room.
    // Se hace UNA sola vez al soltar el elemento, no en cada posición.
    fun saveOrder() {
        viewModelScope.launch {
            routineRepository.reorderExercises(_exercises.value)
        }
    }

    // ── removeExercise ────────────────────────────────────────────────────
    // Actualiza la lista local inmediatamente (UI reactiva) y luego Room.
    fun removeExercise(exerciseId: String) {
        _exercises.value = _exercises.value.filter { it.id != exerciseId }
        viewModelScope.launch {
            routineRepository.removeExercise(exerciseId)
        }
    }

    // ── addExercise ───────────────────────────────────────────────────────
    // Agrega el ejercicio al final de la lista local y persiste en Room.
    // La imagen se carga en paralelo sin bloquear la UI.
    fun addExercise(
        nameEn: String,
        nameEs: String,
        muscleGroup: String,
        sets: Int,
        reps: String,
        restSeconds: Int
    ) {
        val newExercise = Exercise(
            id          = UUID.randomUUID().toString(),
            name        = nameEs,
            nameEn      = nameEn,
            muscleGroup = muscleGroup,
            sets        = sets,
            reps        = reps,
            restSeconds = restSeconds,
            notes       = ""
        )
        val newList = _exercises.value + newExercise
        _exercises.value = newList

        viewModelScope.launch {
            // Persiste en Room
            routineRepository.addExercise(currentDayId, newExercise, newList.size - 1)
            // Carga la imagen del nuevo ejercicio (usa la misma lógica que los demás)
            loadSingleImage(newExercise)
        }
    }

    // ── onSearchQueryChanged ──────────────────────────────────────────────
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    // ── loadAllExercisesForSearch ─────────────────────────────────────────
    // Carga los 873 ejercicios desde el asset para el buscador del BottomSheet.
    // Solo lo hace una vez — las llamadas siguientes encuentran la lista ya en RAM.
    fun loadAllExercisesForSearch() {
        if (_allExercises.value.isNotEmpty()) return
        viewModelScope.launch {
            _allExercises.value = exerciseImageRepository.getAllExercises()
        }
    }

    // ── resetSearch ───────────────────────────────────────────────────────
    // Limpia el buscador al cerrar el BottomSheet
    fun resetSearch() { _searchQuery.value = "" }

    // ── loadImages ────────────────────────────────────────────────────────
    // Carga imágenes de todos los ejercicios en paralelo al inicializar el día
    private fun loadImages(exercises: List<Exercise>) {
        viewModelScope.launch {
            _imageStates.value = exercises.associate { it.id to ExerciseImageState.Loading }
            val results = exercises.map { exercise ->
                async {
                    val state = try {
                        val url = exerciseImageRepository.getImageUrl(exercise.nameEn)
                        if (url != null) ExerciseImageState.Success(url)
                        else             ExerciseImageState.Error
                    } catch (e: Exception) {
                        ExerciseImageState.Error
                    }
                    exercise.id to state
                }
            }.awaitAll()
            _imageStates.value = results.toMap()
        }
    }

    // Carga la imagen de un único ejercicio recién agregado
    private suspend fun loadSingleImage(exercise: Exercise) {
        _imageStates.value = _imageStates.value + (exercise.id to ExerciseImageState.Loading)
        val url = try { exerciseImageRepository.getImageUrl(exercise.nameEn) } catch (e: Exception) { null }
        val state = if (url != null) ExerciseImageState.Success(url) else ExerciseImageState.Error
        _imageStates.value = _imageStates.value + (exercise.id to state)
    }
}