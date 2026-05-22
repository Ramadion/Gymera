package com.DeBiaseRamiro.gymera.domain.model

data class UserPhysicalProfile(
    val uid: String = "",
    val age: Int = 0,           // años
    val weightKg: Float = 0f,   // kilogramos
    val heightCm: Int = 0,      // centímetros
    val gender: String = ""     // "Masculino" / "Femenino" / "No especificado"
)