// domain/repository/RoutineRepository.kt

package com.DeBiaseRamiro.gymera.domain.repository

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
}