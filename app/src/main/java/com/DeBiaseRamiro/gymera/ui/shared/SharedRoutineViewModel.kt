package com.DeBiaseRamiro.gymera.ui.shared

import androidx.lifecycle.ViewModel
import com.DeBiaseRamiro.gymera.domain.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * SharedRoutineViewModel
 *
 * ViewModel compartido entre pantallas, scoped a MainActivity.
 * Su propósito es actuar como estado global temporal para la Routine
 * generada por Gemini, mientras no tenemos Room implementado.
 *
 * Una vez que implementemos Room (feature/room), este ViewModel
 * se simplifica: RoutineScreen leerá directo de Room via Flow,
 * y este ViewModel solo guardará en Room al recibir la rutina.
 *
 * Anotado con @HiltViewModel para que Hilt lo inyecte correctamente.
 */
@HiltViewModel
class SharedRoutineViewModel @Inject constructor() : ViewModel() {

    // StateFlow que contiene la rutina activa, o null si no hay ninguna
    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine

    /**
     * Se llama desde LoadingScreen cuando Gemini termina de generar la rutina.
     * Guarda la rutina en memoria para que RoutineScreen pueda leerla.
     */
    fun setRoutine(routine: Routine) {
        _currentRoutine.value = routine
    }

    /**
     * Se llama cuando el usuario quiere generar una nueva rutina.
     * Resetea el estado para forzar el flujo de Formulario → Loading → Routine.
     */
    fun clearRoutine() {
        _currentRoutine.value = null
    }
}