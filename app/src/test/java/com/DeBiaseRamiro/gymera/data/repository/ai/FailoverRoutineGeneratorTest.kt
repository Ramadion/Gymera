package com.DeBiaseRamiro.gymera.data.repository.ai

import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class FailoverRoutineGeneratorTest {

    private val validJson = """
        {"workoutDays":[{"dayName":"Lunes","dayOrder":1,"isRestDay":false,"muscleFocus":"Pecho","exercises":[]}]}
    """.trimIndent()

    private fun parser(json: String): Routine = Routine(
        id = UUID.randomUUID().toString(),
        goal = "MUSCLE_GAIN",
        level = "INTERMEDIATE",
        daysPerWeek = 1,
        workoutDays = emptyList()
    )

    @Test
    fun `usa el primer proveedor si funciona`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        val p2 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        every { p2.name } returns "provider-2"

        coEvery { p1.generate(any()) } returns validJson
        coEvery { p2.generate(any()) } returns validJson

        val generator = FailoverRoutineGenerator(listOf(p1, p2))
        val result = generator.generate("prompt", ::parser)

        assertNotNull(result)
        coVerify(exactly = 1) { p1.generate(any()) }
        coVerify(exactly = 0) { p2.generate(any()) }
    }

    @Test
    fun `salta al segundo proveedor si el primero falla`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        val p2 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        every { p2.name } returns "provider-2"

        coEvery { p1.generate(any()) } throws RuntimeException("rate limit 429")
        coEvery { p2.generate(any()) } returns validJson

        val generator = FailoverRoutineGenerator(listOf(p1, p2))
        val result = generator.generate("prompt", ::parser)

        assertNotNull(result)
        coVerify { p2.generate(any()) }
    }

    @Test
    fun `salta al siguiente si el JSON del primer proveedor no parsea`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        val p2 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        every { p2.name } returns "provider-2"

        coEvery { p1.generate(any()) } returns "esto no es JSON"
        coEvery { p2.generate(any()) } returns validJson

        val generator = FailoverRoutineGenerator(listOf(p1, p2))
        val result = generator.generate("prompt") { json ->
            if (json.startsWith("{")) parser(json)
            else throw IllegalArgumentException("JSON inválido")
        }

        assertNotNull(result)
        coVerify { p2.generate(any()) }
    }

    @Test
    fun `salta al siguiente si el parser falla (no hay workoutDays)`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        val p2 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        every { p2.name } returns "provider-2"

        coEvery { p1.generate(any()) } returns """{"mensaje": "sin days"}"""
        coEvery { p2.generate(any()) } returns validJson

        val generator = FailoverRoutineGenerator(listOf(p1, p2))
        val result = generator.generate("prompt") { json ->
            if (!json.contains("workoutDays")) throw IllegalArgumentException("sin workoutDays")
            parser(json)
        }

        assertNotNull(result)
        coVerify { p2.generate(any()) }
    }

    @Test
    fun `lanza excepcion si todos los proveedores fallan`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        val p2 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        every { p2.name } returns "provider-2"

        coEvery { p1.generate(any()) } throws RuntimeException("rate limit 429")
        coEvery { p2.generate(any()) } throws RuntimeException("timeout")

        val generator = FailoverRoutineGenerator(listOf(p1, p2))

        var threw = false
        try {
            generator.generate("prompt", ::parser)
        } catch (e: Exception) {
            threw = true
        }
        assertTrue("Se esperaba excepción cuando todos fallan", threw)
    }

    @Test
    fun `la excepcion final menciona reintentar`() = runTest {
        val p1 = mockk<RoutineAIProvider>()
        every { p1.name } returns "provider-1"
        coEvery { p1.generate(any()) } throws RuntimeException("rate limit 429")

        val generator = FailoverRoutineGenerator(listOf(p1))

        var message = ""
        try {
            generator.generate("prompt", ::parser)
        } catch (e: Exception) {
            message = e.message ?: ""
        }
        assertTrue(message.contains("Intentalo"))
    }

    @Test
    fun `una lista vacia de proveedores lanza excepcion`() = runTest {
        val generator = FailoverRoutineGenerator(emptyList())

        var threw = false
        try {
            generator.generate("prompt", ::parser)
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
    }
}