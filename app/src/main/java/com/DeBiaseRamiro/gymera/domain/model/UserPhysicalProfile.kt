package com.DeBiaseRamiro.gymera.domain.model

data class UserPhysicalProfile(
    val uid: String = "",
    val age: Int = 0,
    val weightKg: Float = 0f,
    val heightCm: Int = 0,
    val gender: String = "",
    val birthDateMillis: Long = 0L
)