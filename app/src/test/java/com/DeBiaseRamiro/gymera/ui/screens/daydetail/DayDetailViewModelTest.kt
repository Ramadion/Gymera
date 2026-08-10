package com.DeBiaseRamiro.gymera.ui.screens.daydetail

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DayDetailViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockExerciseImageRepository = mockk<ExerciseImageRepository>()
    private val mockRoutineRepository       = mockk<RoutineRepository>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any<Throwable>()) } returns 0

        // Por defecto las imágenes devuelven Error para simplificar los tests
        coEvery { mockExerciseImageRepository.getImageUrl(any()) } returns null
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildExercise(id: String = UUID.randomUUID().toString(), name: String = "Press de banca") =
        Exercise(id = id, name = name, nameEn = "bench press", muscleGroup = "Pecho", sets = 3, reps = "10", restSeconds = 60, notes = "")

    private fun buildWorkoutDay(exercises: List<Exercise> = emptyList()) = WorkoutDay(
        id = "day-1", dayName = "Lunes", dayOrder = 1,
        isRestDay = false, muscleFocus = "Pecho", exercises = exercises
    )

    private fun buildDto(id: String = "bench-press", name: String = "Bench Press", muscles: List<String> = listOf("chest")) =
        FreeExerciseDto(id = id, name = name, primaryMuscles = muscles, images = listOf("bench_press/0.jpg"))

    private fun createViewModel() = DayDetailViewModel(
        exerciseImageRepository = mockExerciseImageRepository,
        routineRepository       = mockRoutineRepository
    )

    // ── initializeDay ─────────────────────────────────────────────────────

    @Test
    fun `initializeDay carga los ejercicios del dia en la lista local`() = runTest {
        val exercises = listOf(buildExercise("ex-1"), buildExercise("ex-2"))
        val workoutDay = buildWorkoutDay(exercises)

        val viewModel = createViewModel()
        viewModel.initializeDay(workoutDay)
        advanceUntilIdle()

        assertEquals(exercises, viewModel.exercises.value)
    }

    @Test
    fun `initializeDay no reinicializa si se llama dos veces con el mismo dayId`() = runTest {
        val original  = listOf(buildExercise("ex-1"))
        val workoutDay = buildWorkoutDay(original)

        val viewModel = createViewModel()
        viewModel.initializeDay(workoutDay)
        advanceUntilIdle()

        // Simulamos que el usuario agrega un ejercicio localmente
        coEvery { mockRoutineRepository.addExercise(any(), any(), any()) } just Runs
        viewModel.addExercise("squat", "Sentadilla", "Piernas", 3, "10", 60)

        val afterAdd = viewModel.exercises.value.size

        // Segunda llamada a initializeDay con el mismo ID — no debe resetear la lista
        viewModel.initializeDay(workoutDay)

        assertEquals(afterAdd, viewModel.exercises.value.size)
    }

    @Test
    fun `initializeDay reinicializa si el dayId es diferente`() = runTest {
        val day1 = buildWorkoutDay(listOf(buildExercise("ex-1"))).copy(id = "day-1")
        val day2 = WorkoutDay("day-2", "Martes", 2, false, "Espalda", listOf(buildExercise("ex-2"), buildExercise("ex-3")))

        val viewModel = createViewModel()
        viewModel.initializeDay(day1)
        advanceUntilIdle()
        assertEquals(1, viewModel.exercises.value.size)

        viewModel.initializeDay(day2)
        advanceUntilIdle()
        assertEquals(2, viewModel.exercises.value.size)
    }

    // ── moveExercise ──────────────────────────────────────────────────────

    @Test
    fun `moveExercise reordena la lista correctamente`() = runTest {
        val ex1 = buildExercise("ex-1", "Ejercicio 1")
        val ex2 = buildExercise("ex-2", "Ejercicio 2")
        val ex3 = buildExercise("ex-3", "Ejercicio 3")

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(listOf(ex1, ex2, ex3)))
        advanceUntilIdle()

        // Mueve ex1 (índice 0) a índice 2
        viewModel.moveExercise(0, 2)

        val result = viewModel.exercises.value
        assertEquals(ex2, result[0])
        assertEquals(ex3, result[1])
        assertEquals(ex1, result[2])
    }

    @Test
    fun `moveExercise con indices iguales no cambia el orden`() = runTest {
        val exercises = listOf(buildExercise("ex-1"), buildExercise("ex-2"))
        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(exercises))
        advanceUntilIdle()

        viewModel.moveExercise(0, 0)

        assertEquals(exercises, viewModel.exercises.value)
    }

    @Test
    fun `moveExercise con indice fuera de rango no crashea`() = runTest {
        val exercises = listOf(buildExercise("ex-1"))
        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(exercises))
        advanceUntilIdle()

        viewModel.moveExercise(0, 99) // índice inválido

        // La lista no debe cambiar y no debe crashear
        assertEquals(exercises, viewModel.exercises.value)
    }

    // ── saveOrder ─────────────────────────────────────────────────────────

    @Test
    fun `saveOrder llama a reorderExercises con la lista actual`() = runTest {
        val exercises = listOf(buildExercise("ex-1"), buildExercise("ex-2"))
        coEvery { mockRoutineRepository.reorderExercises(any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(exercises))
        advanceUntilIdle()

        viewModel.saveOrder()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.reorderExercises(exercises) }
    }

    // ── removeExercise ────────────────────────────────────────────────────

    @Test
    fun `removeExercise elimina el ejercicio de la lista local`() = runTest {
        val ex1 = buildExercise("ex-1")
        val ex2 = buildExercise("ex-2")
        coEvery { mockRoutineRepository.removeExercise(any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(listOf(ex1, ex2)))
        advanceUntilIdle()

        viewModel.removeExercise("ex-1")

        assertEquals(listOf(ex2), viewModel.exercises.value)
    }

    @Test
    fun `removeExercise llama al repositorio para persistir en Room`() = runTest {
        coEvery { mockRoutineRepository.removeExercise("ex-1") } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(listOf(buildExercise("ex-1"))))
        advanceUntilIdle()

        viewModel.removeExercise("ex-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.removeExercise("ex-1") }
    }

    @Test
    fun `removeExercise con id inexistente no modifica la lista`() = runTest {
        val exercises = listOf(buildExercise("ex-1"))
        coEvery { mockRoutineRepository.removeExercise(any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(exercises))
        advanceUntilIdle()

        viewModel.removeExercise("id-que-no-existe")

        assertEquals(exercises, viewModel.exercises.value)
    }

    // ── addExercise ───────────────────────────────────────────────────────

    @Test
    fun `addExercise agrega el ejercicio al final de la lista`() = runTest {
        val existing = buildExercise("ex-1")
        coEvery { mockRoutineRepository.addExercise(any(), any(), any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(listOf(existing)))
        advanceUntilIdle()

        viewModel.addExercise("squat", "Sentadilla", "Piernas", 4, "8-10", 90)
        advanceUntilIdle()

        assertEquals(2, viewModel.exercises.value.size)
        val added = viewModel.exercises.value.last()
        assertEquals("Sentadilla", added.name)
        assertEquals("squat", added.nameEn)
        assertEquals("Piernas", added.muscleGroup)
        assertEquals(4, added.sets)
    }

    @Test
    fun `addExercise llama a routineRepository con dayId correcto`() = runTest {
        coEvery { mockRoutineRepository.addExercise(any(), any(), any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(emptyList())) // dayId = "day-1"
        advanceUntilIdle()

        viewModel.addExercise("pull up", "Dominadas", "Espalda", 3, "max", 90)
        advanceUntilIdle()

        coVerify { mockRoutineRepository.addExercise("day-1", any(), 0) }
    }

    @Test
    fun `addExercise genera un UUID unico para el nuevo ejercicio`() = runTest {
        coEvery { mockRoutineRepository.addExercise(any(), any(), any()) } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(emptyList()))
        advanceUntilIdle()

        viewModel.addExercise("squat", "Sentadilla", "Piernas", 3, "10", 60)
        viewModel.addExercise("deadlift", "Peso muerto", "Piernas", 3, "8", 90)
        advanceUntilIdle()

        val ids = viewModel.exercises.value.map { it.id }
        assertEquals(ids.distinct().size, ids.size) // todos únicos
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────

    @Test
    fun `loadAllExercisesForSearch carga la lista desde el repositorio`() = runTest {
        val dtos = listOf(
            buildDto("bench", "Bench Press"),
            buildDto("squat", "Barbell Squat")
        )
        every { mockExerciseImageRepository.getAllExercises() } returns dtos
        every { mockExerciseImageRepository.getMuscleGroups() } returns listOf("chest", "quadriceps")

        val viewModel = createViewModel()

        // Collector activo — activa el WhileSubscribed y mantiene el Flow corriendo
        val collectJob = launch { viewModel.searchResults.collect { } }

        viewModel.loadAllExercisesForSearch()
        advanceUntilIdle()
        // Avanzamos más que el debounce de 300ms para que el query vacío pase
        advanceTimeBy(400L)
        advanceUntilIdle()

        assertEquals(dtos, viewModel.searchResults.value)

        collectJob.cancel()
    }

    @Test
    fun `onSearchQueryChanged actualiza searchQuery`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChanged("press")

        assertEquals("press", viewModel.searchQuery.value)
    }

    @Test
    fun `onMuscleSelected selecciona un musculo`() = runTest {
        val viewModel = createViewModel()
        viewModel.onMuscleSelected("chest")

        assertEquals("chest", viewModel.selectedMuscle.value)
    }

    @Test
    fun `onMuscleSelected con el mismo musculo lo deselecciona (toggle)`() = runTest {
        val viewModel = createViewModel()
        viewModel.onMuscleSelected("chest")
        viewModel.onMuscleSelected("chest") // segunda vez = deselect

        assertNull(viewModel.selectedMuscle.value)
    }

    @Test
    fun `resetSearch limpia query y musculo seleccionado`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChanged("press")
        viewModel.onMuscleSelected("chest")

        viewModel.resetSearch()

        assertEquals("", viewModel.searchQuery.value)
        assertNull(viewModel.selectedMuscle.value)
    }

    @Test
    fun `imageStates empieza vacio y se llena al inicializar el dia`() = runTest {
        val exercise = buildExercise("ex-1")
        coEvery { mockExerciseImageRepository.getImageUrl("bench press") } returns "http://example.com/img.jpg"

        val viewModel = createViewModel()
        assertTrue(viewModel.imageStates.value.isEmpty())

        viewModel.initializeDay(buildWorkoutDay(listOf(exercise)))
        advanceUntilIdle()

        assertTrue(viewModel.imageStates.value.containsKey("ex-1"))
        assertTrue(viewModel.imageStates.value["ex-1"] is ExerciseImageState.Success)
    }

    @Test
    fun `imageStates marca Error cuando la imagen no se encuentra`() = runTest {
        val exercise = buildExercise("ex-1")
        coEvery { mockExerciseImageRepository.getImageUrl(any()) } returns null

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(listOf(exercise)))
        advanceUntilIdle()

        assertEquals(ExerciseImageState.Error, viewModel.imageStates.value["ex-1"])
    }

    @Test
    fun `updateDescription llama al repositorio con el dia y texto correctos`() = runTest {
        coEvery { mockRoutineRepository.setWorkoutDayMuscleFocus("day-1", "Pecho y Triceps") } just Runs

        val viewModel = createViewModel()
        viewModel.initializeDay(buildWorkoutDay(emptyList())) // currentDayId = "day-1"
        viewModel.updateDescription("Pecho y Triceps")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockRoutineRepository.setWorkoutDayMuscleFocus("day-1", "Pecho y Triceps")
        }
    }
}