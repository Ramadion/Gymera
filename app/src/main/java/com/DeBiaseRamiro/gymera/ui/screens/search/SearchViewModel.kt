package com.DeBiaseRamiro.gymera.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val exerciseImageRepository: ExerciseImageRepository
) : ViewModel() {

    // Lista completa cargada una sola vez desde el asset (vía ExerciseImageRepository).
    // A partir de ese momento todos los filtros operan en memoria — sin red, sin Room.
    private var allExercises: List<FreeExerciseDto> = emptyList()

    // Grupos musculares para el desplegable — se derivan de allExercises al cargar
    private val _muscleGroups = MutableStateFlow<List<String>>(emptyList())
    val muscleGroups: StateFlow<List<String>> = _muscleGroups

    // Texto que el usuario escribe en la barra de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Músculo seleccionado en el desplegable — null significa "Todos"
    private val _selectedMuscle = MutableStateFlow<String?>(null)
    val selectedMuscle: StateFlow<String?> = _selectedMuscle

    // true mientras se carga la lista por primera vez desde el asset
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Resultado filtrado — combina búsqueda por texto + filtro de músculo.
    // Se recalcula automáticamente cada vez que cambia el query o el músculo.
    // debounce(300ms) evita filtrar en cada letra mientras el usuario escribe.
    @OptIn(FlowPreview::class)
    val filteredExercises: StateFlow<List<FreeExerciseDto>> = combine(
        _searchQuery.debounce(300L),
        _selectedMuscle
    ) { query, muscle ->
        filterExercises(query, muscle)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadExercises()
    }

    // Carga la lista desde el asset via ExerciseImageRepository.
    // getAllExercises() y getMuscleGroups() leen de RAM si ya está cargado,
    // del asset solo la primera vez — sin ninguna llamada de red.
    private fun loadExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            allExercises = exerciseImageRepository.getAllExercises()
            _muscleGroups.value = exerciseImageRepository.getMuscleGroups()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    // Toggle: si se selecciona el mismo músculo que ya está activo, lo limpia
    fun onMuscleSelected(muscle: String?) {
        _selectedMuscle.value = if (_selectedMuscle.value == muscle) null else muscle
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedMuscle.value = null
    }

    // ── filterExercises ───────────────────────────────────────────────────────
    // Filtra la lista completa en memoria. Ambos filtros son independientes:
    // pueden aplicarse juntos, por separado, o ninguno (devuelve todo).
    //
    // BUG CORREGIDO: en la versión anterior el filtro de músculo estaba anidado
    // dentro del if de texto, por lo que si el buscador estaba vacío el filtro
    // de músculo nunca se aplicaba. Ahora los dos bloques son secuenciales e
    // independientes — cada uno reduce el resultado del anterior.
    // ─────────────────────────────────────────────────────────────────────────
    private fun filterExercises(query: String, muscle: String?): List<FreeExerciseDto> {
        var result = allExercises

        // Filtro 1: por texto — busca en el nombre en inglés del ejercicio
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }

        // Filtro 2: por grupo muscular — busca en primaryMuscles
        // Es independiente del filtro de texto: si el buscador está vacío,
        // este filtro igual se aplica sobre la lista completa.
        if (muscle != null) {
            result = result.filter { dto ->
                dto.primaryMuscles.any { m -> m.lowercase() == muscle.lowercase() }
            }
        }

        return result
    }
}