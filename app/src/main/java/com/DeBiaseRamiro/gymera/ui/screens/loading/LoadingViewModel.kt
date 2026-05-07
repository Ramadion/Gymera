package com.DeBiaseRamiro.gymera.ui.screens.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
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
    private val routineRepository: RoutineRepository,
    private val firebaseAuth: FirebaseAuth   // para obtener el UID del usuario
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingUiState>(LoadingUiState.Loading)
    val uiState: StateFlow<LoadingUiState> = _uiState

    fun generateRoutine(userProfile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = LoadingUiState.Loading
            try {
                // 1. Generamos la rutina via Gemini (igual que antes)
                val routine = routineRepository.generateRoutine(userProfile)

                // 2. NUEVO: guardamos en Room para persistencia offline
                // El UID del usuario autenticado es necesario para asociar la rutina
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")
                routineRepository.saveRoutine(routine, uid)

                // 3. Notificamos éxito a la UI (igual que antes)
                _uiState.value = LoadingUiState.Success(routine)

            } catch (e: Exception) {
                _uiState.value = LoadingUiState.Error(
                    e.message ?: "Error al generar la rutina"
                )
            }
        }
    }
}