package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

// ── RoutineResolver ───────────────────────────────────────────────────────────
// Servicio centralizado que resuelve "¿dónde está la rutina activa del usuario?"
// al arrancar la app. Lo usan tanto SplashViewModel como LoginViewModel.
//
// El problema que resuelve: el flujo anterior usaba un timeout arbitrario
// (withTimeoutOrNull(2000L)) que confundía "la carga de Room tardó más de 2s en
// un cold start" con "no hay rutina". Eso mandaba al Form aunque la rutina
// existiera. Este resolver distingue con certeza:
//
//   Found        → hay rutina (Room o Firestore)
//   None         → CERTEZA de que no hay rutina (Room y Firestore confirmados vacíos)
//   Unavailable  → no se pudo determinar (fallo temporal); NO hay que navegar a Form
//
// Estragias:
//   - Room con reintento: un arranque frío puede tardar en abrir la DB; se reintenta
//     una vez antes de concluir. getActiveRoutine() (suspend) hace 1 query + N por día.
//   - Firestore cache-first: se lee primero el cache local de Firestore (offline,
//     instantáneo) y solo si está vacío se va a red. No depende de conexión.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class RoutineResolver @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val firestoreRepository: FirestoreRepository
) {

    sealed class Result {
        data class Found(val routine: Routine) : Result()
        object None : Result()
        object Unavailable : Result()
    }

    suspend fun resolve(userUid: String): Result {
        // ── Nivel 1: Room (local, fuente de verdad) ────────────────────────
        // Reintento: un cold start puede tardar en abrir la DB / correr la migración.
        var roomRoutine = routineRepository.getActiveRoutine(userUid)
        if (roomRoutine == null) {
            delay(ROOM_RETRY_DELAY_MS)
            roomRoutine = routineRepository.getActiveRoutine(userUid)
        }
        if (roomRoutine != null) {
            return Result.Found(roomRoutine)
        }

        // ── Nivel 2: Firestore (cache local primero, luego red) ─────────────
        // Solo llegamos acá si Room confirmó estar vacía tras el reintento.
        // fetchRoutineFromCacheFirst devuelve null solo cuando NO existe rutina en
        // ni el cache ni la red; lanza si la red no está y no hay cache → lo mapeamos
        // a Unavailable (fallo temporal) para no forzar la navegación a Form.
        return try {
            when (val cloud = firestoreRepository.fetchRoutineFromCacheFirst(userUid)) {
                null   -> Result.None                 // cache y red vacíos → certeza de que no hay
                else   -> {
                    // Persistimos localmente para disponibilidad offline futura
                    runCatching { routineRepository.saveRoutine(cloud, userUid) }
                    Result.Found(cloud)
                }
            }
        } catch (e: Exception) {
            Result.Unavailable
        }
    }

    private companion object {
        const val ROOM_RETRY_DELAY_MS = 600L
    }
}
