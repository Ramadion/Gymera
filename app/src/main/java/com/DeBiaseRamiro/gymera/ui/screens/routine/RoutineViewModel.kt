package com.DeBiaseRamiro.gymera.ui.screens.routine

import androidx.lifecycle.ViewModel
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.ui.shared.SharedRoutineViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * RoutineViewModel
 *
 * ViewModel de la pantalla de Rutina Semanal.
 * Por ahora delega al SharedRoutineViewModel para leer la rutina.
 *
 * En feature/room, este ViewModel inyectará el RoutineRepository
 * y leerá desde Room con un Flow, reemplazando completamente
 * la dependencia de SharedRoutineViewModel.
 *
 * NOTA: No podemos inyectar SharedRoutineViewModel directamente
 * con Hilt porque necesita ser el MISMO INSTANCIA que usa LoadingScreen.
 *
 * Por eso la UI pasa el StateFlow como parámetro (ver RoutineScreen).
 */
@HiltViewModel
class RoutineViewModel @Inject constructor() : ViewModel() {

    // Por ahora no tiene lógica propia — la pantalla recibe
    // el StateFlow<Routine?> directamente desde el SharedViewModel.
    // Aquí irá la lógica de Room cuando implementemos feature/room.

    /**
     * En feature/room, agrego:
     *   private val routineRepository: RoutineRepository
     *   val routine: Flow<Routine?> = routineRepository.getActiveRoutine()
     *
     * fun generateNewRoutine() { ... llama al formulario ... }
     */
}