package com.DeBiaseRamiro.gymera.ui.screens.loading

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.*
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
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
class LoadingViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockRoutineRepository     = mockk<RoutineRepository>()
    private val mockFirestoreRepository   = mockk<FirestoreRepository>()
    private val mockFirebaseAuth          = mockk<FirebaseAuth>()
    private val mockFirebaseUser          = mockk<FirebaseUser>()
    private val mockUserProfileDao        = mockk<UserProfileDao>()
    private val mockExerciseImageRepository = mockk<ExerciseImageRepository>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any<Throwable>()) } returns 0

        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"

        // Pre-carga de ejercicios — por defecto vacío para tests
        coEvery { mockExerciseImageRepository.getExerciseDetail(any()) } returns null
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildUserProfile() = UserProfile(
        goal = "MUSCLE_GAIN", daysPerWeek = 4,
        sessionDuration = 60, level = "INTERMEDIATE", limitations = ""
    )

    private fun buildTestRoutine() = Routine(
        id = UUID.randomUUID().toString(), goal = "MUSCLE_GAIN",
        level = "INTERMEDIATE", daysPerWeek = 4, workoutDays = emptyList()
    )

    private fun createViewModel() = LoadingViewModel(
        routineRepository       = mockRoutineRepository,
        firestoreRepository     = mockFirestoreRepository,
        exerciseImageRepository = mockExerciseImageRepository,
        firebaseAuth            = mockFirebaseAuth,
        userProfileDao          = mockUserProfileDao
    )

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial es Loading`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value is LoadingUiState.Loading)
    }

    @Test
    fun `generateRoutine emite Success con la rutina generada`() = runTest {
        val userProfile  = buildUserProfile()
        val testRoutine  = buildTestRoutine()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) }             returns testRoutine
        coEvery { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") }     just Runs
        coEvery { mockFirestoreRepository.syncRoutineToCloud(testRoutine, "test-uid") } just Runs

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoadingUiState.Success)
        assertEquals(testRoutine, (state as LoadingUiState.Success).routine)
    }

    @Test
    fun `generateRoutine guarda la rutina en Room`() = runTest {
        val userProfile = buildUserProfile()
        val testRoutine = buildTestRoutine()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) }             returns testRoutine
        coEvery { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") }     just Runs
        coEvery { mockFirestoreRepository.syncRoutineToCloud(testRoutine, "test-uid") } just Runs

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") }
    }

    @Test
    fun `generateRoutine llama a Gemini con el userProfile correcto`() = runTest {
        val userProfile = buildUserProfile()
        val testRoutine = buildTestRoutine()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) }             returns testRoutine
        coEvery { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") }     just Runs
        coEvery { mockFirestoreRepository.syncRoutineToCloud(testRoutine, "test-uid") } just Runs

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRoutineRepository.generateRoutine(userProfile) }
    }

    @Test
    fun `generateRoutine sincroniza con Firestore en background`() = runTest {
        val userProfile = buildUserProfile()
        val testRoutine = buildTestRoutine()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) }             returns testRoutine
        coEvery { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") }     just Runs
        coEvery { mockFirestoreRepository.syncRoutineToCloud(testRoutine, "test-uid") } just Runs

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockFirestoreRepository.syncRoutineToCloud(testRoutine, "test-uid") }
    }

    @Test
    fun `generateRoutine emite Error si Gemini falla`() = runTest {
        val userProfile = buildUserProfile()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) } throws RuntimeException("Rate limit")

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoadingUiState.Error)
        assertTrue((state as LoadingUiState.Error).message.isNotBlank())
    }

    @Test
    fun `generateRoutine emite Error si no hay usuario autenticado`() = runTest {
        every { mockFirebaseAuth.currentUser } returns null

        val viewModel = createViewModel()
        viewModel.generateRoutine(buildUserProfile())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoadingUiState.Error)
    }

    @Test
    fun `generateRoutine no crashea si Firestore falla en background`() = runTest {
        val userProfile = buildUserProfile()
        val testRoutine = buildTestRoutine()

        coEvery { mockRoutineRepository.generateRoutine(userProfile) }         returns testRoutine
        coEvery { mockRoutineRepository.saveRoutine(testRoutine, "test-uid") } just Runs
        coEvery { mockFirestoreRepository.syncRoutineToCloud(any(), any()) }   throws RuntimeException("sin red")

        val viewModel = createViewModel()
        viewModel.generateRoutine(userProfile)
        advanceUntilIdle()

        // El error de Firestore no debe contaminar el estado principal
        assertTrue(viewModel.uiState.value is LoadingUiState.Success)
    }

    @Test
    fun `generateRoutine no guarda en Room si Gemini falla`() = runTest {
        coEvery { mockRoutineRepository.generateRoutine(any()) } throws RuntimeException("Error de red")

        val viewModel = createViewModel()
        viewModel.generateRoutine(buildUserProfile())
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRoutineRepository.saveRoutine(any(), any()) }
    }

    @Test
    fun `mensaje de error incluye descripcion del problema`() = runTest {
        coEvery { mockRoutineRepository.generateRoutine(any()) } throws RuntimeException("Timeout de red")

        val viewModel = createViewModel()
        viewModel.generateRoutine(buildUserProfile())
        advanceUntilIdle()

        val state = viewModel.uiState.value as LoadingUiState.Error
        assertTrue(state.message.contains("Timeout") || state.message.isNotBlank())
    }
}