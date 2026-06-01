package com.DeBiaseRamiro.gymera.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,           // v3 → v4: agrega columna instructionsEs en exercise_cache
    exportSchema = false
)
abstract class GymeraDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseCacheDao(): ExerciseCacheDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        // ── Migration 3 → 4 ───────────────────────────────────────────────────
        // Agrega la columna instructionsEs a exercise_cache.
        //
        // DEFAULT '' porque Room no permite columnas NOT NULL sin default en
        // migraciones ALTER TABLE. Los registros existentes quedarán con ""
        // (string vacío) y serán reemplazados la próxima vez que se cargue
        // el asset gymera_exercises.json (que ya trae las traducciones).
        //
        // Nota: si usás fallbackToDestructiveMigration() en el builder, Room
        // borra y recrea la base al detectar versión incompatible, por lo que
        // esta migration solo aplica a usuarios que ya tenían la v3 instalada.
        // Para desarrollo es indistinto — en producción es importante tenerla.
        // ─────────────────────────────────────────────────────────────────────
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercise_cache ADD COLUMN instructionsEs TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}