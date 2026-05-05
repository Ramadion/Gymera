package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.data.remote.api.FreeExerciseDbApi
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExerciseImageRepository
 *
 * Responsabilidad única: dado un nombre de ejercicio en inglés,
 * devolver la URL de su imagen desde el repo free-exercise-db.
 *
 * Estrategia de caché en memoria:
 * - La primera vez que se llama a getImageUrl(), descarga el JSON completo
 *   (~800 ejercicios) y lo guarda en _exerciseList.
 * - Las llamadas siguientes usan la lista en memoria directamente.
 * - En feature/room, esto se reemplaza por Room como fuente de verdad.
 *
 * Estrategia de búsqueda (fuzzy matching):
 * El nameEn de Gemini puede ser "barbell bench press" y el repo tiene
 * "Barbell Bench Press - Medium Grip". Usamos varios niveles de matching:
 *   1. Match exacto (ignorando mayúsculas)
 *   2. El nombre del repo contiene el nameEn completo
 *   3. El nameEn contiene el nombre del repo
 *   4. Coincidencia de palabras clave (al menos 2 palabras en común)
 */
@Singleton
class ExerciseImageRepository @Inject constructor(
    private val freeExerciseDbApi: FreeExerciseDbApi
) {
    // Caché en memoria — se llena la primera vez y no se vuelve a descargar
    private var _exerciseList: List<FreeExerciseDto>? = null

    // Base URL para construir las URLs de imagen
    companion object {
        const val IMAGE_BASE_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    }

    /**
     * Dado un nombre de ejercicio en inglés (como lo manda Gemini),
     * devuelve la URL de su imagen, o null si no se encuentra.
     *
     * @param nameEn Nombre en inglés del ejercicio (ej: "barbell bench press")
     */
    suspend fun getImageUrl(nameEn: String): String? {
        if (nameEn.isBlank()) return null

        // Cargamos la lista si no está en memoria todavía
        val exercises = getExerciseList() ?: return null

        // Buscamos el mejor match para el nombre dado
        val match = findBestMatch(nameEn.trim().lowercase(), exercises)
            ?: return null

        // Tomamos la primera imagen del ejercicio encontrado
        val imagePath = match.images.firstOrNull() ?: return null

        return IMAGE_BASE_URL + imagePath
    }

    /**
     * Descarga el JSON completo la primera vez y lo cachea en memoria.
     * Las llamadas siguientes devuelven la lista cacheada directamente.
     */
    private suspend fun getExerciseList(): List<FreeExerciseDto>? {
        // Si ya está en memoria, la devolvemos directamente
        _exerciseList?.let { return it }

        return try {
            android.util.Log.d("GYMERA_DEBUG", "Descargando lista de ejercicios...")
            val list = freeExerciseDbApi.getAllExercises()
            _exerciseList = list
            android.util.Log.d("GYMERA_DEBUG", "Lista descargada: ${list.size} ejercicios")
            list
        } catch (e: Exception) {
            android.util.Log.e("GYMERA_DEBUG", "Error descargando ejercicios: ${e.message}")
            null
        }
    }

    /**
     * Encuentra el ejercicio que mejor coincide con el nombre buscado.
     *
     * Niveles de matching en orden de prioridad:
     * 1. Match exacto
     * 2. El nombre del repo contiene el nameEn completo
     * 3. El nameEn contiene el nombre del repo
     * 4. Al menos 2 palabras clave en común (excluye palabras cortas)
     */
    private fun findBestMatch(
        query: String,
        exercises: List<FreeExerciseDto>
    ): FreeExerciseDto? {

        // Nivel 1: match exacto (ignorando mayúsculas)
        exercises.firstOrNull { it.name.lowercase() == query }
            ?.let { return it }

        // Nivel 2: el nombre del repo contiene el query completo
        // ej: query="bench press" → match con "Barbell Bench Press - Medium Grip"
        exercises.firstOrNull { it.name.lowercase().contains(query) }
            ?.let { return it }

        // Nivel 3: el query contiene el nombre del repo
        // ej: query="dumbbell romanian deadlift" → match con "Romanian Deadlift"
        exercises.firstOrNull { query.contains(it.name.lowercase()) }
            ?.let { return it }

        // Nivel 4: coincidencia de palabras clave
        // Filtramos palabras cortas o genéricas que no aportan al matching
        val stopWords = setOf("the", "a", "an", "with", "on", "at", "to", "of", "in", "and")
        val queryWords = query.split(" ")
            .filter { it.length > 2 && it !in stopWords }
            .toSet()

        if (queryWords.size < 2) return null

        // Buscamos el ejercicio con más palabras en común
        return exercises
            .map { exercise ->
                val exerciseWords = exercise.name.lowercase()
                    .split(" ")
                    .filter { it.length > 2 && it !in stopWords }
                    .toSet()
                val commonWords = queryWords.intersect(exerciseWords).size
                exercise to commonWords
            }
            .filter { (_, commonCount) -> commonCount >= 2 }
            .maxByOrNull { (_, commonCount) -> commonCount }
            ?.first
    }
}