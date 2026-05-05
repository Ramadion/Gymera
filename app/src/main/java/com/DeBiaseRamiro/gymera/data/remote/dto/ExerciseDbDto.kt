package com.DeBiaseRamiro.gymera.data.remote.dto

/**
 * ExerciseDbDto — respuesta de ExerciseDB por ejercicio.
 *
 * Campos que nos interesan:
 * - id:                 ID del ejercicio (4 chars, ej: "3235") → para construir la URL de imagen
 * - name:               Nombre en inglés
 * - bodyPart:           Parte del cuerpo (chest, back, legs, etc.)
 * - target:             Músculo principal objetivo
 * - secondaryMuscles:   Lista de músculos secundarios
 * - instructions:       Pasos del ejercicio
 * - description:        Descripción general
 * - difficulty:         Nivel de dificultad
 *
 * El GIF se obtiene construyendo esta URL:
 * https://exercisedb.p.rapidapi.com/image?exerciseId={id}&resolution=180
 */
data class ExerciseDbDto(
    val id: String = "",
    val name: String = "",
    val bodyPart: String = "",
    val target: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val description: String = "",
    val difficulty: String = "",
    val category: String = ""
)