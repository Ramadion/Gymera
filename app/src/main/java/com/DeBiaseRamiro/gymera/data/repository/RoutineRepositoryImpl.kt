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

    override suspend fun generateRoutine(
        userProfile: UserProfile,
        physicalProfile: UserPhysicalProfile?
    ): Routine {

        // Construimos la sección de datos físicos solo si el usuario los completó
        // Cada campo se agrega individualmente para no enviar datos vacíos a Gemini
        val physicalSection = buildString {
            if (physicalProfile != null) {
                val hasAnyData = physicalProfile.age > 0 ||
                        physicalProfile.weightKg > 0f ||
                        physicalProfile.heightCm > 0

                if (hasAnyData) {
                    appendLine("Datos físicos del usuario (tenerlos en cuenta para personalizar la rutina):")
                    if (physicalProfile.age > 0)
                        appendLine("- Edad: ${physicalProfile.age} años")
                    if (physicalProfile.weightKg > 0f)
                        appendLine("- Peso: ${physicalProfile.weightKg} kg")
                    if (physicalProfile.heightCm > 0)
                        appendLine("- Altura: ${physicalProfile.heightCm} cm")
                    if (physicalProfile.gender.isNotBlank() &&
                        physicalProfile.gender != "No especificado")
                        appendLine("- Género: ${physicalProfile.gender}")
                }
            }
        }

        val prompt = """
            Eres un entrenador personal experto. Genera una rutina de entrenamiento semanal
            personalizada basada en estos datos del usuario:
            - Objetivo: ${userProfile.goal}
            - Dias por semana que puede entrenar: ${userProfile.daysPerWeek}
            - Duracion por sesion: ${userProfile.sessionDuration} minutos
            - Nivel de experiencia: ${userProfile.level}
            - Lesiones o limitaciones: ${userProfile.limitations.ifEmpty { "Ninguna" }}
            ${physicalSection.ifBlank { "" }}
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

    override suspend fun saveRoutine(routine: Routine, userUid: String) {
        // Borramos TODAS las rutinas anteriores — solo existe 1 a la vez
        routineDao.deleteAllRoutines(userUid)

        routineDao.insertRoutine(
            RoutineEntity(
                id              = routine.id,
                userUid         = userUid,
                goal            = routine.goal,
                daysPerWeek     = routine.daysPerWeek,
                sessionDuration = 0,
                level           = routine.level,
                limitations     = "",
                generatedAt     = System.currentTimeMillis(),
                isActive        = 1
            )
        )

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

        val exerciseEntities = routine.workoutDays.flatMap { day ->
            day.exercises.mapIndexed { index, exercise ->
                ExerciseAssignmentEntity(
                    id           = exercise.id,
                    workoutDayId = day.id,
                    nameEs       = exercise.name,
                    nameEn       = exercise.nameEn
                        .replace(oldValue = "-", newValue = " ")
                        .replace(oldValue = "/", newValue = " "),
                    muscleGroup  = exercise.muscleGroup,
                    sets         = exercise.sets,
                    reps         = exercise.reps,
                    restSeconds  = exercise.restSeconds,
                    orderInDay   = index,
                    notes        = exercise.notes
                )
            }
        }
        routineDao.insertExercises(exerciseEntities)
    }

    override fun getActiveRoutineFlow(userUid: String): Flow<Routine?> =
        routineDao.getActiveRoutineFlow(userUid).map { entity ->
            entity?.let { buildRoutineFromEntity(it) }
        }

    override suspend fun getActiveRoutine(userUid: String): Routine? {
        val entity = routineDao.getActiveRoutine(userUid) ?: return null
        return buildRoutineFromEntity(entity)
    }

    override suspend fun deactivateActiveRoutine(userUid: String) {
        routineDao.deactivateAllRoutines(userUid)
    }

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