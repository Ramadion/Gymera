package com.DeBiaseRamiro.gymera.data.local.dao

import androidx.room.*
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseAssignmentEntity
import com.DeBiaseRamiro.gymera.data.local.entity.RoutineEntity
import com.DeBiaseRamiro.gymera.data.local.entity.WorkoutDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    // ── INSERT ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseAssignmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseAssignmentEntity)

    // ── QUERY ─────────────────────────────────────────────────────────────

    // ── BUG FIX: JOIN con workout_day y exercise_assignment ───────────────
    // Antes: query solo referenciaba `routine`. Room's InvalidationTracker
    // solo re-emite el Flow cuando cambia una tabla de la query. Agregar o
    // eliminar ejercicios modifica `exercise_assignment` (no `routine`),
    // así que el Flow no emitía y la UI quedaba desactualizada.
    //
    // Ahora: LEFT JOIN con workout_day y exercise_assignment hace que Room
    // observe las tres tablas. Cualquier cambio en exercises dispara una
    // nueva emisión, propagando la actualización por toda la cadena:
    //   RoutineDao → RoutineRepositoryImpl → SharedRoutineViewModel → NavGraph
    //
    // LEFT JOIN (no INNER JOIN) para que la rutina se devuelva aunque todos
    // los días sean descanso (sin ningún exercise_assignment).
    // LIMIT 1 alcanza porque todos los campos son de `r` — la misma fila de
    // routine — y cualquiera de las filas duplicadas del JOIN sirve.
    // ─────────────────────────────────────────────────────────────────────
    @Query("""
        SELECT r.id, r.userUid, r.goal, r.daysPerWeek, r.sessionDuration,
               r.level, r.limitations, r.generatedAt, r.isActive
        FROM routine r
        LEFT JOIN workout_day wd ON wd.routineId = r.id
        LEFT JOIN exercise_assignment ea ON ea.workoutDayId = wd.id
        WHERE r.userUid = :uid AND r.isActive = 1
        LIMIT 1
    """)
    fun getActiveRoutineFlow(uid: String): Flow<RoutineEntity?>

    // Versión suspend para el Splash (lee una sola vez, no necesita Flow)
    @Query("SELECT * FROM routine WHERE userUid = :uid AND isActive = 1 LIMIT 1")
    suspend fun getActiveRoutine(uid: String): RoutineEntity?

    @Query("SELECT * FROM workout_day WHERE routineId = :routineId ORDER BY dayOrder ASC")
    suspend fun getWorkoutDays(routineId: String): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_day WHERE id = :dayId LIMIT 1")
    suspend fun getWorkoutDayById(dayId: String): WorkoutDayEntity?

    @Query("SELECT * FROM exercise_assignment WHERE workoutDayId = :dayId ORDER BY orderInDay ASC")
    suspend fun getExercisesForDay(dayId: String): List<ExerciseAssignmentEntity>

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Query("UPDATE routine SET isActive = 0 WHERE userUid = :uid")
    suspend fun deactivateAllRoutines(uid: String)

    @Query("UPDATE exercise_assignment SET orderInDay = :order WHERE id = :exerciseId")
    suspend fun updateExerciseOrder(exerciseId: String, order: Int)

    // Marca un día como descanso (1) o entrenamiento (0) sin tocar sus ejercicios.
    // Los ejercicios en silencio quedan guardados y vuelven a aparecer al reactivar.
    @Query("UPDATE workout_day SET isRestDay = :isRest WHERE id = :dayId")
    suspend fun updateWorkoutDayIsRest(dayId: String, isRest: Int)

    // Actualiza la descripción/enfoque del día (muscleFocus). Puede ser vacía.
    @Query("UPDATE workout_day SET muscleFocus = :muscleFocus WHERE id = :dayId")
    suspend fun updateWorkoutDayMuscleFocus(dayId: String, muscleFocus: String)

    // Mueve todos los ejercicios de un día a otro (cambia su workoutDayId).
    @Query("UPDATE exercise_assignment SET workoutDayId = :newDayId WHERE workoutDayId = :oldDayId")
    suspend fun reassignExercises(oldDayId: String, newDayId: String)

    // ── DELETE ────────────────────────────────────────────────────────────

    @Query("DELETE FROM routine WHERE userUid = :uid AND isActive = 0")
    suspend fun deleteInactiveRoutines(uid: String)

    @Query("DELETE FROM routine WHERE userUid = :uid")
    suspend fun deleteAllRoutines(uid: String)

    @Query("DELETE FROM exercise_assignment WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: String)

    // Borra todos los ejercicios de un día, pero el día NO se elimina:
    // queda como entrenamiento con 0 ejercicios.
    @Query("DELETE FROM exercise_assignment WHERE workoutDayId = :dayId")
    suspend fun deleteExercisesForDay(dayId: String)
}