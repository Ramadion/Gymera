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
        val dto: FreeExerciseDto,       // datos completos del repositorio free-exercise-db
        val imageUrls: List<String>,         // URL de imagen construida
        val nameEs: String,             // nombre en español que viene de Gemini (para mostrar)
        val sets: Int,
        val reps: String,
        val restSeconds: Int,
        val notes: String
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
     * Carga el detalle completo del ejercicio.
     * Recibe todos los datos que vienen de DayDetailScreen via argumentos de navegación.
     *
     * @param nameEn   nombre en inglés para buscar en free-exercise-db
     * @param nameEs   nombre en español para mostrar al usuario
     * @param sets     series (de Gemini)
     * @param reps     repeticiones (de Gemini)
     * @param restSeconds  descanso en segundos (de Gemini)
     * @param notes    notas de la IA (de Gemini)
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

            // Buscamos el ejercicio en el JSON cacheado usando fuzzy matching
            val dto = exerciseImageRepository.getExerciseDetail(nameEn)
            val imageUrls = dto!!.images.mapNotNull { imagePath ->
                if (imagePath.isNotBlank()) ExerciseImageRepository.IMAGE_BASE_URL + imagePath
                else null
            }

            if (dto == null) {
                // Si no encontramos el ejercicio en free-exercise-db, mostramos error
                // Esto puede pasar con ejercicios muy específicos que Gemini inventó
                _uiState.value = ExerciseDetailUiState.Error(
                    "No se encontró información detallada para \"$nameEs\""
                )
                return@launch
            }

            // Construimos la URL de imagen usando el DTO que ya encontramos
            // (evita hacer el fuzzy match dos veces)
            val imageUrl = exerciseImageRepository.getImageUrlFromDto(dto)

            _uiState.value = ExerciseDetailUiState.Success(
                dto = dto,
                imageUrls = imageUrls,
                nameEs = nameEs,
                sets = sets,
                reps = reps,
                restSeconds = restSeconds,
                notes = notes
            )
        }
    }
}