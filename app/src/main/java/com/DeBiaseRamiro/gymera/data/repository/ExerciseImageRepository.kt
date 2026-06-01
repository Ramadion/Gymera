package com.DeBiaseRamiro.gymera.data.repository

import android.content.Context
import android.util.Log
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ── ExerciseImageRepository ───────────────────────────────────────────────────
// Provee imágenes, instrucciones y metadata de ejercicios.
//
// Fuente de datos: assets/gymera_exercises.json (bundleado en el APK).
//
// ── Por qué assets en lugar de red ───────────────────────────────────────────
// La versión anterior descargaba exercises.json desde GitHub en cada sesión.
// Problemas que resolvemos con el cambio a assets:
//
//   1. Cero llamadas de red para ejercicios → preservamos el rate limit de
//      Gemini API (2 req/min gratis) exclusivamente para generar rutinas.
//   2. Funciona 100% offline desde el primer arranque — no necesita conexión
//      ni siquiera una vez para cargar los datos de ejercicios.
//   3. Las instrucciones ya vienen traducidas al español (por translate_exercises.py)
//      así que no necesitamos traducción lazy con Gemini.
//   4. Elimina FreeExerciseDbApi y el segundo Retrofit — arquitectura más simple.
//
// ── Estrategia de caché ───────────────────────────────────────────────────────
// El JSON del asset se deserializa UNA SOLA VEZ y queda en _exerciseList (RAM).
// Llamadas siguientes usan la lista ya cargada — lectura O(1) del mapa en memoria.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class ExerciseImageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val IMAGE_BASE_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

        private const val ASSET_FILE = "gymera_exercises.json"
        private const val TAG = "GYM_EXERCISE_REPO"
    }

    // Cache en memoria del JSON deserializado.
    // null = todavía no se cargó desde el asset.
    // Una vez cargado, nunca se vuelve a leer el asset (vive en RAM mientras
    // el proceso esté activo).
    private var _exerciseList: List<FreeExerciseDto>? = null

    // Mapa auxiliar id → dto para lookups O(1) en getExerciseDetail()
    private var _exerciseMap: Map<String, FreeExerciseDto>? = null

    private val gson = Gson()

    // ── getImageUrl ───────────────────────────────────────────────────────────
    // Devuelve la URL de la primera imagen de un ejercicio dado su nombre en inglés.
    // Usa fuzzy matching de 4 niveles porque el nombre que genera Gemini no siempre
    // coincide exactamente con el campo "name" del repositorio.
    //
    // @param nameEn  nombre en inglés generado por Gemini (ej: "barbell bench press")
    // @return URL completa de imagen, o null si no se encuentra el ejercicio
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getImageUrl(nameEn: String): String? {
        if (nameEn.isBlank()) return null
        val exercises = getExerciseList() ?: return null
        val match = findBestMatch(nameEn.trim().lowercase(), exercises) ?: return null
        val imagePath = match.images.firstOrNull() ?: return null
        return IMAGE_BASE_URL + imagePath
    }

    // ── getExerciseDetail ─────────────────────────────────────────────────────
    // Devuelve el FreeExerciseDto completo de un ejercicio.
    // Usado por ExerciseDetailViewModel para mostrar instrucciones, músculos,
    // equipamiento e imágenes animadas.
    //
    // @param nameEn  nombre en inglés para hacer fuzzy matching
    // @return FreeExerciseDto completo, o null si no se encuentra
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getExerciseDetail(nameEn: String): FreeExerciseDto? {
        if (nameEn.isBlank()) return null
        val exercises = getExerciseList() ?: return null
        return findBestMatch(nameEn.trim().lowercase(), exercises)
    }

    // ── getExerciseList ───────────────────────────────────────────────────────
    // Carga la lista de ejercicios desde el asset la primera vez.
    // Las llamadas siguientes devuelven la lista ya en memoria.
    //
    // Lee assets/gymera_exercises.json, que es el JSON de free-exercise-db
    // con el campo instructionsEs agregado por translate_exercises.py.
    // ─────────────────────────────────────────────────────────────────────────
    private fun getExerciseList(): List<FreeExerciseDto>? {
        // Si ya está en memoria, la devolvemos directamente
        _exerciseList?.let { return it }

        return try {
            Log.d(TAG, "Cargando $ASSET_FILE desde assets...")

            val jsonString = context.assets
                .open(ASSET_FILE)
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<FreeExerciseDto>>() {}.type
            val list: List<FreeExerciseDto> = gson.fromJson(jsonString, type)

            // Guardamos en memoria para no volver a leer el asset
            _exerciseList = list
            _exerciseMap  = list.associateBy { it.id }

            Log.d(TAG, "Asset cargado: ${list.size} ejercicios en memoria")
            list

        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo $ASSET_FILE: ${e.message}", e)
            null
        }
    }

    // ── findBestMatch ─────────────────────────────────────────────────────────
    // Algoritmo de fuzzy matching de 4 niveles de prioridad.
    //
    // Necesario porque Gemini genera nombres en inglés que no siempre coinciden
    // exactamente con el campo "name" del JSON.
    // Ejemplo: Gemini manda "barbell bench press" pero el JSON tiene
    //          "Barbell Bench Press - Medium Grip".
    //
    // Los 4 niveles garantizan que la mayoría de los ejercicios encuentren imagen.
    // ─────────────────────────────────────────────────────────────────────────
    private fun findBestMatch(query: String, exercises: List<FreeExerciseDto>): FreeExerciseDto? {
        // Nivel 1: match exacto ignorando mayúsculas
        // "dumbbell biceps curl" = "Dumbbell Biceps Curl"
        exercises.find { it.name.lowercase() == query }?.let { return it }

        // Nivel 2: el nombre del repositorio CONTIENE el query completo
        // "bench press" encontrado en "Barbell Bench Press - Medium Grip"
        exercises.find { it.name.lowercase().contains(query) }?.let { return it }

        // Nivel 3: el query CONTIENE el nombre del repositorio
        // "dumbbell romanian deadlift" contiene "Romanian Deadlift"
        exercises.find { query.contains(it.name.lowercase()) }?.let { return it }

        // Nivel 4: al menos 2 palabras clave en común (excluyendo stop words)
        // "barbell squat" y "Barbell Squat (on knees)" comparten "barbell" y "squat"
        val stopWords = setOf("the", "a", "an", "with", "on", "in", "at", "to", "for", "of", "and")
        val queryWords = query.split(" ").filter { it.length > 2 && it !in stopWords }.toSet()

        return exercises.find { exercise ->
            val exerciseWords = exercise.name.lowercase()
                .split(" ")
                .filter { it.length > 2 && it !in stopWords }
                .toSet()
            (queryWords intersect exerciseWords).size >= 2
        }
    }

    // ── getAllExercises ────────────────────────────────────────────────────────
    // Devuelve la lista completa de ejercicios para SearchScreen.
    // Lee del asset la primera vez y de memoria las siguientes — sin red.
    // ─────────────────────────────────────────────────────────────────────────
    fun getAllExercises(): List<FreeExerciseDto> {
        return getExerciseList() ?: emptyList()
    }

    // ── getMuscleGroups ───────────────────────────────────────────────────────
    // Devuelve la lista de grupos musculares únicos y ordenados alfabéticamente.
    // Se usa para poblar el desplegable de filtros en SearchScreen.
    // Se deriva de primaryMuscles de todos los ejercicios — sin llamada extra.
    // ─────────────────────────────────────────────────────────────────────────
    fun getMuscleGroups(): List<String> {
        return getExerciseList()
            ?.flatMap { it.primaryMuscles }
            ?.map { it.lowercase() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }
}