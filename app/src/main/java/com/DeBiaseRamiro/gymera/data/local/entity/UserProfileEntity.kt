package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Almacena los datos físicos del usuario localmente.
// Se usa para alimentar el prompt de Gemini con información relevante
// sin necesidad de llamar a Firestore en cada generación de rutina.
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Int,
    val gender: String
)