package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

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

            // Nivel 1: Room — usamos el Flow en lugar de lectura puntual
            // Esto resuelve el race condition donde Room no terminó de escribir
            val localRoutine = withTimeoutOrNull(2000L) {
                routineRepository.getActiveRoutineFlow(user.uid).first()
            }


            if (localRoutine != null) {
                _destination.value = SplashDestination.Routine
                return@launch
            }

            // Nivel 2: Firestore — busca directo en la subcolección
            val cloudRoutine = try {
                firestoreRepository.fetchRoutineFromCloud(user.uid)
            } catch (e: Exception) {
                android.util.Log.w("GYM_SPLASH", "Firestore no disponible: ${e.message}")
                null
            }
            if (cloudRoutine != null) {
                routineRepository.saveRoutine(cloudRoutine, user.uid)
                android.util.Log.d("GYM_NAV", "Splash — user: ${user?.uid}")
                android.util.Log.d("GYM_NAV", "Splash — localRoutine: $localRoutine")
                _destination.value = SplashDestination.Routine
                return@launch
            }

            // Nivel 3: No tiene rutina, manda al form para generar 1
            _destination.value = SplashDestination.Form
        }
    }
}