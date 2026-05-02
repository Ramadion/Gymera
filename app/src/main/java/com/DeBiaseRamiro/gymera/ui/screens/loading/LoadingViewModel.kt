package com.DeBiaseRamiro.gymera.ui.screens.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoadingUiState {
    object Loading : LoadingUiState()
    data class Success(val routine: Routine) : LoadingUiState()
    data class Error(val message: String) : LoadingUiState()
}

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingUiState>(LoadingUiState.Loading)
    val uiState: StateFlow<LoadingUiState> = _uiState

    fun generateRoutine(userProfile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = LoadingUiState.Loading
            try {
                val routine = routineRepository.generateRoutine(userProfile)
                _uiState.value = LoadingUiState.Success(routine)
            } catch (e: Exception) {
                _uiState.value = LoadingUiState.Error(
                    e.message ?: "Error al generar la rutina"
                )
            }
        }
    }
}