package com.DeBiaseRamiro.gymera.domain.repository

import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {

    //genera la rutina via Gemini
    suspend fun generateRoutine(userProfile: UserProfile): Routine

    //guarda la rutina completa en Room (rutina + días + ejercicios)
    suspend fun saveRoutine(routine: Routine, userUid: String)

    //Flow que emite la rutina activa cada vez que cambia en Room
    // La UI observa esto y se actualiza automáticamente
    fun getActiveRoutineFlow(userUid: String): Flow<Routine?>

    //versión suspend para el Splash (una sola lectura)
    suspend fun getActiveRoutine(userUid: String): Routine?

    suspend fun deactivateActiveRoutine(userUid: String)
}