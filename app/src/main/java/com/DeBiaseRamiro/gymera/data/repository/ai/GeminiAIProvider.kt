package com.DeBiaseRamiro.gymera.data.repository.ai

import com.DeBiaseRamiro.gymera.BuildConfig
import com.DeBiaseRamiro.gymera.data.remote.api.GeminiApi
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequest
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequestContent
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequestPart
import javax.inject.Inject

class GeminiAIProvider @Inject constructor(
    private val geminiApi: GeminiApi
) : RoutineAIProvider {

    override val name: String = "gemini-2.5-flash"

    override suspend fun generate(prompt: String): String {
        val response = geminiApi.generateRoutine(
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(
                    GeminiRequestContent(parts = listOf(GeminiRequestPart(text = prompt)))
                )
            )
        )
        return response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text ?: throw Exception("Respuesta vacía de Gemini")
    }
}