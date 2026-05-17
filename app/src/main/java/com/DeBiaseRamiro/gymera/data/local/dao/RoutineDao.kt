package com.DeBiaseRamiro.gymera.data.local.dao

import androidx.room.*
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseAssignmentEntity
import com.DeBiaseRamiro.gymera.data.local.entity.RoutineEntity
import com.DeBiaseRamiro.gymera.data.local.entity.WorkoutDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    // ── INSERT ────────────────────────────────────────────────────────────

    // REPLACE reemplaza la fila si ya existe el mismo PrimaryKey
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseAssignmentEntity>)

    // ── QUERY ─────────────────────────────────────────────────────────────

    // Flow: emite automáticamente cada vez que cambia la tabla routine.
    // La UI observa este Flow y se actualiza sola sin polling.
    @Query("SELECT * FROM routine WHERE userUid = :uid AND isActive = 1 LIMIT 1")
    fun getActiveRoutineFlow(uid: String): Flow<RoutineEntity?>

    // Versión suspend para el Splash (necesita el valor una sola vez, no un stream)
    @Query("SELECT * FROM routine WHERE userUid = :uid AND isActive = 1 LIMIT 1")
    suspend fun getActiveRoutine(uid: String): RoutineEntity?

    @Query("SELECT * FROM workout_day WHERE routineId = :routineId ORDER BY dayOrder ASC")
    suspend fun getWorkoutDays(routineId: String): List<WorkoutDayEntity>

    @Query("SELECT * FROM exercise_assignment WHERE workoutDayId = :dayId ORDER BY orderInDay ASC")
    suspend fun getExercisesForDay(dayId: String): List<ExerciseAssignmentEntity>

    // ── UPDATE ────────────────────────────────────────────────────────────

    // Desactiva todas las rutinas del usuario antes de activar la nueva
    @Query("UPDATE routine SET isActive = 0 WHERE userUid = :uid")
    suspend fun deactivateAllRoutines(uid: String)

    // ── DELETE ────────────────────────────────────────────────────────────

    // Borra rutinas inactivas para no acumular basura en la DB
    @Query("DELETE FROM routine WHERE userUid = :uid AND isActive = 0")
    suspend fun deleteInactiveRoutines(uid: String)

    // RoutineDao.kt — agregar
    @Query("DELETE FROM routine WHERE userUid = :uid")
    suspend fun deleteAllRoutines(uid: String)
}