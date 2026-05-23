package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // 8 pasos: 3 físicos nuevos + 5 existentes
    val totalSteps = 8

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    // ── Datos físicos recopilados durante el form ─────────────────────────
    private var gender         = ""
    private var birthDateMs    = 0L
    private var weightKg       = 0f
    private var heightCm       = 0

    // ── Errores de validación ─────────────────────────────────────────────
    private val _birthDateError = MutableStateFlow<String?>(null)
    val birthDateError: StateFlow<String?> = _birthDateError

    private val _metricsError = MutableStateFlow<String?>(null)
    val metricsError: StateFlow<String?> = _metricsError

    // ── Setters de pasos físicos ──────────────────────────────────────────

    fun setGender(value: String) {
        gender = value
        nextStep()
    }

    fun setBirthDate(millis: Long) {
        val age = calculateAge(millis)
        when {
            age < 10  -> { _birthDateError.value = "Debés tener al menos 10 años" ; return }
            age > 100 -> { _birthDateError.value = "Ingresá una fecha de nacimiento válida" ; return }
        }
        _birthDateError.value = null
        birthDateMs = millis
        nextStep()
    }

    fun setBodyMetrics(weight: Float, height: Int): Boolean {
        val errors = mutableListOf<String>()
        if (weight < 20f || weight > 300f) errors.add("Peso debe estar entre 20 y 300 kg")
        if (height < 100 || height > 250)  errors.add("Altura debe estar entre 100 y 250 cm")

        if (errors.isNotEmpty()) {
            _metricsError.value = errors.joinToString("\n")
            return false
        }
        _metricsError.value = null
        weightKg  = weight
        heightCm  = height
        nextStep()
        return true
    }

    // ── Setters de pasos de rutina (existentes) ───────────────────────────

    fun setGoal(goal: String) {
        _userProfile.value = _userProfile.value.copy(goal = goal)
        nextStep()
    }

    fun setDaysPerWeek(days: Int) {
        _userProfile.value = _userProfile.value.copy(daysPerWeek = days)
        nextStep()
    }

    fun setSessionDuration(minutes: Int) {
        _userProfile.value = _userProfile.value.copy(sessionDuration = minutes)
        nextStep()
    }

    fun setLevel(level: String) {
        _userProfile.value = _userProfile.value.copy(level = level)
        nextStep()
    }

    fun setLimitations(limitations: String) {
        _userProfile.value = _userProfile.value.copy(limitations = limitations)
        // Al completar el último paso, guardamos el perfil físico en Room
        savePhysicalProfile()
        nextStep()
    }

    // ── Guardar perfil físico en Room + Firestore ─────────────────────────
    // Se llama al finalizar el form. El Gemini call tarda varios segundos
    // así que Room va a tener los datos mucho antes de que LoadingViewModel los lea.
    private fun savePhysicalProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        viewModelScope.launch {
            val age = calculateAge(birthDateMs)

            userProfileDao.saveProfile(
                UserProfileEntity(
                    uid            = uid,
                    age            = age,
                    weightKg       = weightKg,
                    heightCm       = heightCm,
                    gender         = gender,
                    birthDateMillis = birthDateMs
                )
            )

            // Sync a Firestore en background — si falla, Room tiene los datos
            launch {
                try {
                    firestore.collection("users")
                        .document(uid)
                        .set(
                            mapOf(
                                "age"            to age,
                                "weightKg"       to weightKg,
                                "heightCm"       to heightCm,
                                "gender"         to gender,
                                "birthDateMillis" to birthDateMs
                            ),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    android.util.Log.w("GYM_PROFILE", "Sync físico falló: ${e.message}")
                }
            }
        }
    }

    // ── Navegación ────────────────────────────────────────────────────────

    private fun nextStep() {
        _currentStep.value = _currentStep.value + 1
    }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.value -= 1
    }

    // ── Cálculo de edad desde fecha de nacimiento ─────────────────────────
    private fun calculateAge(birthDateMillis: Long): Int {
        if (birthDateMillis <= 0L) return 0
        val birthDate = Instant.ofEpochMilli(birthDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return Period.between(birthDate, LocalDate.now()).years
    }
}