package com.DeBiaseRamiro.gymera.domain.model

data class Routine(
    val id: String = "",
    val goal: String = "",
    val level: String = "",
    val daysPerWeek: Int = 0,
    val workoutDays: List<WorkoutDay> = emptyList()
)

data class WorkoutDay(
    val id: String = "",
    val dayName: String = "",       // "Lunes", "Martes", etc.
    val dayOrder: Int = 0,
    val isRestDay: Boolean = false,
    val muscleFocus: String = "",
    val exercises: List<Exercise> = emptyList()
)

data class Exercise(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
    val muscleGroup: String = "",
    val sets: Int = 0,
    val reps: String = "",          // "8-12" o "15"
    val restSeconds: Int = 0,
    val notes: String = ""
)