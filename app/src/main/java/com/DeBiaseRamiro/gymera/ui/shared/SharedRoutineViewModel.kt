package com.DeBiaseRamiro.gymera.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository

@HiltViewModel
class SharedRoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Obtenemos el UID del usuario actual (puede ser null si no está logueado)
    private val userUid: String? = firebaseAuth.currentUser?.uid

    // currentRoutine es un StateFlow que emite la rutina activa desde Room.
    // stateIn() es lo que convierte el Flow normal del repositorio en StateFlow.
    // SharingStarted.WhileSubscribed(5000) mantiene el Flow activo 5s después
    // de que no haya observers, para sobrevivir rotaciones de pantalla.
    val currentRoutine: StateFlow<Routine?> = if (userUid != null) {
        routineRepository.getActiveRoutineFlow(userUid)
    } else {
        flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // UserProfile pendiente durante el flujo Form → Loading (sin cambios)
    private val _pendingUserProfile = MutableStateFlow<UserProfile?>(null)
    val pendingUserProfile: StateFlow<UserProfile?> = _pendingUserProfile

    fun setUserProfile(profile: UserProfile) { _pendingUserProfile.value = profile }
    fun clearUserProfile()                   { _pendingUserProfile.value = null }

    // En SharedRoutineViewModel.kt — reemplazá solo clearRoutine()

    fun clearRoutine() {
        viewModelScope.launch {
            val uid = userUid ?: return@launch
            // Desactivamos en Room — el Flow emite null y la UI navega sola
            routineRepository.deactivateActiveRoutine(uid)
            // Desactivamos en Firestore en background — si falla no importa
            launch {
                try {
                    firestoreRepository.deactivateCloudRoutine(uid)
                } catch (e: Exception) {
                    android.util.Log.w("GYM_FIRESTORE", "Error desactivando en cloud: ${e.message}")
                }
            }
        }
    }
}