package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.data.repository.RoutineResolver
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val routineResolver: RoutineResolver,
    private val routineRepository: RoutineRepository,
    private val exerciseImageRepository: ExerciseImageRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        checkDestination()
        // Precarga los ejercicios del asset en background mientras se muestra el splash.
        // La primera vez parsea el JSON y siembra Room (~200ms); las siguientes solo lee RAM.
        viewModelScope.launch { exerciseImageRepository.getAllExercises() }
    }

    private fun checkDestination() {
        viewModelScope.launch {
            val user = firebaseAuth.currentUser
            if (user == null) {
                _destination.value = SplashDestination.Login
                return@launch
            }

            // Resolvemos la rutina con varias oportunidades de reintento.
            // Unarranque frío puede tardar en abrir Room o Firestore; solo
            // navegamos a Form cuando hay CERTEZA de que no existe rutina.
            var attempts = 0
            while (attempts < MAX_ATTEMPTS) {
                when (val result = routineResolver.resolve(user.uid)) {
                    is RoutineResolver.Result.Found -> {
                        _destination.value = SplashDestination.Routine
                        return@launch
                    }
                    RoutineResolver.Result.None -> {
                        _destination.value = SplashDestination.Form
                        return@launch
                    }
                    RoutineResolver.Result.Unavailable -> {
                        // Incertidumbre — esperamos y reintentamos antes de decidir.
                        attempts++
                        delay(RETRY_DELAY_MS)
                    }
                }
            }

            // Agotamos los reintentos sin certeza. Nos mantenemos en Loading y
            // volvemos a intentar en background en vez de mandar al Form.
            // (No se pierde la rutina del usuario por un fallo temporal).
            _destination.value = SplashDestination.Loading
            delay(RETRY_DELAY_MS)
            checkDestination()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1500L
    }
}