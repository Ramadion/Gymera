package com.DeBiaseRamiro.gymera.data.remote.api

import com.DeBiaseRamiro.gymera.data.remote.dto.ExerciseDbDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ExerciseDbApi — interfaz Retrofit para la API de ExerciseDB (RapidAPI).
 *
 * Usamos dos endpoints:
 * 1. searchByName → para buscar un ejercicio por nombre en inglés y obtener su ID
 * 2. getImage     → construimos la URL directamente (no es un endpoint Retrofit
 *                   porque devuelve bytes de imagen, no JSON)
 *
 * La URL base es https://exercisedb.p.rapidapi.com/
 * Los headers x-rapidapi-key y x-rapidapi-host van en cada llamada.
 */
interface ExerciseDbApi {

    /**
     * Busca ejercicios por nombre (substring, case-insensitive).
     * Usamos limit=1 porque solo necesitamos el primer resultado
     * que matchee con el nombre en inglés que mandó Gemini.
     *
     * @param apiKey  Tu RapidAPI key (de BuildConfig)
     * @param name    Nombre del ejercicio en inglés (ej: "barbell bench press")
     * @param limit   Cantidad de resultados (usamos 1)
     */
    @GET("exercises/name/{name}")
    suspend fun searchByName(
        @Header("x-rapidapi-key")  apiKey: String,
        @Header("x-rapidapi-host") host: String = "exercisedb.p.rapidapi.com",
        @Path("name")              name: String,
        @Query("limit")            limit: Int = 1
    ): List<ExerciseDbDto>
}