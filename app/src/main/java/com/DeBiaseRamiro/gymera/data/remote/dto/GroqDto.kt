package com.DeBiaseRamiro.gymera.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqChatMessage>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("response_format") val responseFormat: GroqResponseFormat
)

data class GroqChatMessage(
    val role: String,
    val content: String
)

data class GroqResponseFormat(
    val type: String
)

data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChatChoice>,
    val usage: GroqUsage?
)

data class GroqChatChoice(
    val index: Int,
    val message: GroqChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class GroqUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)