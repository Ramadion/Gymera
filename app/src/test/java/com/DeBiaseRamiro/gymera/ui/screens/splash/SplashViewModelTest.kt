package com.DeBiaseRamiro.gymera.ui.screens.splash

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.data.repository.RoutineResolver
import com.DeBiaseRamiro.gymera.domain.model.*
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockRoutineResolver       = mockk<RoutineResolver>()
    private val mockRoutineRepository     = mockk<RoutineRepository>()
    private val mockFirebaseAuth          = mockk<FirebaseAuth>()
    private val mockFirebaseUser          = mockk<FirebaseUser>()
    private val mockExerciseImageRepository = mockk<ExerciseImageRepository>()

    @Before
    fun setUp() {
        // android.util.Log no existe en JVM — mockkeamos estáticamente para evitar crash
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any<Throwable>()) } returns 0

        // Warm-up de ejercicios en init del SplashViewModel — mock por defecto.
        coEvery { mockExerciseImageRepository.getAllExercises() } returns emptyList()
        coEvery { mockExerciseImageRepository.getMuscleGroups() } returns emptyList()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildTestRoutine() = Routine(
        id          = UUID.randomUUID().toString(),
        goal        = "MUSCLE_GAIN",
        level       = "INTERMEDIATE",
        daysPerWeek = 4,
        workoutDays = emptyList()
    )

    private fun createViewModel() = SplashViewModel(
        routineResolver         = mockRoutineResolver,
        routineRepository       = mockRoutineRepository,
        exerciseImageRepository = mockExerciseImageRepository,
        firebaseAuth            = mockFirebaseAuth
    )

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial es Loading`() {
        // El ViewModel empieza en Loading antes de que el init corra
        assertNotNull(SplashDestination.Loading)
    }

    @Test
    fun `sin usuario autenticado navega a Login`() = runTest {
        every { mockFirebaseAuth.currentUser } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Login, viewModel.destination.value)
    }

    @Test
    fun `con rutina activa en Room navega a Routine`() = runTest {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"
        coEvery { mockRoutineResolver.resolve("test-uid") } returns RoutineResolver.Result.Found(buildTestRoutine())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Routine, viewModel.destination.value)
    }

    @Test
    fun `sin rutina en ningun lado navega a Form`() = runTest {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"
        coEvery { mockRoutineResolver.resolve("test-uid") } returns RoutineResolver.Result.None

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Form, viewModel.destination.value)
    }

    @Test
    fun `resolver se usa para decidir la navegacion`() = runTest {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"
        coEvery { mockRoutineResolver.resolve("test-uid") } returns RoutineResolver.Result.None

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineResolver.resolve("test-uid") }
    }
}
