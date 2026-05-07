package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.BuildConfig
import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseAssignmentEntity
import com.DeBiaseRamiro.gymera.data.local.entity.RoutineEntity
import com.DeBiaseRamiro.gymera.data.local.entity.WorkoutDayEntity
import com.DeBiaseRamiro.gymera.data.remote.api.GeminiApi
import com.DeBiaseRamiro.gymera.data.remote.dto.*
import com.DeBiaseRamiro.gymera.domain.model.*
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApi,
    private val routineDao: RoutineDao
) : RoutineRepository {

    // ── generateRoutine — sin cambios respecto a tu versión actual ────────
    override suspend fun generateRoutine(userProfile: UserProfile): Routine {
        val prompt = """
            Eres un entrenador personal experto. Genera una rutina de entrenamiento semanal
            personalizada basada en estos datos del usuario:
            - Objetivo: ${userProfile.goal}
            - Dias por semana que puede entrenar: ${userProfile.daysPerWeek}
            - Duracion por sesion: ${userProfile.sessionDuration} minutos
            - Nivel de experiencia: ${userProfile.level}
            - Lesiones o limitaciones: ${userProfile.limitations.ifEmpty { "Ninguna" }}

            REGLAS ESTRICTAS que debes seguir sin excepcion:
            1. El JSON debe tener EXACTAMENTE 7 objetos en "workoutDays", uno por cada dia
               de la semana: Lunes (dayOrder 1), Martes (2), Miercoles (3), Jueves (4),
               Viernes (5), Sabado (6), Domingo (7).
            2. Los dias de descanso tienen isRestDay=true y exercises=[].
            3. La cantidad de dias de entrenamiento debe ser EXACTAMENTE ${userProfile.daysPerWeek}.
               Los dias restantes hasta llegar a 7 deben ser descanso.
            4. Cada ejercicio DEBE tener dos nombres:
               - "name": el nombre en ESPAÑOL (para mostrar al usuario)
               - "nameEn": el nombre en INGLES exacto como aparece en bases de datos
                 de ejercicios internacionales (para busqueda interna)
            5. Responde UNICAMENTE con JSON valido. Sin texto antes ni despues.
               Sin markdown, sin bloques de codigo, sin ```.

            Formato JSON exacto que debes devolver:
            {
              "workoutDays": [
                {
                  "dayName": "Lunes",
                  "dayOrder": 1,
                  "isRestDay": false,
                  "muscleFocus": "Pecho y Triceps",
                  "exercises": [
                    {
                      "name": "Press de banca",
                      "nameEn": "barbell bench press",
                      "muscleGroup": "Pecho",
                      "sets": 4,
                      "reps": "8-12",
                      "restSeconds": 90,
                      "notes": "Mantene los codos a 45 grados"
                    }
                  ]
                },
                {
                  "dayName": "Martes",
                  "dayOrder": 2,
                  "isRestDay": true,
                  "muscleFocus": "",
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiRequestContent(parts = listOf(GeminiRequestPart(text = prompt)))
            )
        )

        val response = geminiApi.generateRoutine(
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = requestBody
        )

        val jsonText = response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text ?: throw Exception("Respuesta vacía de Gemini")

        val cleanJson = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return parseRoutineFromJson(cleanJson, userProfile)
    }

    // ── saveRoutine — guarda rutina completa en Room ──────────────────────
    override suspend fun saveRoutine(routine: Routine, userUid: String) {
        // 1. Desactivamos cualquier rutina activa anterior del usuario
        routineDao.deactivateAllRoutines(userUid)

        // 2. Insertamos la nueva rutina como activa
        routineDao.insertRoutine(
            RoutineEntity(
                id              = routine.id,
                userUid         = userUid,
                goal            = routine.goal,
                daysPerWeek     = routine.daysPerWeek,
                sessionDuration = 0,   // no está en el modelo de dominio, valor default
                level           = routine.level,
                limitations     = "",
                generatedAt     = System.currentTimeMillis(),
                isActive        = 1
            )
        )

        // 3. Insertamos los días de la semana
        val dayEntities = routine.workoutDays.map { day ->
            WorkoutDayEntity(
                id          = day.id,
                routineId   = routine.id,
                dayName     = day.dayName,
                dayOrder    = day.dayOrder,
                isRestDay   = if (day.isRestDay) 1 else 0,
                muscleFocus = day.muscleFocus
            )
        }
        routineDao.insertWorkoutDays(dayEntities)

        // 4. Insertamos todos los ejercicios de todos los días
        val exerciseEntities = routine.workoutDays.flatMap { day ->
            day.exercises.mapIndexed { index, exercise ->
                ExerciseAssignmentEntity(
                    id            = exercise.id,
                    workoutDayId  = day.id,
                    nameEs        = exercise.name,
                    nameEn        = exercise.nameEn,
                    muscleGroup   = exercise.muscleGroup,
                    sets          = exercise.sets,
                    reps          = exercise.reps,
                    restSeconds   = exercise.restSeconds,
                    orderInDay    = index,
                    notes         = exercise.notes
                )
            }
        }
        routineDao.insertExercises(exerciseEntities)

        // 5. Limpiamos rutinas viejas inactivas para no acumular basura
        routineDao.deleteInactiveRoutines(userUid)
    }

    // ── getActiveRoutineFlow — Flow para que la UI observe cambios ────────
    override fun getActiveRoutineFlow(userUid: String): Flow<Routine?> =
        routineDao.getActiveRoutineFlow(userUid).map { entity ->
            entity?.let { buildRoutineFromEntity(it) }
        }

    // ── getActiveRoutine — lectura única para el Splash ───────────────────
    override suspend fun getActiveRoutine(userUid: String): Routine? {
        val entity = routineDao.getActiveRoutine(userUid) ?: return null
        return buildRoutineFromEntity(entity)
    }

    // ── Reconstruye el objeto Routine completo desde las 3 tablas de Room ─
    private suspend fun buildRoutineFromEntity(entity: RoutineEntity): Routine {
        val dayEntities = routineDao.getWorkoutDays(entity.id)

        val workoutDays = dayEntities.map { dayEntity ->
            val exerciseEntities = routineDao.getExercisesForDay(dayEntity.id)

            val exercises = exerciseEntities.map { ex ->
                Exercise(
                    id          = ex.id,
                    name        = ex.nameEs,
                    nameEn      = ex.nameEn,
                    muscleGroup = ex.muscleGroup,
                    sets        = ex.sets,
                    reps        = ex.reps,
                    restSeconds = ex.restSeconds,
                    notes       = ex.notes
                )
            }

            WorkoutDay(
                id          = dayEntity.id,
                dayName     = dayEntity.dayName,
                dayOrder    = dayEntity.dayOrder,
                isRestDay   = dayEntity.isRestDay == 1,
                muscleFocus = dayEntity.muscleFocus,
                exercises   = exercises
            )
        }

        return Routine(
            id          = entity.id,
            goal        = entity.goal,
            level       = entity.level,
            daysPerWeek = entity.daysPerWeek,
            workoutDays = workoutDays
        )
    }

    override suspend fun deactivateActiveRoutine(userUid: String) {
        routineDao.deactivateAllRoutines(userUid)
    }

    // ── parseRoutineFromJson — sin cambios ────────────────────────────────
    private fun parseRoutineFromJson(json: String, userProfile: UserProfile): Routine {
        val gson = Gson()
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val rootMap: Map<String, Any> = gson.fromJson(json, mapType)
        val workoutDaysRaw = rootMap["workoutDays"] as? List<*> ?: emptyList<Any>()

        val workoutDays = workoutDaysRaw.mapIndexed { index, dayRaw ->
            val day = dayRaw as? Map<*, *> ?: return@mapIndexed null
            val exercisesRaw = day["exercises"] as? List<*> ?: emptyList<Any>()
            val exercises = exercisesRaw.mapIndexed { _, exRaw ->
                val ex = exRaw as? Map<*, *> ?: return@mapIndexed null
                Exercise(
                    id          = UUID.randomUUID().toString(),
                    name        = ex["name"]        as? String ?: "",
                    nameEn      = ex["nameEn"]      as? String ?: "",
                    muscleGroup = ex["muscleGroup"] as? String ?: "",
                    sets        = (ex["sets"]        as? Double)?.toInt() ?: 3,
                    reps        = ex["reps"]        as? String ?: "10",
                    restSeconds = (ex["restSeconds"] as? Double)?.toInt() ?: 60,
                    notes       = ex["notes"]       as? String ?: ""
                )
            }.filterNotNull()

            WorkoutDay(
                id          = UUID.randomUUID().toString(),
                dayName     = day["dayName"]     as? String ?: "Día ${index + 1}",
                dayOrder    = (day["dayOrder"]   as? Double)?.toInt() ?: (index + 1),
                isRestDay   = day["isRestDay"]   as? Boolean ?: false,
                muscleFocus = day["muscleFocus"] as? String ?: "",
                exercises   = exercises
            )
        }.filterNotNull()

        return Routine(
            id          = UUID.randomUUID().toString(),
            goal        = userProfile.goal,
            level       = userProfile.level,
            daysPerWeek = userProfile.daysPerWeek,
            workoutDays = workoutDays
        )
    }
}