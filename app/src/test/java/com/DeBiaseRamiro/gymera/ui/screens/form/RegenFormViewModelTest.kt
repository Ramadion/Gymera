package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegenFormViewModelTest {

    private fun createViewModel(
        goal: String? = null,
        days: Int? = null,
        level: String? = null
    ) = RegenFormViewModel(
        SavedStateHandle(
            buildMap {
                if (goal != null) put("goal", goal)
                if (days != null) put("days", days)
                if (level != null) put("level", level)
            }
        )
    )

    @Test
    fun `precarga objetivo dias y nivel de la rutina actual`() {
        val viewModel = createViewModel(
            goal  = "MUSCLE_GAIN",
            days  = 4,
            level = "INTERMEDIATE"
        )

        assertEquals("MUSCLE_GAIN", viewModel.goal.value)
        assertEquals(4, viewModel.daysPerWeek.value)
        assertEquals("INTERMEDIATE", viewModel.level.value)
        // El tiempo por sesión se re-pide cada vez: arranca en 60
        assertEquals(60, viewModel.sessionDuration.value)
        assertTrue(viewModel.canGenerate.value)
    }

    @Test
    fun `sin rutina previa queda incompleto y no puede generar`() {
        val viewModel = createViewModel()

        assertEquals("", viewModel.goal.value)
        assertEquals(0, viewModel.daysPerWeek.value)
        assertEquals("", viewModel.level.value)
        assertFalse(viewModel.canGenerate.value)
    }

    @Test
    fun `elegir todos los campos habilita generar y arma el perfil`() {
        val viewModel = createViewModel()

        viewModel.setGoal("WEIGHT_LOSS")
        viewModel.setDaysPerWeek(3)
        viewModel.setSessionDuration(45)
        viewModel.setLevel("BEGINNER")
        viewModel.setLimitations("Rodilla izquierda")

        assertTrue(viewModel.canGenerate.value)

        val profile = viewModel.buildProfile()
        assertEquals("WEIGHT_LOSS", profile.goal)
        assertEquals(3, profile.daysPerWeek)
        assertEquals(45, profile.sessionDuration)
        assertEquals("BEGINNER", profile.level)
        assertEquals("Rodilla izquierda", profile.limitations)
    }

    @Test
    fun `sin nivel seleccionado no puede generar`() {
        val viewModel = createViewModel(
            goal = "TONING",
            days = 5
        )

        viewModel.setGoal("TONING")
        viewModel.setDaysPerWeek(5)
        // no tocamos el nivel

        assertFalse(viewModel.canGenerate.value)
    }

    @Test
    fun `cambiar el tiempo por sesion se refleja en buildProfile`() {
        val viewModel = createViewModel()

        viewModel.setSessionDuration(90)

        assertEquals(90, viewModel.sessionDuration.value)
        assertEquals(90, viewModel.buildProfile().sessionDuration)
    }

    @Test
    fun `limitaciones opcional y se recorta el texto`() {
        val viewModel = createViewModel(
            goal  = "MUSCLE_GAIN",
            days  = 4,
            level = "ADVANCED"
        )

        viewModel.setLimitations("  sin molestias  ")

        assertEquals("sin molestias", viewModel.buildProfile().limitations)
    }

    @Test
    fun `cambiar el objetivo precargado actualiza el perfil`() {
        val viewModel = createViewModel(
            goal  = "MUSCLE_GAIN",
            days  = 4,
            level = "INTERMEDIATE"
        )

        viewModel.setGoal("ENDURANCE")

        assertEquals("ENDURANCE", viewModel.buildProfile().goal)
        // El resto de la rutina previa se mantiene
        assertEquals(4, viewModel.buildProfile().daysPerWeek)
        assertEquals("INTERMEDIATE", viewModel.buildProfile().level)
    }
}