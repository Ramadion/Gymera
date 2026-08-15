package com.DeBiaseRamiro.gymera.data.repository.ai

import com.DeBiaseRamiro.gymera.domain.model.Routine
import javax.inject.Inject

class FailoverRoutineGenerator @Inject constructor(
    private val providers: List<@JvmSuppressWildcards RoutineAIProvider>
) {

    suspend fun generate(prompt: String, parse: (String) -> Routine): Routine {
        var lastError: Exception? = null

        for (provider in providers) {
            try {
                val rawJson = provider.generate(prompt)
                val cleanJson = rawJson.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                return parse(cleanJson)
            } catch (e: Exception) {
                lastError = e
                android.util.Log.w("GYM_FAILOVER", "Provider ${provider.name} falló: ${e.message}")
            }
        }

        throw Exception(
            "Todos los servicios de IA fallaron. Intentalo en unos minutos.",
            lastError
        )
    }
}