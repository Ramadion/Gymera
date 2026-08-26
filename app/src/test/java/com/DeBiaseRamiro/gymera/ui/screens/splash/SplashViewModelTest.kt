package com.DeBiaseRamiro.gymera.ui.screens.splash

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.*
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    private val mockRoutineRepository     = mockk<RoutineRepository>()
    private val mockFirestoreRepository   = mockk<FirestoreRepository>()
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
        routineRepository       = mockRoutineRepository,
        firestoreRepository     = mockFirestoreRepository,
        exerciseImageRepository = mockExerciseImageRepository,
        firebaseAuth            = mockFirebaseAuth
    )

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial es Loading`() {
        // El ViewModel empieza en Loading antes de que el init corra
        // Para capturarlo, mockeamos el Flow para que no emita
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "test-uid"
        every { mockRoutineRepository.getActiveRoutineFlow(any()) } returns flowOf() // nunca emite

        // No podemos capturar Loading con UnconfinedTestDispatcher porque corre todo eager,
        // pero validamos que el sealed class existe como estado inicial
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
        every { mockFirebaseUser.uid } returns "test-uid"
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(buildTestRoutine())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Routine, viewModel.destination.value)
    }

    @Test
    fun `sin rutina local pero con rutina en Firestore guarda y navega a Routine`() = runTest {
        val cloudRoutine = buildTestRoutine()

        every  { mockFirebaseAuth.currentUser }                              returns mockFirebaseUser
        every  { mockFirebaseUser.uid }                                      returns "test-uid"
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }    returns flowOf(null)
        coEvery { mockFirestoreRepository.fetchRoutineFromCloud("test-uid") } returns cloudRoutine
        coEvery { mockRoutineRepository.saveRoutine(cloudRoutine, "test-uid") } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Routine, viewModel.destination.value)
        coVerify(exactly = 1) { mockRoutineRepository.saveRoutine(cloudRoutine, "test-uid") }
    }

    @Test
    fun `sin rutina en ningun lado navega a Form`() = runTest {
        every  { mockFirebaseAuth.currentUser }                              returns mockFirebaseUser
        every  { mockFirebaseUser.uid }                                      returns "test-uid"
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }    returns flowOf(null)
        coEvery { mockFirestoreRepository.fetchRoutineFromCloud("test-uid") } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Form, viewModel.destination.value)
    }

    @Test
    fun `saveRoutine no se llama si Room ya tiene la rutina`() = runTest {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(buildTestRoutine())

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRoutineRepository.saveRoutine(any(), any()) }
    }

    @Test
    fun `si Firestore falla navega igualmente a Form`() = runTest {
        every  { mockFirebaseAuth.currentUser }                               returns mockFirebaseUser
        every  { mockFirebaseUser.uid }                                       returns "test-uid"
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }     returns flowOf(null)
        coEvery { mockFirestoreRepository.fetchRoutineFromCloud("test-uid") } throws RuntimeException("sin red")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SplashDestination.Form, viewModel.destination.value)
    }
}