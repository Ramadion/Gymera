package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseAssignmentEntity
import com.DeBiaseRamiro.gymera.data.local.entity.RoutineEntity
import com.DeBiaseRamiro.gymera.data.local.entity.WorkoutDayEntity
import com.DeBiaseRamiro.gymera.data.repository.ai.FailoverRoutineGenerator
import com.DeBiaseRamiro.gymera.domain.model.*
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val aiGenerator: FailoverRoutineGenerator,
    private val routineDao: RoutineDao
) : RoutineRepository {

    override suspend fun generateRoutine(
        userProfile: UserProfile,
        physicalProfile: UserPhysicalProfile?
    ): Routine {

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

        val exercisesPerSession = when {
            userProfile.sessionDuration >= 90 -> "7 a 8 ejercicios"
            userProfile.sessionDuration >= 60 -> "5 a 6 ejercicios"
            userProfile.sessionDuration >= 45 -> "4 a 5 ejercicios"
            else                              -> "3 a 4 ejercicios"
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
            4. CADA dia de entrenamiento debe tener ${exercisesPerSession}:
               la duracion de la sesion es ${userProfile.sessionDuration} minutos y hay que cubrirla
               con una cantidad acorde de ejercicios. NUNCA dejes poca cantidad de ejercicios.
            5. Cada ejercicio DEBE tener dos nombres:
               - "name": el nombre en ESPAÑOL (para mostrar al usuario)
               - "nameEn": el nombre en INGLES exacto como aparece en bases de datos
                 de ejercicios internacionales (para busqueda interna)
            6. IMPORTANTE: respeta las limitaciones del usuario. Si reporta dolor o lesion
               (ej: rodilla, espalda, hombros), NO incluyas ejercicios que los agraven.
               Ejemplos: dolor de rodilla = nada de sentadillas, zancadas, peso muerto ni saltos.
               Dolor de espalda = nada de peso muerto, remo pesado ni hiperextensiones.
            7. Responde UNICAMENTE con JSON valido. Sin texto antes ni despues.
               Sin markdown, sin bloques de codigo, sin ```.
            8. IMPORTANTE: el objeto RAIZ del JSON debe ser EXACTAMENTE {"workoutDays": [...]}.
               No envuelvas el JSON en objetos adicionales (ej: NO uses "weeklyWorkoutPlan"
               ni ninguna otra clave envoltorio). Solo la clave "workoutDays" en la raiz.

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
                      "notes": "Ajustar el peso"
                    },
                    {
                      "name": "Aperturas con mancuernas",
                      "nameEn": "dumbbell fly",
                      "muscleGroup": "Pecho",
                      "sets": 3,
                      "reps": "10-15",
                      "restSeconds": 60,
                      "notes": "Controlar la bajada"
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

        return aiGenerator.generate(prompt) { json ->
            parseRoutineFromJson(json, userProfile)
        }
    }

    override suspend fun saveRoutine(routine: Routine, userUid: String) {
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

    // ── Edición manual de ejercicios ──────────────────────────────────────

    override suspend fun removeExercise(exerciseId: String) {
        routineDao.deleteExercise(exerciseId)
    }

    // Persiste el nuevo orden actualizando orderInDay de cada ejercicio.
    // Se llama una sola vez al soltar el drag — no en cada posición intermedia.
    override suspend fun reorderExercises(exercises: List<Exercise>) {
        exercises.forEachIndexed { index, exercise ->
            routineDao.updateExerciseOrder(exercise.id, index)
        }
    }

    // Inserta un ejercicio nuevo al final de un día específico.
    override suspend fun addExercise(dayId: String, exercise: Exercise, orderInDay: Int) {
        routineDao.insertExercise(
            ExerciseAssignmentEntity(
                id           = exercise.id,
                workoutDayId = dayId,
                nameEs       = exercise.name,
                nameEn       = exercise.nameEn,
                muscleGroup  = exercise.muscleGroup,
                sets         = exercise.sets,
                reps         = exercise.reps,
                restSeconds  = exercise.restSeconds,
                orderInDay   = orderInDay,
                notes        = exercise.notes
            )
        )
    }

    // ── Edición de días ──────────────────────────────────────────────────

    // Solo flipea el flag: los ejercicios del día quedan intactos en Room,
    // ocultos mientras el día es descanso.
    override suspend fun setWorkoutDayRest(dayId: String, isRest: Boolean) {
        routineDao.updateWorkoutDayIsRest(dayId, if (isRest) 1 else 0)
    }

    override suspend fun setWorkoutDayMuscleFocus(dayId: String, muscleFocus: String) {
        routineDao.updateWorkoutDayMuscleFocus(dayId, muscleFocus)
    }

    // Borra los ejercicios del día pero NO el día: queda como entrenamiento
    // con 0 ejercicios.
    override suspend fun clearExercisesFromDay(dayId: String) {
        routineDao.deleteExercisesForDay(dayId)
    }

    // 1) Los ejercicios del día origen pasan al día destino.
    // 2) El día destino deja de ser descanso y queda activo.
    // 3) Se copia la descripción (muscleFocus) del origen al destino.
    // 4) Se renumera el orden del día destino (los movidos iban con su orden viejo).
    // 5) El día origen queda como día de descanso vacío (sin ejercicios ni descripción).
    override suspend fun moveExercisesToRestDay(fromDayId: String, toDayId: String) {
        val sourceDay = routineDao.getWorkoutDayById(fromDayId)
        routineDao.reassignExercises(fromDayId, toDayId)
        routineDao.updateWorkoutDayIsRest(toDayId, 0)
        routineDao.updateWorkoutDayMuscleFocus(toDayId, sourceDay?.muscleFocus ?: "")

        val reordered = routineDao.getExercisesForDay(toDayId)
            .sortedBy { it.orderInDay }
        reordered.forEachIndexed { index, exercise ->
            routineDao.updateExerciseOrder(exercise.id, index)
        }

        routineDao.updateWorkoutDayIsRest(fromDayId, 1)
        routineDao.updateWorkoutDayMuscleFocus(fromDayId, "")
    }

    // ── Helpers privados ──────────────────────────────────────────────────

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

        // Algunos proveedores (ej: Gemini) envuelven el resultado en un objeto
        // extra como "weeklyWorkoutPlan". Si "workoutDays" no está en la raíz,
        // buscamos dentro del primer objeto anidado.
        val workoutDaysRaw = rootMap["workoutDays"] as? List<*>
            ?: rootMap.values.firstNotNullOfOrNull { value ->
                (value as? Map<*, *>)?.get("workoutDays") as? List<*>
            }

        // Si no hay workoutDays, lanzamos excepción para que el failover
        // multicanal salte automáticamente al siguiente proveedor.
        if (workoutDaysRaw == null) {
            throw IllegalArgumentException("La respuesta de la IA no tiene workoutDays")
        }

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