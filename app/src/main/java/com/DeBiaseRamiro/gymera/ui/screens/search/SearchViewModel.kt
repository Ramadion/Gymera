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

    // Lista completa cargada una sola vez desde el cache
    private var allExercises: List<FreeExerciseDto> = emptyList()

    // Grupos musculares para el desplegable — se cargan junto con los ejercicios
    private val _muscleGroups = MutableStateFlow<List<String>>(emptyList())
    val muscleGroups: StateFlow<List<String>> = _muscleGroups

    // Texto que el usuario escribe en la barra de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Músculo seleccionado en el desplegable — null significa "Todos"
    private val _selectedMuscle = MutableStateFlow<String?>(null)
    val selectedMuscle: StateFlow<String?> = _selectedMuscle

    // true mientras se carga la lista por primera vez
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Resultado filtrado — combina búsqueda por texto + filtro de músculo
    // Se recalcula automáticamente cada vez que cambia el query o el músculo
    @OptIn(FlowPreview::class)
    val filteredExercises: StateFlow<List<FreeExerciseDto>> = combine(
        _searchQuery
            .debounce(300L), // esperamos 300ms después del último tipeo para no filtrar en cada letra
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

    private fun loadExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            // Carga desde RAM/Room/red según el nivel de cache disponible
            allExercises = exerciseImageRepository.getAllExercises()
            _muscleGroups.value = exerciseImageRepository.getMuscleGroups()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onMuscleSelected(muscle: String?) {
        // Si se selecciona el mismo músculo que ya está, lo deseleccionamos (toggle)
        _selectedMuscle.value = if (_selectedMuscle.value == muscle) null else muscle
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedMuscle.value = null
    }

    // Filtra la lista completa en memoria — sin tocar la red ni Room
    private fun filterExercises(query: String, muscle: String?): List<FreeExerciseDto> {
        var result = allExercises

        // Filtro por texto — busca en el nombre del ejercicio
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }

        // Filtro por grupo muscular — busca en primaryMuscles
        if (muscle != null) {
            result = result.filter { dto ->
                dto.primaryMuscles.any { m ->
                    m.lowercase() == muscle.lowercase()
                }
            }
        }

        return result
    }
}