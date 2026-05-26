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

    private val _userUid = MutableStateFlow<String?>(firebaseAuth.currentUser?.uid)

    // ── FIX: MutableStateFlow directo en lugar de stateIn ──────────────────────
    //
    // El problema anterior: currentRoutine era un StateFlow derivado de Room via
    // flatMapLatest + stateIn. Cuando se navegaba a ROUTINE justo después de
    // saveRoutine(), Room todavía no había emitido su nuevo valor (race condition).
    // El composable veía null, iniciaba el timer de 600ms, y si Room tardaba más,
    // redirigía al Form.
    //
    // La solución: _currentRoutine es un MutableStateFlow que puede ser actualizado
    // de dos formas:
    //   1. Por setRoutine() — inmediatamente, antes de navegar (elimina la race condition)
    //   2. Por el Flow de Room — reactivamente cuando Room actualiza sus tablas
    //
    // Room sigue siendo la fuente de verdad: su emisión siempre sobreescribe.
    // setRoutine() solo sirve para dar un valor inmediato mientras Room "llega".
    // ───────────────────────────────────────────────────────────────────────────
    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine

    init {
        // Actualizamos el UID cuando el usuario hace login o logout
        firebaseAuth.addAuthStateListener { auth ->
            _userUid.value = auth.currentUser?.uid
        }

        // Observamos Room de forma reactiva.
        // flatMapLatest cancela el Flow anterior cuando el UID cambia,
        // garantizando que siempre escuchamos al usuario correcto.
        viewModelScope.launch {
            _userUid
                .flatMapLatest { uid ->
                    if (uid != null) {
                        routineRepository.getActiveRoutineFlow(uid)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { routineFromRoom ->
                    // Room es la fuente de verdad final.
                    // Siempre aplicamos lo que Room dice, ya sea una rutina nueva o null.
                    _currentRoutine.value = routineFromRoom
                }
        }
    }

    // ── setRoutine ──────────────────────────────────────────────────────────────
    // Establece la rutina de forma inmediata en el StateFlow, ANTES de que Room
    // emita. Esto elimina la race condition al navegar a RoutineScreen justo
    // después de guardar en Room.
    //
    // Uso: llamar desde NavGraph en onRoutineGenerated, ANTES de navegar.
    //
    //   sharedRoutineViewModel.setRoutine(routine)
    //   navController.navigate(Routes.ROUTINE) { ... }
    //
    // Cuando Room emita el mismo valor (milisegundos después), la actualización
    // es idempotente — el StateFlow queda con el mismo valor, sin efecto visible.
    // ───────────────────────────────────────────────────────────────────────────
    fun setRoutine(routine: Routine) {
        _currentRoutine.value = routine
    }

    // ── clearRoutine ────────────────────────────────────────────────────────────
    // Limpia la rutina inmediatamente en la UI (_currentRoutine = null) y luego
    // desactiva en Room y Firestore en background.
    //
    // La limpieza inmediata es importante para que RoutineScreen no siga
    // mostrando la rutina vieja mientras se navega al Form.
    // ───────────────────────────────────────────────────────────────────────────
    fun clearRoutine() {
        _currentRoutine.value = null  // Limpieza inmediata en la UI

        viewModelScope.launch {
            val uid = _userUid.value ?: return@launch

            // Desactivamos en Room (fuente de verdad local)
            routineRepository.deactivateActiveRoutine(uid)

            // Desactivamos en Firestore en background (best-effort)
            launch {
                try {
                    firestoreRepository.deactivateCloudRoutine(uid)
                } catch (e: Exception) {
                    android.util.Log.w("GYM_FIRESTORE", "Error desactivando en cloud: ${e.message}")
                }
            }
        }
    }

    // ── Perfil pendiente (Form → Loading) ───────────────────────────────────────
    private val _pendingUserProfile = MutableStateFlow<UserProfile?>(null)
    val pendingUserProfile: StateFlow<UserProfile?> = _pendingUserProfile

    fun setUserProfile(profile: UserProfile) { _pendingUserProfile.value = profile }
    fun clearUserProfile()                   { _pendingUserProfile.value = null }
}