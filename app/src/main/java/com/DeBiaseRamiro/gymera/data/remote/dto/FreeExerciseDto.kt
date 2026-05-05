package com.DeBiaseRamiro.gymera.data.remote.dto

/**
 * FreeExerciseDto — estructura de cada ejercicio del repo free-exercise-db.
 *
 * El campo más importante para nosotros es images[0], que combinado con
 * la base URL nos da la imagen:
 * https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/{images[0]}
 *
 * Ejemplo de images[0]: "Alternate_Incline_Dumbbell_Curl/0.jpg"
 */
data class FreeExerciseDto(
    val id: String = "",                          // ej: "Alternate_Incline_Dumbbell_Curl"
    val name: String = "",                         // ej: "Alternate Incline Dumbbell Curl"
    val force: String? = null,
    val level: String = "",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "",
    val images: List<String> = emptyList()         // ej: ["Alternate_Incline_Dumbbell_Curl/0.jpg"]
)