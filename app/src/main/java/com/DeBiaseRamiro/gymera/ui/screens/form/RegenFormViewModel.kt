package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RegenFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Precargamos objetivo/días/nivel de la rutina actual (se pasan por nav args
    // antes de generar). Tiempo y limitaciones se re-piden cada vez a propósito:
    // pueden cambiar y solo importan al momento de crear la rutina.

    private val initialGoal  = savedStateHandle.get<String>("goal")  ?: ""
    private val initialDays  = savedStateHandle.get<Int>("days")     ?: 0
    private val initialLevel = savedStateHandle.get<String>("level") ?: ""

    private val _goal = MutableStateFlow(initialGoal)
    val goal: StateFlow<String> = _goal

    private val _daysPerWeek = MutableStateFlow(initialDays)
    val daysPerWeek: StateFlow<Int> = _daysPerWeek

    private val _sessionDuration = MutableStateFlow(60)
    val sessionDuration: StateFlow<Int> = _sessionDuration

    private val _level = MutableStateFlow(initialLevel)
    val level: StateFlow<String> = _level

    private val _limitations = MutableStateFlow("")
    val limitations: StateFlow<String> = _limitations

    private val _canGenerate = MutableStateFlow(false)
    val canGenerate: StateFlow<Boolean> = _canGenerate

    init { recomputeCanGenerate() }

    fun setGoal(value: String) {
        _goal.value = value
        recomputeCanGenerate()
    }

    fun setDaysPerWeek(value: Int) {
        _daysPerWeek.value = value
        recomputeCanGenerate()
    }

    fun setSessionDuration(value: Int) {
        _sessionDuration.value = value
        recomputeCanGenerate()
    }

    fun setLevel(value: String) {
        _level.value = value
        recomputeCanGenerate()
    }

    fun setLimitations(value: String) {
        _limitations.value = value
    }

    fun buildProfile(): UserProfile = UserProfile(
        goal            = _goal.value,
        daysPerWeek     = _daysPerWeek.value,
        sessionDuration = _sessionDuration.value,
        level           = _level.value,
        limitations     = _limitations.value.trim()
    )

    private fun recomputeCanGenerate() {
        _canGenerate.value = _goal.value.isNotBlank() &&
                _daysPerWeek.value > 0 &&
                _sessionDuration.value > 0 &&
                _level.value.isNotBlank()
    }
}