package com.DeBiaseRamiro.gymera.data.remote.api

import com.DeBiaseRamiro.gymera.data.remote.dto.GroqChatRequest
import com.DeBiaseRamiro.gymera.data.remote.dto.GroqChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body body: GroqChatRequest
    ): GroqChatResponse
}