package com.DeBiaseRamiro.gymera.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val uid = firebaseAuth.currentUser?.uid ?: ""

    // Flow de Room — la UI se actualiza automáticamente cuando cambian los datos
    val physicalProfile: StateFlow<UserProfileEntity?> = userProfileDao
        .getProfileFlow(uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Estado de guardado para mostrar feedback al usuario
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun savePhysicalProfile(
        age: Int,
        weightKg: Float,
        heightCm: Int,
        gender: String
    ) {
        if (uid.isEmpty()) return

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val entity = UserProfileEntity(
                uid       = uid,
                age       = age,
                weightKg  = weightKg,
                heightCm  = heightCm,
                gender    = gender
            )

            // 1. Guardamos en Room — instantáneo, sin red
            userProfileDao.saveProfile(entity)

            // 2. Sincronizamos a Firestore en background
            launch {
                try {
                    firestore.collection("users")
                        .document(uid)
                        .set(
                            mapOf(
                                "age"      to age,
                                "weightKg" to weightKg,
                                "heightCm" to heightCm,
                                "gender"   to gender
                            ),
                            SetOptions.merge() // no pisa otros campos del usuario
                        )
                        .await()
                } catch (e: Exception) {
                    android.util.Log.w("GYM_PROFILE", "Sync perfil falló: ${e.message}")
                    // Room ya tiene los datos — el usuario no nota nada
                }
            }

            _saveState.value = SaveState.Saved
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

sealed class SaveState {
    object Idle   : SaveState()
    object Saving : SaveState()
    object Saved  : SaveState()
}