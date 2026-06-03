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
}