package com.DeBiaseRamiro.gymera.data.repository.ai

interface RoutineAIProvider {
    val name: String
    suspend fun generate(prompt: String): String
}