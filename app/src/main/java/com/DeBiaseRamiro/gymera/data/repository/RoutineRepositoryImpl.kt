package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.BuildConfig
import com.DeBiaseRamiro.gymera.data.remote.api.GeminiApi
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequest
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequestContent
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequestPart
import com.DeBiaseRamiro.gymera.domain.model.Exercise
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.model.WorkoutDay
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApi
) : RoutineRepository {

    override suspend fun generateRoutine(userProfile: UserProfile): Routine {

        // Construimos el prompt — muy específico para forzar JSON válido
        val prompt = """
            Eres un entrenador personal experto. Genera una rutina de entrenamiento semanal 
            personalizada basada en estos datos del usuario:
            
            - Objetivo: ${userProfile.goal}
            - Días por semana: ${userProfile.daysPerWeek}
            - Duración por sesión: ${userProfile.sessionDuration} minutos
            - Nivel: ${userProfile.level}
            - Limitaciones físicas: ${userProfile.limitations.ifEmpty { "Ninguna" }}
            
            IMPORTANTE: Responde ÚNICAMENTE con un JSON válido, sin texto adicional, 
            sin markdown, sin bloques de código. Solo el JSON puro.
            
            El formato debe ser exactamente este:
            {
              "workoutDays": [
                {
                  "dayName": "Lunes",
                  "dayOrder": 1,
                  "isRestDay": false,
                  "muscleFocus": "Pecho y Tríceps",
                  "exercises": [
                    {
                      "name": "Press de banca",
                      "muscleGroup": "Pecho",
                      "sets": 4,
                      "reps": "8-12",
                      "restSeconds": 90,
                      "notes": "Bajar controlado"
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
            
            Genera exactamente 7 días (Lunes a Domingo). 
            Los días de descanso tienen isRestDay: true y exercises vacío.
            Cada día de entrenamiento debe tener entre 4 y 6 ejercicios.
        """.trimIndent()

        // Armamos el body de la request
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiRequestContent(
                    parts = listOf(GeminiRequestPart(text = prompt))
                )
            )
        )

        // Llamamos a Gemini
        val response = geminiApi.generateRoutine(
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = requestBody
        )

        // Extraemos el texto de la respuesta
        val jsonText = response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text ?: throw Exception("Respuesta vacía de Gemini")

        // Limpiamos por si Gemini igual agrega markdown
        val cleanJson = jsonText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Parseamos el JSON
        return parseRoutineFromJson(cleanJson, userProfile)
    }

    private fun parseRoutineFromJson(json: String, userProfile: UserProfile): Routine {
        val gson = Gson()

        // Tipo para parsear el mapa raíz
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val rootMap: Map<String, Any> = gson.fromJson(json, mapType)

        val workoutDaysRaw = rootMap["workoutDays"] as? List<*> ?: emptyList<Any>()

        val workoutDays = workoutDaysRaw.mapIndexed { index, dayRaw ->
            val day = dayRaw as? Map<*, *> ?: return@mapIndexed null

            val exercisesRaw = day["exercises"] as? List<*> ?: emptyList<Any>()
            val exercises = exercisesRaw.mapIndexed { exIndex, exRaw ->
                val ex = exRaw as? Map<*, *> ?: return@mapIndexed null
                Exercise(
                    id = UUID.randomUUID().toString(),
                    name = ex["name"] as? String ?: "",
                    muscleGroup = ex["muscleGroup"] as? String ?: "",
                    sets = (ex["sets"] as? Double)?.toInt() ?: 3,
                    reps = ex["reps"] as? String ?: "10",
                    restSeconds = (ex["restSeconds"] as? Double)?.toInt() ?: 60,
                    notes = ex["notes"] as? String ?: ""
                )
            }.filterNotNull()

            WorkoutDay(
                id = UUID.randomUUID().toString(),
                dayName = day["dayName"] as? String ?: "Día ${index + 1}",
                dayOrder = (day["dayOrder"] as? Double)?.toInt() ?: (index + 1),
                isRestDay = day["isRestDay"] as? Boolean ?: false,
                muscleFocus = day["muscleFocus"] as? String ?: "",
                exercises = exercises
            )
        }.filterNotNull()

        return Routine(
            id = UUID.randomUUID().toString(),
            goal = userProfile.goal,
            level = userProfile.level,
            daysPerWeek = userProfile.daysPerWeek,
            workoutDays = workoutDays
        )
    }
}