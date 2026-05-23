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
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val uid = firebaseAuth.currentUser?.uid ?: ""

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

            val entity = UserProfileEntity(
                uid             = uid,
                age             = age,
                weightKg        = weightKg,
                heightCm        = heightCm,
                gender          = gender,
                birthDateMillis = birthDateMillis
            )

            userProfileDao.saveProfile(entity)

            launch {
                try {
                    firestore.collection("users")
                        .document(uid)
                        .set(
                            mapOf(
                                "age"             to age,
                                "weightKg"        to weightKg,
                                "heightCm"        to heightCm,
                                "gender"          to gender,
                                "birthDateMillis" to birthDateMillis
                            ),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    android.util.Log.w("GYM_PROFILE", "Sync perfil falló: ${e.message}")
                }
            }

            _saveState.value = SaveState.Saved
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    // Calcula la edad exacta desde la fecha de nacimiento
    fun calculateAge(birthDateMillis: Long): Int {
        if (birthDateMillis <= 0L) return 0
        val birthDate = Instant.ofEpochMilli(birthDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return Period.between(birthDate, LocalDate.now()).years
    }
}

sealed class SaveState {
    object Idle   : SaveState()
    object Saving : SaveState()
    object Saved  : SaveState()
}