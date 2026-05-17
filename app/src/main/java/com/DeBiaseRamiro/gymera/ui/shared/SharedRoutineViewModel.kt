package com.DeBiaseRamiro.gymera.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedRoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // MutableStateFlow del UID — arranca con el usuario actual (puede ser null)
    // Se actualiza automáticamente cuando el usuario hace login o logout
    private val _userUid = MutableStateFlow<String?>(firebaseAuth.currentUser?.uid)

    init {
        // Escuchamos cambios de sesión de Firebase
        // Cuando el usuario hace login, _userUid se actualiza y currentRoutine
        // automáticamente empieza a observar Room con el UID correcto
        firebaseAuth.addAuthStateListener { auth ->
            _userUid.value = auth.currentUser?.uid
        }
    }

    // flatMapLatest: cada vez que _userUid cambia, cancela el Flow anterior
    // y empieza uno nuevo con el UID actualizado
    val currentRoutine: StateFlow<Routine?> = _userUid
        .flatMapLatest { uid ->
            if (uid != null) {
                routineRepository.getActiveRoutineFlow(uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _pendingUserProfile = MutableStateFlow<UserProfile?>(null)
    val pendingUserProfile: StateFlow<UserProfile?> = _pendingUserProfile

    fun setUserProfile(profile: UserProfile) { _pendingUserProfile.value = profile }
    fun clearUserProfile()                   { _pendingUserProfile.value = null }

    fun clearRoutine() {
        viewModelScope.launch {
            val uid = _userUid.value ?: return@launch
            routineRepository.deactivateActiveRoutine(uid)
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