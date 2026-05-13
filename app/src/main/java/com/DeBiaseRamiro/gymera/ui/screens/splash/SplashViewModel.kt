package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Loading  : SplashDestination()
    object Login    : SplashDestination()
    object Form     : SplashDestination()
    object Routine  : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init { checkDestination() }

    private fun checkDestination() {
        viewModelScope.launch {
            val user = firebaseAuth.currentUser
            if (user == null) {
                _destination.value = SplashDestination.Login
                return@launch
            }

            // Nivel 1: buscamos en Room (instantáneo, sin red)
            val localRoutine = routineRepository.getActiveRoutine(user.uid)
            if (localRoutine != null) {
                _destination.value = SplashDestination.Routine
                return@launch
            }

            // Nivel 2: Room vacío (dispositivo nuevo o reinstalación)
            // Intentamos bajar la rutina desde Firestore
            val cloudRoutine = firestoreRepository.fetchRoutineFromCloud(user.uid)
            if (cloudRoutine != null) {
                // La guardamos en Room para que funcione offline de ahora en adelante
                routineRepository.saveRoutine(cloudRoutine, user.uid)
                _destination.value = SplashDestination.Routine
                return@launch
            }

            // Nivel 3: no hay rutina en ningún lado → formulario
            _destination.value = SplashDestination.Form
        }
    }
}