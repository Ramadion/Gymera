package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Representa una rutina generada por Gemini en la base de datos local.
// is_active = 1 significa que es la rutina que el usuario está usando actualmente.
// Cuando se genera una nueva, la anterior pasa a is_active = 0.
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val userUid: String,         // Firebase UID del dueño
    val goal: String,
    val daysPerWeek: Int,
    val sessionDuration: Int,
    val level: String,
    val limitations: String,
    val generatedAt: Long,       // timestamp Unix en ms
    val isActive: Int            // 1 = activa, 0 = inactiva
)