package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Loading : SplashDestination()   // todavía verificando
    object Login : SplashDestination()     // no hay sesión
    object Form : SplashDestination()      // hay sesión pero no rutina
    object Routine : SplashDestination()   // hay sesión y rutina activa
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        checkDestination()
    }

    private fun checkDestination() {
        viewModelScope.launch {
            val user = firebaseAuth.currentUser
            if (user == null) {
                // No hay sesión de Firebase → Login
                _destination.value = SplashDestination.Login
                return@launch
            }
            // Hay sesión → verificamos si tiene rutina activa en Room
            val hasRoutine = routineRepository.getActiveRoutine(user.uid) != null
            _destination.value = if (hasRoutine) {
                SplashDestination.Routine   // directo a la rutina
            } else {
                SplashDestination.Form      // tiene cuenta pero sin rutina
            }
        }
    }
}