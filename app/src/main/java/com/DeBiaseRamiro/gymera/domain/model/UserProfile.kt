package com.DeBiaseRamiro.gymera.domain.model

data class UserProfile(
    val goal: String = "",              // Objetivo del usuario
    val daysPerWeek: Int = 0,           // Días de entrenamiento por semana
    val sessionDuration: Int = 0,       // Duración de sesión en minutos
    val level: String = "",             // Nivel de experiencia
    val limitations: String = ""        // Lesiones o limitaciones físicas
)