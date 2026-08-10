package com.DeBiaseRamiro.gymera.ui.shared

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
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
class SharedRoutineViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockRoutineRepository   = mockk<RoutineRepository>()
    private val mockFirestoreRepository = mockk<FirestoreRepository>()
    private val mockFirebaseAuth        = mockk<FirebaseAuth>()
    private val mockFirebaseUser        = mockk<FirebaseUser>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        every { mockFirebaseAuth.currentUser }            returns mockFirebaseUser
        every { mockFirebaseUser.uid }                    returns "test-uid"
        every { mockFirebaseAuth.addAuthStateListener(any()) } just Runs
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildTestRoutine(id: String = UUID.randomUUID().toString()) = Routine(
        id          = id,
        goal        = "MUSCLE_GAIN",
        level       = "INTERMEDIATE",
        daysPerWeek = 4,
        workoutDays = emptyList()
    )

    private fun createViewModel(): SharedRoutineViewModel {
        return SharedRoutineViewModel(
            routineRepository   = mockRoutineRepository,
            firestoreRepository = mockFirestoreRepository,
            firebaseAuth        = mockFirebaseAuth
        )
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `currentRoutine es null cuando no hay usuario autenticado`() = runTest {
        every { mockFirebaseAuth.currentUser } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.currentRoutine.value)
    }

    @Test
    fun `currentRoutine emite la rutina que devuelve Room`() = runTest {
        val testRoutine = buildTestRoutine()
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(testRoutine)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(testRoutine, viewModel.currentRoutine.value)
    }

    @Test
    fun `currentRoutine es null cuando Room devuelve null`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.currentRoutine.value)
    }

    @Test
    fun `setRoutine actualiza currentRoutine inmediatamente sin esperar Room`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)

        val viewModel    = createViewModel()
        val testRoutine  = buildTestRoutine()

        viewModel.setRoutine(testRoutine)

        // La actualización es síncrona — no necesita advanceUntilIdle
        assertEquals(testRoutine, viewModel.currentRoutine.value)
    }

    @Test
    fun `setRoutine sobreescribe el valor previo de Room`() = runTest {
        val roomRoutine = buildTestRoutine("room-id")
        val newRoutine  = buildTestRoutine("new-id")
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(roomRoutine)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(roomRoutine, viewModel.currentRoutine.value)

        viewModel.setRoutine(newRoutine)

        assertEquals(newRoutine, viewModel.currentRoutine.value)
    }

    @Test
    fun `clearRoutine pone currentRoutine en null inmediatamente`() = runTest {
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }       returns flowOf(null)
        coEvery { mockRoutineRepository.deactivateActiveRoutine("test-uid") }   just Runs
        coEvery { mockFirestoreRepository.deactivateCloudRoutine("test-uid") }  just Runs

        val viewModel = createViewModel()
        viewModel.setRoutine(buildTestRoutine())
        assertNotNull(viewModel.currentRoutine.value) // tiene valor

        viewModel.clearRoutine()

        assertNull(viewModel.currentRoutine.value)
    }

    @Test
    fun `clearRoutine llama a deactivateActiveRoutine en Room`() = runTest {
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }       returns flowOf(null)
        coEvery { mockRoutineRepository.deactivateActiveRoutine("test-uid") }   just Runs
        coEvery { mockFirestoreRepository.deactivateCloudRoutine("test-uid") }  just Runs

        val viewModel = createViewModel()
        viewModel.setRoutine(buildTestRoutine())
        viewModel.clearRoutine()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.deactivateActiveRoutine("test-uid") }
    }

    @Test
    fun `clearRoutine intenta desactivar en Firestore en background`() = runTest {
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }       returns flowOf(null)
        coEvery { mockRoutineRepository.deactivateActiveRoutine("test-uid") }   just Runs
        coEvery { mockFirestoreRepository.deactivateCloudRoutine("test-uid") }  just Runs

        val viewModel = createViewModel()
        viewModel.clearRoutine()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockFirestoreRepository.deactivateCloudRoutine("test-uid") }
    }

    @Test
    fun `clearRoutine no crashea si Firestore falla`() = runTest {
        every  { mockRoutineRepository.getActiveRoutineFlow("test-uid") }       returns flowOf(null)
        coEvery { mockRoutineRepository.deactivateActiveRoutine("test-uid") }   just Runs
        coEvery { mockFirestoreRepository.deactivateCloudRoutine(any()) }       throws RuntimeException("sin red")

        val viewModel = createViewModel()
        viewModel.clearRoutine()
        advanceUntilIdle()

        // No debe crashear — Firestore es best-effort
        assertNull(viewModel.currentRoutine.value)
    }

    @Test
    fun `setUserProfile y clearUserProfile actualizan pendingUserProfile`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)

        val viewModel = createViewModel()
        val profile   = UserProfile(goal = "WEIGHT_LOSS", daysPerWeek = 3, level = "BEGINNER")

        assertNull(viewModel.pendingUserProfile.value)

        viewModel.setUserProfile(profile)
        assertEquals(profile, viewModel.pendingUserProfile.value)

        viewModel.clearUserProfile()
        assertNull(viewModel.pendingUserProfile.value)
    }

    @Test
    fun `sin usuario autenticado clearRoutine no llama a los repositorios`() = runTest {
        every { mockFirebaseAuth.currentUser } returns null

        val viewModel = createViewModel()
        viewModel.clearRoutine()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRoutineRepository.deactivateActiveRoutine(any()) }
        coVerify(exactly = 0) { mockFirestoreRepository.deactivateCloudRoutine(any()) }
    }

    // ── setWorkoutDayRest / clearExercisesFromDay ─────────────────────────

    @Test
    fun `setWorkoutDayRest llama al repositorio con la bandera correcta`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)
        coEvery { mockRoutineRepository.setWorkoutDayRest("day-1", true) } just Runs

        val viewModel = createViewModel()
        viewModel.setWorkoutDayRest("day-1", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.setWorkoutDayRest("day-1", true) }
    }

    @Test
    fun `clearExercisesFromDay llama al repositorio`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)
        coEvery { mockRoutineRepository.clearExercisesFromDay("day-1") } just Runs

        val viewModel = createViewModel()
        viewModel.clearExercisesFromDay("day-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.clearExercisesFromDay("day-1") }
    }

    @Test
    fun `moveExercisesToRestDay llama al repositorio`() = runTest {
        every { mockRoutineRepository.getActiveRoutineFlow("test-uid") } returns flowOf(null)
        coEvery { mockRoutineRepository.moveExercisesToRestDay("day-1", "day-rest") } just Runs

        val viewModel = createViewModel()
        viewModel.moveExercisesToRestDay("day-1", "day-rest")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockRoutineRepository.moveExercisesToRestDay("day-1", "day-rest")
        }
    }
}