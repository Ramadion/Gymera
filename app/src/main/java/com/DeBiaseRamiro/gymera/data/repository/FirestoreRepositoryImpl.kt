package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    // ── syncRoutineToCloud ────────────────────────────────────────────────
    // Guarda la rutina completa en Firestore bajo users/{uid}/routines/{routineId}
    // y actualiza el flag hasActiveRoutine en el documento del usuario
    override suspend fun syncRoutineToCloud(routine: Routine, userUid: String) {
        // Borramos TODAS las rutinas anteriores de Firestore
        val allPrevious = firestore
            .collection("users")
            .document(userUid)
            .collection("routines")
            .get()
            .await()

        allPrevious.documents.forEach { doc ->
            if (doc.id != routine.id) {
                doc.reference.delete().await()
            }
        }

        // Subimos la nueva rutina
        val routineMap = mapOf(
            "id"          to routine.id,
            "goal"        to routine.goal,
            "level"       to routine.level,
            "daysPerWeek" to routine.daysPerWeek,
            "generatedAt" to System.currentTimeMillis(),
            "isActive"    to true,
            "workoutDays" to routine.workoutDays.map { day ->
                mapOf(
                    "id"          to day.id,
                    "dayName"     to day.dayName,
                    "dayOrder"    to day.dayOrder,
                    "isRestDay"   to day.isRestDay,
                    "muscleFocus" to day.muscleFocus,
                    "exercises"   to day.exercises.map { ex ->
                        mapOf(
                            "id"          to ex.id,
                            "name"        to ex.name,
                            "nameEn"      to ex.nameEn,
                            "muscleGroup" to ex.muscleGroup,
                            "sets"        to ex.sets,
                            "reps"        to ex.reps,
                            "restSeconds" to ex.restSeconds,
                            "notes"       to ex.notes
                        )
                    }
                )
            }
        )

        firestore
            .collection("users")
            .document(userUid)
            .collection("routines")
            .document(routine.id)
            .set(routineMap)
            .await()

        // Actualizamos hasActiveRoutine en el documento del usuario
        firestore
            .collection("users")
            .document(userUid)
            .update("hasActiveRoutine", true)
            .await()
    }

    // ── fetchRoutineFromCloud ─────────────────────────────────────────────
    // Descarga la rutina activa desde Firestore y la reconstruye como objeto Routine
    // Se usa en el Splash cuando Room está vacío (dispositivo nuevo)
    override suspend fun fetchRoutineFromCloud(userUid: String): Routine? {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userUid)
                .collection("routines")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull() ?: return null

            // Reconstruimos el objeto Routine desde el Map de Firestore
            parseRoutineFromFirestore(doc.data ?: return null)

        } catch (e: Exception) {
            android.util.Log.e("GYM_FIRESTORE", "Error bajando rutina: ${e.message}")
            null  // Si falla la red, devolvemos null y el Splash lo maneja
        }
    }

    // ── deactivateCloudRoutine ────────────────────────────────────────────
    // Marca hasActiveRoutine = false — se llama cuando el usuario pide nueva rutina
    override suspend fun deactivateCloudRoutine(userUid: String) {
        try {
            // Marcamos todas las rutinas activas como inactivas
            val activeRoutines = firestore
                .collection("users")
                .document(userUid)
                .collection("routines")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            activeRoutines.documents.forEach { doc ->
                doc.reference.update("isActive", false).await()
            }

            // Actualizamos el flag del usuario
            firestore
                .collection("users")
                .document(userUid)
                .set(mapOf("hasActiveRoutine" to false), SetOptions.merge())
                .await()

        } catch (e: Exception) {
            android.util.Log.e("GYM_FIRESTORE", "Error desactivando rutina: ${e.message}")
            // No lanzamos la excepción — si falla Firestore, Room sigue siendo la fuente de verdad
        }
    }

    // ── Parser de Firestore → Routine ─────────────────────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun parseRoutineFromFirestore(data: Map<String, Any>): Routine? {
        return try {
            val workoutDaysRaw = data["workoutDays"] as? List<Map<String, Any>>
                ?: return null

            val workoutDays = workoutDaysRaw.map { dayMap ->
                val exercisesRaw = dayMap["exercises"] as? List<Map<String, Any>>
                    ?: emptyList()

                val exercises = exercisesRaw.map { exMap ->
                    Exercise(
                        id          = exMap["id"]          as? String ?: "",
                        name        = exMap["name"]        as? String ?: "",
                        nameEn      = exMap["nameEn"]      as? String ?: "",
                        muscleGroup = exMap["muscleGroup"] as? String ?: "",
                        sets        = (exMap["sets"]       as? Long)?.toInt() ?: 0,
                        reps        = exMap["reps"]        as? String ?: "",
                        restSeconds = (exMap["restSeconds"] as? Long)?.toInt() ?: 0,
                        notes       = exMap["notes"]       as? String ?: ""
                    )
                }

                WorkoutDay(
                    id          = dayMap["id"]          as? String ?: "",
                    dayName     = dayMap["dayName"]     as? String ?: "",
                    dayOrder    = (dayMap["dayOrder"]   as? Long)?.toInt() ?: 0,
                    isRestDay   = dayMap["isRestDay"]   as? Boolean ?: false,
                    muscleFocus = dayMap["muscleFocus"] as? String ?: "",
                    exercises   = exercises
                )
            }

            Routine(
                id          = data["id"]          as? String ?: "",
                goal        = data["goal"]        as? String ?: "",
                level       = data["level"]       as? String ?: "",
                daysPerWeek = (data["daysPerWeek"] as? Long)?.toInt() ?: 0,
                workoutDays = workoutDays
            )
        } catch (e: Exception) {
            android.util.Log.e("GYM_FIRESTORE", "Error parseando rutina: ${e.message}")
            null
        }
    }
}