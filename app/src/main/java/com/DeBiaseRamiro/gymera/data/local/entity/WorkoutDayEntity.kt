package com.DeBiaseRamiro.gymera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Representa un día de la semana dentro de una rutina.
// ForeignKey a routine.id con CASCADE: si se borra la rutina, se borran sus días.
@Entity(
    tableName = "workout_day",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE  // al borrar rutina, se borran sus días
        )
    ],
    indices = [Index("routineId")]  // índice para acelerar queries por routineId
)
data class WorkoutDayEntity(
    @PrimaryKey val id: String,
    val routineId: String,       // FK → routine.id
    val dayName: String,
    val dayOrder: Int,
    val isRestDay: Int,          // Room no soporta Boolean directamente: 1=true, 0=false
    val muscleFocus: String
)