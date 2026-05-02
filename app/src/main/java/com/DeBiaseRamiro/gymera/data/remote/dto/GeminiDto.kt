package com.DeBiaseRamiro.gymera.data.remote.dto

import com.google.gson.annotations.SerializedName

// Estructura completa de la respuesta de Gemini
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

data class GeminiCandidate(
    val content: GeminiContent
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// Request
data class GeminiRequest(
    val contents: List<GeminiRequestContent>
)

data class GeminiRequestContent(
    val parts: List<GeminiRequestPart>
)

data class GeminiRequestPart(
    val text: String
)