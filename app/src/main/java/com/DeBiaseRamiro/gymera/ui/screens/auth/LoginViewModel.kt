package com.DeBiaseRamiro.gymera.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.repository.RoutineResolver
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val routineResolver: RoutineResolver
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

                // Resolvemos la rutina con el servicio centralizado.
                // Solo navegamos a Form cuando hay CERTEZA de que no existe rutina.
                var attempts = 0
                while (attempts < MAX_ATTEMPTS) {
                    when (val result = routineResolver.resolve(user.uid)) {
                        is RoutineResolver.Result.Found -> {
                            _uiState.value = LoginUiState.GoToRoutine
                            return@launch
                        }
                        RoutineResolver.Result.None -> {
                            _uiState.value = LoginUiState.GoToForm
                            return@launch
                        }
                        RoutineResolver.Result.Unavailable -> {
                            attempts++
                            delay(RETRY_DELAY_MS)
                        }
                    }
                }

                // Sin certeza tras los reintentos: no forzamos el Form (no queremos
                // perder la rutina del usuario por un fallo temporal). Volvemos a
                // Idle para que el usuario pueda reintentar.
                _uiState.value = LoginUiState.Error("No se pudo recuperar tu rutina. Reintentá.")

            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() { _uiState.value = LoginUiState.Idle }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1500L
    }
}