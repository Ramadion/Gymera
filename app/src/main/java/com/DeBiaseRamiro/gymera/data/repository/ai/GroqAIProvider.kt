package com.DeBiaseRamiro.gymera.data.repository.ai

import com.DeBiaseRamiro.gymera.BuildConfig
import com.DeBiaseRamiro.gymera.data.remote.api.GroqApi
import com.DeBiaseRamiro.gymera.data.remote.dto.GroqChatMessage
import com.DeBiaseRamiro.gymera.data.remote.dto.GroqChatRequest
import com.DeBiaseRamiro.gymera.data.remote.dto.GroqResponseFormat
import javax.inject.Inject

class GroqAIProvider @Inject constructor(
    private val groqApi: GroqApi,
    private val modelId: String,
    private val maxTokens: Int
) : RoutineAIProvider {

    override val name: String = "groq-$modelId"

    override suspend fun generate(prompt: String): String {
        val response = groqApi.createChatCompletion(
            authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
            body = GroqChatRequest(
                model = modelId,
                messages = listOf(
                    GroqChatMessage(
                        role = "system",
                        content = "Eres un entrenador personal experto. Siempre respondes UNICAMENTE con un objeto JSON valido y sin texto adicional. No usas markdown, bloques de codigo ni ```."
                    ),
                    GroqChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.7,
                maxTokens = maxTokens,
                responseFormat = GroqResponseFormat(type = "json_object")
            )
        )
        return response.choices.firstOrNull()
            ?.message?.content ?: throw Exception("Respuesta vacía de Groq ($modelId)")
    }
}