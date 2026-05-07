package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Cachea el JSON completo de free-exercise-db en Room.
// Se descarga UNA SOLA VEZ en la vida de la app y nunca más se toca la red
// para obtener datos de ejercicios.
// primaryMuscles y secondaryMuscles se guardan como JSON string porque Room
// no soporta List<String> nativamente sin un TypeConverter.
@Entity(tableName = "exercise_cache")
data class ExerciseCacheEntity(
    @PrimaryKey val id: String,      // ID del ejercicio en free-exercise-db
    val name: String,                // nombre en inglés
    val primaryMuscles: String,      // JSON array serializado: ["chest","shoulders"]
    val secondaryMuscles: String,    // JSON array serializado
    val equipment: String,
    val level: String,
    val category: String,
    val imageUrl: String,            // URL completa de la primera imagen
    val imageUrl2: String,           // URL de la segunda imagen (para la animación)
    val instructions: String,        // JSON array serializado de strings
    val cachedAt: Long               // timestamp de cuándo se guardó
)