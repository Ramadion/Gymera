package com.DeBiaseRamiro.gymera.ui.screens.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.repository.ExerciseImageRepository
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserPhysicalProfile
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoadingUiState {
    object Loading : LoadingUiState()
    data class Success(val routine: Routine) : LoadingUiState()
    data class Error(val message: String) : LoadingUiState()
}

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository,
    private val exerciseImageRepository: ExerciseImageRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingUiState>(LoadingUiState.Loading)
    val uiState: StateFlow<LoadingUiState> = _uiState

    fun generateRoutine(userProfile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = LoadingUiState.Loading
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")

                // 0. Recuperamos el perfil físico guardado en Room (peso, altura,
                //    edad, género) para que Gemini lo tenga en cuenta. Se lee solo
                //    al inicio del login; después se edita desde ProfileScreen.
                val physicalProfile = userProfileDao.getProfile(uid)?.let { entity ->
                    UserPhysicalProfile(
                        uid         = entity.uid,
                        age         = entity.age,
                        weightKg    = entity.weightKg,
                        heightCm    = entity.heightCm,
                        gender      = entity.gender,
                        birthDateMillis = entity.birthDateMillis
                    )
                }

                // 1. Generamos via Gemini
                val rawRoutine = routineRepository.generateRoutine(userProfile, physicalProfile)

                // 1b. Filtramos ejercicios que no matcheen con el asset.
                //     Esto asegura que el conteo guardado en Room sea correcto
                //     desde el inicio y el usuario no vea ejercicios desaparecer
                //     al abrir un día.
                val routine = rawRoutine.copy(
                    workoutDays = rawRoutine.workoutDays.map { day ->
                        day.copy(
                            exercises = day.exercises.filter { ex ->
                                exerciseImageRepository.getExerciseDetail(ex.nameEn) != null
                            }
                        )
                    }
                )

                // 2. Guardamos en Room (fuente de verdad local)
                routineRepository.saveRoutine(routine, uid)

                // 3. Sincronizamos con Firestore en background
                // launch separado para que si falla Firestore no afecte al usuario
                launch {
                    try {
                        android.util.Log.d("GYM_FIRESTORE", "Iniciando sync para uid: $uid")
                        firestoreRepository.syncRoutineToCloud(routine, uid)
                        android.util.Log.d("GYM_FIRESTORE", "Sync exitoso")
                    } catch (e: Exception) {
                        // Solo logueamos — Room ya tiene la rutina, el usuario no nota nada
                        android.util.Log.w("GYM_FIRESTORE", "Sync falló, se reintentará: ${e.message}")
                        android.util.Log.e("GYM_FIRESTORE", "Sync falló: ${e.message}", e)
                    }
                }

                // 4. Notificamos éxito — no esperamos a que termine Firestore
                _uiState.value = LoadingUiState.Success(routine)

            } catch (e: Exception) {
                _uiState.value = LoadingUiState.Error(
                    e.message ?: "Error al generar la rutina"
                )
            }
        }
    }
}