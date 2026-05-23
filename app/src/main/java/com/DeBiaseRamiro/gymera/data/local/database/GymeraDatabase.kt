package com.DeBiaseRamiro.gymera.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.DeBiaseRamiro.gymera.data.local.dao.ExerciseCacheDao
import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.entity.*

@Database(
    entities = [
        RoutineEntity::class,
        WorkoutDayEntity::class,
        ExerciseAssignmentEntity::class,
        ExerciseCacheEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GymeraDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseCacheDao(): ExerciseCacheDao
    abstract fun userProfileDao(): UserProfileDao
}