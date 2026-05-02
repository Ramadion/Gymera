package com.DeBiaseRamiro.gymera.data.remote.api

import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiResponse
import com.DeBiaseRamiro.gymera.data.remote.dto.GeminiRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {

    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateRoutine(
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}