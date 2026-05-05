package com.DeBiaseRamiro.gymera.data.remote.api

import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import retrofit2.http.GET

/**
 * FreeExerciseDbApi — interfaz Retrofit para el repo público free-exercise-db.
 *
 * Un solo endpoint: descarga el JSON completo con todos los ejercicios.
 * Sin API key, sin límites, completamente público.
 *
 * Base URL: https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/
 */
interface FreeExerciseDbApi {

    /**
     * Descarga el JSON completo con todos los ejercicios (~800 ejercicios).
     * Se llama UNA SOLA VEZ y se cachea en memoria (y luego en Room).
     */
    @GET("dist/exercises.json")
    suspend fun getAllExercises(): List<FreeExerciseDto>
}