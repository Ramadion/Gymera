package com.DeBiaseRamiro.gymera.ui.screens.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExerciseImageState {
    object Loading : ExerciseImageState()
    data class Success(val imageUrl: String) : ExerciseImageState()
    object Error : ExerciseImageState()
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val exerciseImageRepository: ExerciseImageRepository
) : ViewModel() {

    private val _imageStates = MutableStateFlow<Map<String, ExerciseImageState>>(emptyMap())
    val imageStates: StateFlow<Map<String, ExerciseImageState>> = _imageStates

    private var initialized = false

    fun loadImages(exercises: List<Exercise>) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            // Marcamos todos como Loading
            _imageStates.value = exercises.associate { it.id to ExerciseImageState.Loading }

            // La primera llamada descarga el JSON completo (~800 ejercicios).
            // Las siguientes usan el caché en memoria — no hay más descargas.
            val results = exercises.map { exercise ->
                async {
                    val imageState = try {
                        val url = exerciseImageRepository.getImageUrl(exercise.nameEn)
                        if (url != null) {
                            android.util.Log.d("GYMERA_DEBUG", "✅ ${exercise.nameEn} → $url")
                            ExerciseImageState.Success(url)
                        } else {
                            android.util.Log.w("GYMERA_DEBUG", "⚠️ Sin match para: ${exercise.nameEn}")
                            ExerciseImageState.Error
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GYMERA_DEBUG", "❌ Error: ${e.message}")
                        ExerciseImageState.Error
                    }
                    exercise.id to imageState
                }
            }.awaitAll()

            _imageStates.value = results.toMap()
        }
    }
}