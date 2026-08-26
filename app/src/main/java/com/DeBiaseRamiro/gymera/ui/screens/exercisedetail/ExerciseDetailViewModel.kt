package com.DeBiaseRamiro.gymera.ui.screens.exercisedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la pantalla — sealed class para manejar loading/success/error limpiamente
sealed class ExerciseDetailUiState {
    object Loading : ExerciseDetailUiState()
    data class Success(
        val dto: FreeExerciseDto,           // datos completos del asset gymera_exercises.json
        val imageUrls: List<String>,        // URLs de las imágenes para la animación
        val nameEs: String,                 // nombre en español que viene de Gemini (para mostrar)
        val sets: Int,
        val reps: String,
        val restSeconds: Int,
        val notes: String,

        // ── Instrucciones en español ──────────────────────────────────────────
        // Vienen directamente del campo instructionsEs del JSON bundleado.
        // Nunca requieren una llamada de red adicional — el asset ya las tiene.
        //
        // Si está vacía (edge case: traducción falló en translate_exercises.py),
        // la UI muestra instructions (inglés) como fallback. Ver ExerciseDetailScreen.
        // ─────────────────────────────────────────────────────────────────────
        val instructionsEs: List<String>
    ) : ExerciseDetailUiState()
    data class Error(val message: String) : ExerciseDetailUiState()
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseImageRepository: ExerciseImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExerciseDetailUiState>(ExerciseDetailUiState.Loading)
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    /**
     * Carga el detalle completo del ejercicio desde el asset (via Room o memoria).
     * No realiza ninguna llamada de red — todo viene del JSON bundleado.
     *
     * @param nameEn       nombre en inglés para buscar en el asset (fuzzy matching)
     * @param nameEs       nombre en español para mostrar al usuario (viene de Gemini)
     * @param sets         series (de Gemini)
     * @param reps         repeticiones (de Gemini)
     * @param restSeconds  descanso en segundos (de Gemini)
     * @param notes        notas de la IA (de Gemini)
     */
    fun loadExercise(
        nameEn: String,
        nameEs: String,
        sets: Int,
        reps: String,
        restSeconds: Int,
        notes: String
    ) {
        viewModelScope.launch {
            _uiState.value = ExerciseDetailUiState.Loading

            val dto = exerciseImageRepository.getExerciseDetail(nameEn)

            // Si no se encontró el ejercicio en el asset, mostramos un estado
            // degradado con los datos de Gemini (nombre, series, reps, notas)
            // pero sin imagen ni instrucciones del asset.
            val exerciseDto = dto ?: FreeExerciseDto(
                id             = nameEn,
                name           = nameEn,
                level          = "",
                equipment      = null,
                primaryMuscles = emptyList(),
                secondaryMuscles = emptyList(),
                category       = "",
                images         = emptyList(),
                instructions   = emptyList(),
                instructionsEs = emptyList()
            )

            val imageUrls = exerciseDto.images
                .orEmpty()
                .filter { it.isNotBlank() }
                .map { ExerciseImageRepository.IMAGE_BASE_URL + it }

            val instructionsEs = exerciseDto.instructionsEs

            _uiState.value = ExerciseDetailUiState.Success(
                dto           = exerciseDto,
                imageUrls     = imageUrls,
                nameEs        = nameEs,
                sets          = sets,
                reps          = reps,
                restSeconds   = restSeconds,
                notes         = notes,
                instructionsEs = instructionsEs
            )
        }
    }
}