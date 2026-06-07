package com.DeBiaseRamiro.gymera.ui.screens.form

import android.util.Log
import com.DeBiaseRamiro.gymera.MainCoroutineRule
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockUserProfileDao = mockk<UserProfileDao>()
    private val mockFirebaseAuth   = mockk<FirebaseAuth>()
    private val mockFirebaseUser   = mockk<FirebaseUser>()
    // Firestore se mockea relaxed para no necesitar configurar cada llamada encadenada
    private val mockFirestore      = mockk<FirebaseFirestore>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid }         returns "test-uid"
        coEvery { mockUserProfileDao.saveProfile(any()) } just Runs
    }

    private fun createViewModel() = FormViewModel(
        userProfileDao = mockUserProfileDao,
        firebaseAuth   = mockFirebaseAuth,
        firestore      = mockFirestore
    )

    // ── Helpers ───────────────────────────────────────────────────────────

    // Genera un timestamp de una persona de 25 años
    private fun birthDateOf(years: Int): Long {
        val date = LocalDate.now().minusYears(years.toLong())
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene 8 pasos y paso 0`() {
        val viewModel = createViewModel()
        assertEquals(8, viewModel.totalSteps)
        assertEquals(0, viewModel.currentStep.value)
    }

    @Test
    fun `setGender avanza al paso 1`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        assertEquals(1, viewModel.currentStep.value)
    }

    @Test
    fun `setBirthDate valida avanza al paso siguiente`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Femenino")         // paso 0 → 1
        viewModel.setBirthDate(birthDateOf(25)) // paso 1 → 2

        assertEquals(2, viewModel.currentStep.value)
        assertNull(viewModel.birthDateError.value)
    }

    @Test
    fun `setBirthDate con edad menor a 10 no avanza y muestra error`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        val step = viewModel.currentStep.value

        viewModel.setBirthDate(birthDateOf(5)) // menor a 10 años

        assertEquals(step, viewModel.currentStep.value) // no avanzó
        assertNotNull(viewModel.birthDateError.value)
    }

    @Test
    fun `setBirthDate con edad mayor a 100 no avanza y muestra error`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        val step = viewModel.currentStep.value

        viewModel.setBirthDate(birthDateOf(105))

        assertEquals(step, viewModel.currentStep.value)
        assertNotNull(viewModel.birthDateError.value)
    }

    @Test
    fun `setBodyMetrics con valores validos avanza y retorna true`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        viewModel.setBirthDate(birthDateOf(25))

        val result = viewModel.setBodyMetrics(weight = 75f, height = 175)

        assertTrue(result)
        assertNull(viewModel.metricsError.value)
        assertEquals(3, viewModel.currentStep.value)
    }

    @Test
    fun `setBodyMetrics con peso invalido retorna false y muestra error`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        viewModel.setBirthDate(birthDateOf(25))
        val step = viewModel.currentStep.value

        val result = viewModel.setBodyMetrics(weight = 5f, height = 175) // peso < 20

        assertFalse(result)
        assertNotNull(viewModel.metricsError.value)
        assertEquals(step, viewModel.currentStep.value)
    }

    @Test
    fun `setBodyMetrics con altura invalida retorna false`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        viewModel.setBirthDate(birthDateOf(25))

        val result = viewModel.setBodyMetrics(weight = 70f, height = 50) // altura < 100

        assertFalse(result)
        assertNotNull(viewModel.metricsError.value)
    }

    @Test
    fun `setGoal actualiza goal en userProfile y avanza`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGoal("MUSCLE_GAIN")

        assertEquals("MUSCLE_GAIN", viewModel.userProfile.value.goal)
        assertEquals(1, viewModel.currentStep.value)
    }

    @Test
    fun `setDaysPerWeek actualiza daysPerWeek y avanza`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGoal("WEIGHT_LOSS")
        viewModel.setDaysPerWeek(4)

        assertEquals(4, viewModel.userProfile.value.daysPerWeek)
        assertEquals(2, viewModel.currentStep.value)
    }

    @Test
    fun `setLevel actualiza level y avanza`() = runTest {
        val viewModel = createViewModel()
        viewModel.setLevel("BEGINNER")

        assertEquals("BEGINNER", viewModel.userProfile.value.level)
    }

    @Test
    fun `setLimitations avanza al ultimo paso y guarda perfil en Room`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino")
        viewModel.setBirthDate(birthDateOf(25))
        viewModel.setBodyMetrics(70f, 175)
        viewModel.setGoal("MUSCLE_GAIN")
        viewModel.setDaysPerWeek(4)
        viewModel.setSessionDuration(60)
        viewModel.setLevel("INTERMEDIATE")
        viewModel.setLimitations("Ninguna")

        advanceUntilIdle()

        // Verifica que se guardó el perfil en Room
        coVerify(exactly = 1) { mockUserProfileDao.saveProfile(any<UserProfileEntity>()) }
        assertEquals(8, viewModel.currentStep.value)
    }

    @Test
    fun `previousStep decrementa el paso`() = runTest {
        val viewModel = createViewModel()
        viewModel.setGender("Masculino") // paso 0 → 1
        viewModel.previousStep()         // paso 1 → 0

        assertEquals(0, viewModel.currentStep.value)
    }

    @Test
    fun `previousStep no va por debajo de 0`() = runTest {
        val viewModel = createViewModel()
        assertEquals(0, viewModel.currentStep.value)

        viewModel.previousStep() // ya está en 0

        assertEquals(0, viewModel.currentStep.value)
    }

    @Test
    fun `sin usuario autenticado setLimitations no crashea`() = runTest {
        every { mockFirebaseAuth.currentUser } returns null

        val viewModel = createViewModel()
        viewModel.setLimitations("Rodilla izquierda")

        advanceUntilIdle()
        // No debe lanzar excepción — el perfil simplemente no se guarda
        coVerify(exactly = 0) { mockUserProfileDao.saveProfile(any()) }
    }
}