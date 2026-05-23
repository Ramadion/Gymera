package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Int,
    val gender: String,
    val birthDateMillis: Long
)