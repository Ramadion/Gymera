package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.lifecycle.ViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor() : ViewModel() {

    // El paso actual del formulario (0 a 4)
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep

    // El perfil que se va armando con cada respuesta
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    // Total de pasos del formulario
    val totalSteps = 5

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
        nextStep()
    }

    private fun nextStep() {
        _currentStep.value = _currentStep.value + 1
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value = _currentStep.value - 1
        }
    }
}