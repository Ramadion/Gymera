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
    // Matcher por SCORE (reemplazó al fuzzy matching de 4 niveles).
    //
    // El algoritmo anterior devolvía la PRIMERA coincidencia "suficiente"
    // (`.find {}`), lo que provocaba dos bugs conocidos:
    //
    //   1. "overhead barbell press" / "barbell overhead press" (press militar)
    //      coincidía con "Barbell Bench Press - Medium Grip" porque comparten
    //      las palabras {barbell, press} y el bench press aparece antes en el
    //      JSON (orden alfabético). El nuevo matcher elige el MEJOR score, así
    //      que gana "Barbell Shoulder Press".
    //
    //   2. Variantes de "skull crusher" (ej: "ez bar skull crusher") no
    //      encontraban "EZ-Bar Skullcrusher" por la palabra compuesta
    //      ("skullcrusher" vs "skull crusher") y por el guión.
    //
    // Algoritmo nuevo:
    //   - Normaliza el query y TODOS los nombres por igual (guiones, slashes,
    //     paréntesis, comas → espacios) para que "EZ-Bar Skullcrusher" ==
    //     "ez bar skullcrusher".
    //   - Puntúa cada ejercicio por las palabras del query que coinciden:
    //       * token IGUAL = 2 pts
    //       * token por substring/prefix = 1 pt (captura "skullcrusher" vs
    //         "skull crusher")
    //   - Se queda con el de mayor score. Empates → mayor cobertura del query,
    //     y luego menos tokens extra (match más específico).
    //   - Queries de una sola palabra (ej: "deadlift", "squat", "dips") aceptan
    //     un hit de substring para no romper esos casos.
    // ─────────────────────────────────────────────────────────────────────────
    private fun findBestMatch(query: String, exercises: List<FreeExerciseDto>): FreeExerciseDto? {
        val stopWords = setOf("the", "a", "an", "with", "on", "in", "at", "to", "for", "of", "and")

        val queryTokens = normalizeName(query)
            .split(" ")
            .filter { it.length >= 3 && it !in stopWords }
        if (queryTokens.isEmpty()) return null

        // Busca el mejor candidato en UNA sola pasada (no devuelve el primero).
        var best: MatchCandidate? = null

        for (exercise in exercises) {
            val nameTokens = normalizeName(exercise.name)
                .split(" ")
                .filter { it.length >= 3 && it !in stopWords }
            if (nameTokens.isEmpty()) continue

            var score = 0.0
            var matchedTokens = 0

            for (queryToken in queryTokens) {
                var hit = 0.0
                for (nameToken in nameTokens) {
                    if (queryToken == nameToken) {
                        hit = 2.0
                    } else if (
                        queryToken.length >= 4 && nameToken.length >= 4 &&
                        (queryToken in nameToken ||
                         nameToken in queryToken ||
                         queryToken.startsWith(nameToken) ||
                         nameToken.startsWith(queryToken))
                    ) {
                        hit = 1.0
                    }
                    if (hit > 0) break
                }
                if (hit > 0) {
                    score += hit
                    matchedTokens++
                }
            }

            if (matchedTokens >= 1) {
                val coverage = matchedTokens.toDouble() / queryTokens.size
                val candidate = MatchCandidate(score, coverage, matchedTokens, nameTokens.size, exercise)
                if (best == null || candidate.betterThan(best)) best = candidate
            }
        }

        val winner = best ?: return null

        return when {
            // Multi-word: requiere ≥2 tokens, score decente y ≥50% de cobertura.
            queryTokens.size >= 2 ->
                if (winner.matchedTokens >= 2 && winner.score >= 2.5 && winner.coverage >= 0.5)
                    winner.exercise else null
            // Single-word (deadlift, squat, dips, ...): un hit alcanza.
            else ->
                if (winner.score >= 2) winner.exercise else null
        }
    }

    // Normaliza un nombre para comparar igual a ambos lados: minúsculas y los
    // separadores (guión, barra, paréntesis, comas, puntos) → espacios.
    private fun normalizeName(name: String): String =
        name.lowercase()
            .replace("(", " ")
            .replace(")", " ")
            .replace("'", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("-", " ")
            .replace("/", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    // Candidato con métricas del match para comparar puntajes de forma limpia.
    private data class MatchCandidate(
        val score: Double,
        val coverage: Double,
        val matchedTokens: Int,
        val nameTokenCount: Int,
        val exercise: FreeExerciseDto
    ) {
        fun betterThan(other: MatchCandidate): Boolean {
            if (score != other.score) return score > other.score
            if (coverage != other.coverage) return coverage > other.coverage
            return nameTokenCount < other.nameTokenCount
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