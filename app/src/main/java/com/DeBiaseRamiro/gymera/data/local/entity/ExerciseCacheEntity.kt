package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Cachea el JSON bundleado (assets/gymera_exercises.json) en Room.
// Se carga UNA SOLA VEZ al primer arranque de la app, leyendo desde assets —
// sin llamadas de red. A partir de ese momento todas las pantallas leen de Room.
//
// primaryMuscles, secondaryMuscles, instructions e instructionsEs se guardan
// como JSON string porque Room no soporta List<String> sin TypeConverter.
//
// ── Por qué instructionsEs ya no es nullable (vs versión anterior) ────────────
// Antes era nullable porque se planeaba traducir lazy con Gemini.
// Ahora el JSON ya viene traducido desde el asset, así que instructionsEs
// siempre tiene valor. Si por algún edge case llegara vacío ("[]"), la UI
// usa instructions (inglés) como fallback — nunca queda sin mostrar nada.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "exercise_cache")
data class ExerciseCacheEntity(
    @PrimaryKey val id: String,          // ID del ejercicio en free-exercise-db
    val name: String,                    // nombre en inglés
    val primaryMuscles: String,          // JSON array serializado: ["chest","shoulders"]
    val secondaryMuscles: String,        // JSON array serializado
    val equipment: String,
    val level: String,
    val category: String,
    val imageUrl: String,                // URL completa de la primera imagen
    val imageUrl2: String,               // URL de la segunda imagen (para la animación)
    val instructions: String,            // JSON array serializado de instrucciones en inglés
    val instructionsEs: String,          // JSON array serializado de instrucciones en español
    val cachedAt: Long                   // timestamp de cuándo se cargó desde el asset
)