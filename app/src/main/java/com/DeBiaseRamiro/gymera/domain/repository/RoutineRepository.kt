package com.DeBiaseRamiro.gymera.domain.repository

import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserPhysicalProfile
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {

    suspend fun generateRoutine(
        userProfile: UserProfile,
        physicalProfile: UserPhysicalProfile? = null
    ): Routine

    suspend fun saveRoutine(routine: Routine, userUid: String)

    fun getActiveRoutineFlow(userUid: String): Flow<Routine?>

    suspend fun getActiveRoutine(userUid: String): Routine?

    suspend fun deactivateActiveRoutine(userUid: String)

    // ── Edición manual de ejercicios (DayDetailScreen) ────────────────────

    // Elimina un ejercicio del día. Room emite el Flow actualizado automáticamente.
    suspend fun removeExercise(exerciseId: String)

    // Persiste el nuevo orden de los ejercicios después de un drag-and-drop.
    // Recibe la lista completa reordenada — actualiza orderInDay de cada uno.
    suspend fun reorderExercises(exercises: List<Exercise>)

    // Agrega un ejercicio nuevo al final de un día específico.
    // @param dayId        ID del WorkoutDay al que pertenece
    // @param exercise     ejercicio con todos sus campos ya seteados
    // @param orderInDay   posición dentro del día (tamaño actual de la lista)
    suspend fun addExercise(dayId: String, exercise: Exercise, orderInDay: Int)

    // ── Edición de días (RoutineScreen) ──────────────────────────────────

    // Marca un día como descanso (true) o entrenamiento (false).
    // Los ejercicios del día NO se borran: quedan guardados y reaparecen
    // si el día se reactiva más adelante.
    suspend fun setWorkoutDayRest(dayId: String, isRest: Boolean)

    // Actualiza la descripción/enfoque del día. Puede ser vacía (sin descripción).
    suspend fun setWorkoutDayMuscleFocus(dayId: String, muscleFocus: String)

    // Borra todos los ejercicios de un día. El día NO desaparece: queda como
    // entrenamiento con 0 ejercicios (el usuario puede volver a agregar).
    suspend fun clearExercisesFromDay(dayId: String)

    // Mueve los ejercicios de un día a un día de descanso. El origen NO se
    // borra: pasa a ser día de descanso (queda vacío). El destino pasa a ser
    // de entrenamiento y recibe los ejercicios al final de su lista.
    suspend fun moveExercisesToRestDay(fromDayId: String, toDayId: String)
}