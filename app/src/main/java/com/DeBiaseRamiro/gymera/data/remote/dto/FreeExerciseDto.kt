package com.DeBiaseRamiro.gymera.data.remote.dto

// DTO que modela cada ejercicio del JSON bundleado en assets/gymera_exercises.json.
//
// Este archivo fue generado UNA SOLA VEZ con translate_exercises.py y ya no
// se descarga de la red en runtime. Todos los campos son los originales de
// free-exercise-db más el campo instructionsEs que agregamos nosotros.
//
// Nota: Gson ignora campos que no existen en el JSON (no crashea si algún
// ejercicio no tiene instructionsEs — devuelve emptyList() por el default).
data class FreeExerciseDto(
    val id: String = "",
    val name: String = "",
    val force: String? = null,
    val level: String = "",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val category: String = "",
    val images: List<String> = emptyList(),

    // Instrucciones originales en inglés (del repositorio free-exercise-db)
    val instructions: List<String> = emptyList(),

    // Instrucciones traducidas al español por translate_exercises.py + Gemini.
    // Siempre disponibles desde el asset — sin llamadas de red, sin latencia.
    // Si un ejercicio no fue traducido (edge case), la app usa instructions como fallback.
    val instructionsEs: List<String> = emptyList()
)



/** * El campo más importante para nosotros es images[0], que combinado con
 * la base URL nos da la imagen:
 * https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/{images[0]}
 *
 * Ejemplo de images[0]: "Alternate_Incline_Dumbbell_Curl/0.jpg"
 */