package com.DeBiaseRamiro.gymera.data.repository

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseAssignmentEntity
import com.DeBiaseRamiro.gymera.data.local.entity.RoutineEntity
import com.DeBiaseRamiro.gymera.data.local.entity.WorkoutDayEntity
import com.DeBiaseRamiro.gymera.data.repository.ai.FailoverRoutineGenerator
import com.DeBiaseRamiro.gymera.domain.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineRepositoryImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockAiGenerator = mockk<FailoverRoutineGenerator>()
    private val mockRoutineDao  = mockk<RoutineDao>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildTestRoutine(
        id: String = UUID.randomUUID().toString(),
        exercises: List<Exercise> = listOf(
            Exercise(UUID.randomUUID().toString(), "Press de banca", "bench press", "Pecho", 3, "10", 60, "")
        )
    ) = Routine(
        id = id, goal = "MUSCLE_GAIN", level = "INTERMEDIATE", daysPerWeek = 3,
        workoutDays = listOf(
            WorkoutDay("day-1", "Lunes", 1, false, "Pecho", exercises),
            WorkoutDay("day-2", "Martes", 2, true, "", emptyList())
        )
    )

    private fun buildRoutineEntity(id: String = "routine-1") = RoutineEntity(
        id = id, userUid = "test-uid", goal = "MUSCLE_GAIN", daysPerWeek = 3,
        sessionDuration = 60, level = "INTERMEDIATE", limitations = "",
        generatedAt = System.currentTimeMillis(), isActive = 1
    )

    private fun buildDayEntity(routineId: String = "routine-1") = WorkoutDayEntity(
        id = "day-1", routineId = routineId, dayName = "Lunes",
        dayOrder = 1, isRestDay = 0, muscleFocus = "Pecho"
    )

    private fun buildExerciseEntity(dayId: String = "day-1") = ExerciseAssignmentEntity(
        id = "ex-1", workoutDayId = dayId, nameEs = "Press de banca",
        nameEn = "bench press", muscleGroup = "Pecho",
        sets = 3, reps = "10", restSeconds = 60, orderInDay = 0, notes = ""
    )

    private fun createRepository() = RoutineRepositoryImpl(
        aiGenerator = mockAiGenerator,
        routineDao  = mockRoutineDao
    )

    // ── generateRoutine (failover multicanal) ─────────────────────────────

    private val sampleJson = """
        {
          "workoutDays": [
            {
              "dayName": "Lunes",
              "dayOrder": 1,
              "isRestDay": false,
              "muscleFocus": "Pecho y Triceps",
              "exercises": [
                {
                  "name": "Press de banca",
                  "nameEn": "barbell bench press",
                  "muscleGroup": "Pecho",
                  "sets": 4,
                  "reps": "8-12",
                  "restSeconds": 90,
                  "notes": "Ajustar peso"
                }
              ]
            },
            {
              "dayName": "Martes",
              "dayOrder": 2,
              "isRestDay": true,
              "muscleFocus": "",
              "exercises": []
            }
          ]
        }
    """.trimIndent()

    private val userProfile = UserProfile(
        goal = "MUSCLE_GAIN",
        daysPerWeek = 1,
        sessionDuration = 90,
        level = "INTERMEDIATE",
        limitations = ""
    )

    @Test
    fun `generateRoutine delega al failover y parsea el JSON del proveedor`() = runTest {
        coEvery { mockAiGenerator.generate(any(), any()) } answers {
            secondArg<(String) -> Routine>()(sampleJson)
        }

        val repo = createRepository()
        val routine = repo.generateRoutine(userProfile, null)

        assertNotNull(routine)
        assertEquals(2, routine.workoutDays.size)
        assertEquals("barbell bench press", routine.workoutDays[0].exercises[0].nameEn)
        coVerify(exactly = 1) { mockAiGenerator.generate(any(), any()) }
    }

    @Test
    fun `generateRoutine construye el prompt con la regla de cantidad por duracion`() = runTest {
        val promptSlot = slot<String>()
        coEvery { mockAiGenerator.generate(capture(promptSlot), any()) } answers {
            secondArg<(String) -> Routine>()(sampleJson)
        }

        val repo = createRepository()
        repo.generateRoutine(userProfile, null)

        val prompt = promptSlot.captured
        assertTrue(prompt.contains("Duracion por sesion: 90 minutos"))
        assertTrue(prompt.contains("7 a 8 ejercicios"))
        assertTrue(prompt.contains("dolor o lesion"))
    }

    @Test
    fun `generateRoutine propaga la excepcion cuando todos los proveedores fallan`() = runTest {
        coEvery { mockAiGenerator.generate(any(), any()) } throws
            Exception("Todos los servicios de IA fallaron")

        val repo = createRepository()
        var threw = false
        try {
            repo.generateRoutine(userProfile, null)
        } catch (e: Exception) {
            threw = true
        }
        assertTrue("Se esperaba excepción cuando el failover falla", threw)
    }

    @Test
    fun `generateRoutine unwrappe el JSON cuando la IA lo envuelve en weeklyWorkoutPlan`() = runTest {
        val wrappedJson = """
            {
              "weeklyWorkoutPlan": {
                "workoutDays": [
                  {
                    "dayName": "Lunes",
                    "dayOrder": 1,
                    "isRestDay": false,
                    "muscleFocus": "Pecho y Triceps",
                    "exercises": [
                      {
                        "name": "Press de banca",
                        "nameEn": "barbell bench press",
                        "muscleGroup": "Pecho",
                        "sets": 4,
                        "reps": "8-12",
                        "restSeconds": 90,
                        "notes": "Ajustar peso"
                      }
                    ]
                  },
                  {
                    "dayName": "Martes",
                    "dayOrder": 2,
                    "isRestDay": true,
                    "muscleFocus": "",
                    "exercises": []
                  }
                ]
              }
            }
        """.trimIndent()

        coEvery { mockAiGenerator.generate(any(), any()) } answers {
            secondArg<(String) -> Routine>()(wrappedJson)
        }

        val repo = createRepository()
        val routine = repo.generateRoutine(userProfile, null)

        assertNotNull(routine)
        assertEquals(2, routine.workoutDays.size)
        assertEquals("barbell bench press", routine.workoutDays[0].exercises[0].nameEn)
    }

    @Test
    fun `generateRoutine lanza excepcion cuando la IA devuelve JSON sin workoutDays`() = runTest {
        val badJson = """{"mensaje": "no tengo days"}"""

        coEvery { mockAiGenerator.generate(any(), any()) } answers {
            secondArg<(String) -> Routine>()(badJson)
        }

        val repo = createRepository()
        var threw = false
        try {
            repo.generateRoutine(userProfile, null)
        } catch (e: Exception) {
            threw = true
        }
        assertTrue("Se esperaba excepción cuando el JSON no tiene workoutDays", threw)
    }

    // ── saveRoutine ───────────────────────────────────────────────────────

    @Test
    fun `saveRoutine borra rutinas anteriores antes de insertar`() = runTest {
        val routine = buildTestRoutine()
        coEvery { mockRoutineDao.deleteAllRoutines("test-uid") }     just Runs
        coEvery { mockRoutineDao.insertRoutine(any()) }              just Runs
        coEvery { mockRoutineDao.insertWorkoutDays(any()) }          just Runs
        coEvery { mockRoutineDao.insertExercises(any()) }            just Runs

        val repo = createRepository()
        repo.saveRoutine(routine, "test-uid")

        // deleteAllRoutines debe ejecutarse antes que insertRoutine
        coVerifyOrder {
            mockRoutineDao.deleteAllRoutines("test-uid")
            mockRoutineDao.insertRoutine(any())
        }
    }

    @Test
    fun `saveRoutine persiste la rutina con el uid correcto`() = runTest {
        val routine = buildTestRoutine("my-routine")
        coEvery { mockRoutineDao.deleteAllRoutines(any()) }   just Runs
        coEvery { mockRoutineDao.insertRoutine(any()) }       just Runs
        coEvery { mockRoutineDao.insertWorkoutDays(any()) }   just Runs
        coEvery { mockRoutineDao.insertExercises(any()) }     just Runs

        val repo = createRepository()
        repo.saveRoutine(routine, "test-uid")

        coVerify {
            mockRoutineDao.insertRoutine(match { it.userUid == "test-uid" && it.id == "my-routine" })
        }
    }

    @Test
    fun `saveRoutine inserta los 7 dias de la semana`() = runTest {
        val workoutDays = (1..7).map { i ->
            WorkoutDay("day-$i", "Día $i", i, i > 4, "", emptyList())
        }
        val routine = Routine(UUID.randomUUID().toString(), "ENDURANCE", "BEGINNER", 4, workoutDays)

        coEvery { mockRoutineDao.deleteAllRoutines(any()) }   just Runs
        coEvery { mockRoutineDao.insertRoutine(any()) }       just Runs
        val daysSlot = slot<List<WorkoutDayEntity>>()
        coEvery { mockRoutineDao.insertWorkoutDays(capture(daysSlot)) } just Runs
        coEvery { mockRoutineDao.insertExercises(any()) }     just Runs

        val repo = createRepository()
        repo.saveRoutine(routine, "test-uid")

        assertEquals(7, daysSlot.captured.size)
    }

    @Test
    fun `saveRoutine mapea isRestDay correctamente a entero`() = runTest {
        val routine = Routine(
            id = "r1", goal = "TONING", level = "BEGINNER", daysPerWeek = 3,
            workoutDays = listOf(
                WorkoutDay("d1", "Lunes",   1, false, "Pecho",  emptyList()),
                WorkoutDay("d2", "Martes",  2, true,  "",       emptyList())
            )
        )

        coEvery { mockRoutineDao.deleteAllRoutines(any()) }   just Runs
        coEvery { mockRoutineDao.insertRoutine(any()) }       just Runs
        val daysSlot = slot<List<WorkoutDayEntity>>()
        coEvery { mockRoutineDao.insertWorkoutDays(capture(daysSlot)) } just Runs
        coEvery { mockRoutineDao.insertExercises(any()) }     just Runs

        val repo = createRepository()
        repo.saveRoutine(routine, "test-uid")

        assertEquals(0, daysSlot.captured[0].isRestDay) // Lunes = entrenamiento
        assertEquals(1, daysSlot.captured[1].isRestDay) // Martes = descanso
    }

    // ── removeExercise ────────────────────────────────────────────────────

    @Test
    fun `removeExercise delega al DAO con el id correcto`() = runTest {
        coEvery { mockRoutineDao.deleteExercise("ex-123") } just Runs

        val repo = createRepository()
        repo.removeExercise("ex-123")

        coVerify(exactly = 1) { mockRoutineDao.deleteExercise("ex-123") }
    }

    // ── reorderExercises ──────────────────────────────────────────────────

    @Test
    fun `reorderExercises actualiza el orden de cada ejercicio`() = runTest {
        val exercises = listOf(
            Exercise("ex-1", "Bench", "bench press", "Pecho", 3, "10", 60, ""),
            Exercise("ex-2", "Squat", "squat",       "Piernas", 4, "8",  90, ""),
            Exercise("ex-3", "Row",   "barbell row",  "Espalda", 3, "10", 60, "")
        )
        coEvery { mockRoutineDao.updateExerciseOrder(any(), any()) } just Runs

        val repo = createRepository()
        repo.reorderExercises(exercises)

        coVerify { mockRoutineDao.updateExerciseOrder("ex-1", 0) }
        coVerify { mockRoutineDao.updateExerciseOrder("ex-2", 1) }
        coVerify { mockRoutineDao.updateExerciseOrder("ex-3", 2) }
    }

    @Test
    fun `reorderExercises con lista vacia no llama al DAO`() = runTest {
        val repo = createRepository()
        repo.reorderExercises(emptyList())

        coVerify(exactly = 0) { mockRoutineDao.updateExerciseOrder(any(), any()) }
    }

    // ── addExercise ───────────────────────────────────────────────────────

    @Test
    fun `addExercise inserta entidad con los campos correctos`() = runTest {
        val exercise = Exercise("new-ex", "Sentadilla", "squat", "Piernas", 4, "8-10", 90, "Mantener la espalda recta")
        val entitySlot = slot<ExerciseAssignmentEntity>()
        coEvery { mockRoutineDao.insertExercise(capture(entitySlot)) } just Runs

        val repo = createRepository()
        repo.addExercise("day-5", exercise, 3)

        val entity = entitySlot.captured
        assertEquals("new-ex",     entity.id)
        assertEquals("day-5",      entity.workoutDayId)
        assertEquals("Sentadilla", entity.nameEs)
        assertEquals("squat",      entity.nameEn)
        assertEquals("Piernas",    entity.muscleGroup)
        assertEquals(4,            entity.sets)
        assertEquals("8-10",       entity.reps)
        assertEquals(90,           entity.restSeconds)
        assertEquals(3,            entity.orderInDay)
    }

    // ── deactivateActiveRoutine ───────────────────────────────────────────

    @Test
    fun `deactivateActiveRoutine llama a deactivateAllRoutines en el DAO`() = runTest {
        coEvery { mockRoutineDao.deactivateAllRoutines("test-uid") } just Runs

        val repo = createRepository()
        repo.deactivateActiveRoutine("test-uid")

        coVerify(exactly = 1) { mockRoutineDao.deactivateAllRoutines("test-uid") }
    }

    // ── setWorkoutDayRest ─────────────────────────────────────────────────

    @Test
    fun `setWorkoutDayRest marca el dia como descanso`() = runTest {
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-1", 1) } just Runs

        val repo = createRepository()
        repo.setWorkoutDayRest("day-1", true)

        coVerify(exactly = 1) { mockRoutineDao.updateWorkoutDayIsRest("day-1", 1) }
    }

    @Test
    fun `setWorkoutDayRest reactiva un dia de descanso`() = runTest {
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-1", 0) } just Runs

        val repo = createRepository()
        repo.setWorkoutDayRest("day-1", false)

        coVerify(exactly = 1) { mockRoutineDao.updateWorkoutDayIsRest("day-1", 0) }
    }

    // ── setWorkoutDayMuscleFocus ─────────────────────────────────────────

    @Test
    fun `setWorkoutDayMuscleFocus delega al DAO con el texto correcto`() = runTest {
        coEvery { mockRoutineDao.updateWorkoutDayMuscleFocus("day-1", "Pecho y Triceps") } just Runs

        val repo = createRepository()
        repo.setWorkoutDayMuscleFocus("day-1", "Pecho y Triceps")

        coVerify(exactly = 1) { mockRoutineDao.updateWorkoutDayMuscleFocus("day-1", "Pecho y Triceps") }
    }

    // ── clearExercisesFromDay ─────────────────────────────────────────────

    @Test
    fun `clearExercisesFromDay delega al DAO con el id correcto`() = runTest {
        coEvery { mockRoutineDao.deleteExercisesForDay("day-1") } just Runs

        val repo = createRepository()
        repo.clearExercisesFromDay("day-1")

        coVerify(exactly = 1) { mockRoutineDao.deleteExercisesForDay("day-1") }
    }

    // ── moveExercisesToRestDay ───────────────────────────────────────────

    @Test
    fun `moveExercisesToRestDay mueve ejercicios activa destino y deja el origen como descanso`() = runTest {
        coEvery { mockRoutineDao.getWorkoutDayById("day-1") } returns WorkoutDayEntity("day-1", "routine-1", "Lunes", 1, 0, "Pecho")
        coEvery { mockRoutineDao.reassignExercises("day-1", "day-rest") } just Runs
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-rest", 0) }   just Runs
        coEvery { mockRoutineDao.updateWorkoutDayMuscleFocus("day-rest", "Pecho") } just Runs
        coEvery { mockRoutineDao.updateWorkoutDayMuscleFocus("day-1", "") } just Runs
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-1", 1) }     just Runs
        coEvery { mockRoutineDao.getExercisesForDay("day-rest") } returns listOf(
            ExerciseAssignmentEntity("m1", "day-rest", "Press de banca", "bench press", "Pecho", 3, "10", 60, 0, ""),
            ExerciseAssignmentEntity("m2", "day-rest", "Sentadilla",      "squat",        "Piernas", 4, "8",  90, 1, "")
        )
        coEvery { mockRoutineDao.updateExerciseOrder(any(), any()) } just Runs

        val repo = createRepository()
        repo.moveExercisesToRestDay("day-1", "day-rest")

        // El orden de las operaciones importa: primero mover, luego activar
        // el destino y copiar la descripción, y al final marcar el origen como descanso.
        coVerifyOrder {
            mockRoutineDao.reassignExercises("day-1", "day-rest")
            mockRoutineDao.updateWorkoutDayIsRest("day-rest", 0)
            mockRoutineDao.updateWorkoutDayMuscleFocus("day-rest", "Pecho")
            mockRoutineDao.updateWorkoutDayIsRest("day-1", 1)
            mockRoutineDao.updateWorkoutDayMuscleFocus("day-1", "")
        }
        // El destino queda renumerado de forma secuencial (0..n-1)
        coVerify(exactly = 1) { mockRoutineDao.updateExerciseOrder("m1", 0) }
        coVerify(exactly = 1) { mockRoutineDao.updateExerciseOrder("m2", 1) }
    }

    @Test
    fun `moveExercisesToRestDay deja el origen como descanso aunque no haya ejercicios`() = runTest {
        coEvery { mockRoutineDao.getWorkoutDayById("day-1") } returns WorkoutDayEntity("day-1", "routine-1", "Lunes", 1, 0, "Piernas")
        coEvery { mockRoutineDao.reassignExercises("day-1", "day-rest") } just Runs
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-rest", 0) }   just Runs
        coEvery { mockRoutineDao.updateWorkoutDayMuscleFocus("day-rest", "Piernas") } just Runs
        coEvery { mockRoutineDao.updateWorkoutDayIsRest("day-1", 1) }     just Runs
        coEvery { mockRoutineDao.updateWorkoutDayMuscleFocus("day-1", "") } just Runs
        coEvery { mockRoutineDao.getExercisesForDay("day-rest") }          returns emptyList()
        coEvery { mockRoutineDao.updateExerciseOrder(any(), any()) }       just Runs

        val repo = createRepository()
        repo.moveExercisesToRestDay("day-1", "day-rest")

        // Sin ejercicios no hay nada que renumerar
        coVerify(exactly = 0) { mockRoutineDao.updateExerciseOrder(any(), any()) }
        coVerify(exactly = 1) { mockRoutineDao.updateWorkoutDayIsRest("day-1", 1) }
    }

    // ── getActiveRoutineFlow ──────────────────────────────────────────────

    @Test
    fun `getActiveRoutineFlow mapea entidad a dominio correctamente`() = runTest {
        val entity     = buildRoutineEntity()
        val dayEntity  = buildDayEntity()
        val exEntity   = buildExerciseEntity()

        every  { mockRoutineDao.getActiveRoutineFlow("test-uid") } returns flowOf(entity)
        coEvery { mockRoutineDao.getWorkoutDays("routine-1") }     returns listOf(dayEntity)
        coEvery { mockRoutineDao.getExercisesForDay("day-1") }     returns listOf(exEntity)

        val repo = createRepository()
        val routine = repo.getActiveRoutineFlow("test-uid").first()

        assertNotNull(routine)
        assertEquals("routine-1",   routine!!.id)
        assertEquals("MUSCLE_GAIN", routine.goal)
        assertEquals(1,             routine.workoutDays.size)
        assertEquals("Lunes",       routine.workoutDays[0].dayName)
        assertEquals(1,             routine.workoutDays[0].exercises.size)
        assertEquals("Press de banca", routine.workoutDays[0].exercises[0].name)
        assertEquals("bench press",    routine.workoutDays[0].exercises[0].nameEn)
    }

    @Test
    fun `getActiveRoutineFlow emite null cuando no hay rutina activa`() = runTest {
        every { mockRoutineDao.getActiveRoutineFlow("test-uid") } returns flowOf(null)

        val repo    = createRepository()
        val routine = repo.getActiveRoutineFlow("test-uid").first()

        assertNull(routine)
    }

    @Test
    fun `getActiveRoutineFlow mapea dias de descanso correctamente`() = runTest {
        val entity = buildRoutineEntity()
        val restDayEntity = WorkoutDayEntity("day-rest", "routine-1", "Martes", 2, 1, "")

        every  { mockRoutineDao.getActiveRoutineFlow("test-uid") } returns flowOf(entity)
        coEvery { mockRoutineDao.getWorkoutDays("routine-1") }     returns listOf(restDayEntity)
        coEvery { mockRoutineDao.getExercisesForDay("day-rest") }  returns emptyList()

        val repo = createRepository()
        val routine = repo.getActiveRoutineFlow("test-uid").first()

        assertTrue(routine!!.workoutDays[0].isRestDay)
        assertTrue(routine.workoutDays[0].exercises.isEmpty())
    }
}