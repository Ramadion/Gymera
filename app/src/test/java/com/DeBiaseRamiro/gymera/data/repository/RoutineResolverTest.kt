package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineResolverTest {

    private val mockRoutineRepository = mockk<RoutineRepository>()
    private val mockFirestoreRepository = mockk<FirestoreRepository>()

    private fun buildRoutine() = Routine(
        id          = UUID.randomUUID().toString(),
        goal        = "MUSCLE_GAIN",
        level       = "INTERMEDIATE",
        daysPerWeek = 4,
        workoutDays = emptyList()
    )

    private fun createResolver() = RoutineResolver(mockRoutineRepository, mockFirestoreRepository)

    @Test
    fun `Room con rutina devuelve Found sin tocar Firestore`() = runTest {
        val routine = buildRoutine()
        coEvery { mockRoutineRepository.getActiveRoutine(any()) } returns routine

        val result = createResolver().resolve("uid")

        assertEquals(RoutineResolver.Result.Found(routine), result)
        coVerify(exactly = 0) { mockFirestoreRepository.fetchRoutineFromCacheFirst(any()) }
    }

    @Test
    fun `Room vacio pero Firestore cache con rutina guarda y devuelve Found`() = runTest {
        val routine = buildRoutine()
        coEvery { mockRoutineRepository.getActiveRoutine(any()) } returns null
        coEvery { mockFirestoreRepository.fetchRoutineFromCacheFirst("uid") } returns routine
        coEvery { mockRoutineRepository.saveRoutine(routine, "uid") } just Runs

        val result = createResolver().resolve("uid")

        assertEquals(RoutineResolver.Result.Found(routine), result)
        coVerify(exactly = 1) { mockRoutineRepository.saveRoutine(routine, "uid") }
    }

    @Test
    fun `Room y Firestore vacios devuelve None`() = runTest {
        coEvery { mockRoutineRepository.getActiveRoutine(any()) } returns null
        coEvery { mockFirestoreRepository.fetchRoutineFromCacheFirst("uid") } returns null

        val result = createResolver().resolve("uid")

        assertEquals(RoutineResolver.Result.None, result)
    }

    @Test
    fun `Firestore falla devuelve Unavailable y no guarda`() = runTest {
        coEvery { mockRoutineRepository.getActiveRoutine(any()) } returns null
        coEvery { mockFirestoreRepository.fetchRoutineFromCacheFirst("uid") } throws RuntimeException("sin red")

        val result = createResolver().resolve("uid")

        assertEquals(RoutineResolver.Result.Unavailable, result)
        coVerify(exactly = 0) { mockRoutineRepository.saveRoutine(any(), any()) }
    }
}
