package com.DeBiaseRamiro.gymera.domain.repository

import com.DeBiaseRamiro.gymera.domain.model.Routine

interface FirestoreRepository {

    // Sube la rutina activa a Firestore y marca hasActiveRoutine = true en el usuario
    suspend fun syncRoutineToCloud(routine: Routine, userUid: String)

    // Baja la rutina activa desde Firestore — se usa cuando Room está vacío
    // (dispositivo nuevo o app reinstalada)
    suspend fun fetchRoutineFromCloud(userUid: String): Routine?

    // Marca hasActiveRoutine = false en el documento del usuario
    // Se llama cuando el usuario genera una nueva rutina
    suspend fun deactivateCloudRoutine(userUid: String)
}