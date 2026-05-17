package com.DeBiaseRamiro.gymera.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    object GoToForm    : LoginUiState()
    object GoToRoutine : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val user = authRepository.signInWithGoogle(idToken)
                    ?: throw Exception("No se pudo autenticar")
                android.util.Log.d("GYM_NAV", "Usuario autenticado: ${user.uid}")

                // Nivel 1: verificamos Room (instantáneo)
                val localRoutine = routineRepository.getActiveRoutine(user.uid)
                android.util.Log.d("GYM_NAV", "Room local routine: $localRoutine")
                if (localRoutine != null) {
                    _uiState.value = LoginUiState.GoToRoutine
                    return@launch
                }

                // Nivel 2: Room vacío — buscamos en Firestore
                // (caso: cerró sesión, volvió a logearse, Room fue limpiado)
                val cloudRoutine = firestoreRepository.fetchRoutineFromCloud(user.uid)
                android.util.Log.d("GYM_NAV", "Firestore cloud routine: $cloudRoutine")
                if (cloudRoutine != null) {
                    // Bajamos la rutina a Room para disponibilidad offline
                    routineRepository.saveRoutine(cloudRoutine, user.uid)
                    _uiState.value = LoginUiState.GoToRoutine
                    return@launch
                }

                // Nivel 3: usuario nuevo o sin rutina — al formulario
                _uiState.value = LoginUiState.GoToForm

            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() { _uiState.value = LoginUiState.Idle }
}