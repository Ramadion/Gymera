package com.DeBiaseRamiro.gymera.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import com.DeBiaseRamiro.gymera.data.repository.buildUserProfileEntity
import com.DeBiaseRamiro.gymera.data.repository.calculateAge
import com.DeBiaseRamiro.gymera.data.repository.syncProfileToFirestore
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val uid = firebaseAuth.currentUser?.uid ?: ""

    // Cierre de sesión centralizado — desloguea Firebase Y desvincula Google,
    // así el próximo login muestra el selector de cuentas.
    fun signOut() = authRepository.signOut()

    val physicalProfile: StateFlow<UserProfileEntity?> = userProfileDao
        .getProfileFlow(uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // Recibe birthDateMillis en lugar de age directo
    fun savePhysicalProfile(
        birthDateMillis: Long,
        weightKg: Float,
        heightCm: Int,
        gender: String
    ) {
        if (uid.isEmpty()) return

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            // Calculamos la edad desde la fecha de nacimiento
            val age = calculateAge(birthDateMillis)

            userProfileDao.saveProfile(
                buildUserProfileEntity(
                    uid             = uid,
                    birthDateMillis = birthDateMillis,
                    weightKg        = weightKg,
                    heightCm        = heightCm,
                    gender          = gender
                )
            )

            launch {
                syncProfileToFirestore(
                    firestore       = firestore,
                    uid             = uid,
                    age             = age,
                    weightKg        = weightKg,
                    heightCm        = heightCm,
                    gender          = gender,
                    birthDateMillis = birthDateMillis
                )
            }

            _saveState.value = SaveState.Saved
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }
}

sealed class SaveState {
    object Idle   : SaveState()
    object Saving : SaveState()
    object Saved  : SaveState()
}