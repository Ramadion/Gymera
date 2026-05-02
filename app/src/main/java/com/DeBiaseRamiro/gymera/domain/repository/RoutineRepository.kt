package com.DeBiaseRamiro.gymera.domain.repository

import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile

interface RoutineRepository {
    suspend fun generateRoutine(userProfile: UserProfile): Routine
}