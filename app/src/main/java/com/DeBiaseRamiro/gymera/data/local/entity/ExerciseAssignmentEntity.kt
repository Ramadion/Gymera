package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Representa un ejercicio asignado a un día específico.
// ForeignKey a workout_day.id con CASCADE: si se borra el día, se borran sus ejercicios.
@Entity(
    tableName = "exercise_assignment",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutDayId")]
)
data class ExerciseAssignmentEntity(
    @PrimaryKey val id: String,
    val workoutDayId: String,    // FK → workout_day.id
    val nameEs: String,          // nombre en español (para mostrar)
    val nameEn: String,          // nombre en inglés (para buscar en free-exercise-db)
    val muscleGroup: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val orderInDay: Int,         // posición del ejercicio dentro del día
    val notes: String
)