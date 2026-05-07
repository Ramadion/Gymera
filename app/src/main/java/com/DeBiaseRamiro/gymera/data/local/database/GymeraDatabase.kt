package com.DeBiaseRamiro.gymera.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.DeBiaseRamiro.gymera.data.local.dao.ExerciseCacheDao
import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.entity.*

// version = 1: primera versión del esquema.
// exportSchema = false: no genera archivos de esquema JSON (activar en producción final)
@Database(
    entities = [
        RoutineEntity::class,
        WorkoutDayEntity::class,
        ExerciseAssignmentEntity::class,
        ExerciseCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GymeraDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseCacheDao(): ExerciseCacheDao
}