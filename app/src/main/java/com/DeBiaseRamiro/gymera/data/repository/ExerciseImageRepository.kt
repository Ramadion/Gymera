package com.DeBiaseRamiro.gymera.data.repository

import android.content.Context
import android.util.Log
import com.DeBiaseRamiro.gymera.data.local.dao.ExerciseCacheDao
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseCacheEntity
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
// Tres niveles: RAM → Room (exercise_cache) → asset.
//
//   1. RAM: _exerciseList vive durante todo el proceso — lecturas O(1).
//   2. Room: al primer arranque se siembra desde el asset (insertAll) y en los
//      arranques siguientes se lee de la tabla (query rápida) en vez de
//      re-parsear los ~1.5MB del JSON.
//   3. Asset: solo se lee cuando Room está vacío o DESACTUALIZADO. La
//      desactualización se detecta comparando MAX(cachedAt) contra
//      PackageInfo.lastUpdateTime: si la caché fue sembrada por una versión
//      anterior del APK (asset distinto), se limpia y re-siembra.
//
// Todas las operaciones de I/O corren en Dispatchers.IO (antes el parseo del
// asset bloqueaba el main thread en el primer acceso a Búsqueda/Día).
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class ExerciseImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseCacheDao: ExerciseCacheDao
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

    // Mutex para single-flight: solo una corrutina ejecuta el pipeline de carga;
    // las demás esperan en cola y entran por el fast-path de RAM.
    private val loadMutex = Mutex()

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
    // Devuelve la lista completa de ejercicios. Flujo de 3 niveles:
    //
    //   RAM → Room → asset.
    //
    //   - RAM: si el proceso ya la cargó, retorno directo.
    //   - Room: si exercise_cache tiene filas SEMBRADAS POR ESTA versión del
    //     APK (MAX(cachedAt) >= lastUpdateTime), se leen de la tabla y se
    //     materializan a DTOs — mucho más barato que re-parsear el JSON.
    //   - Asset: si Room está vacío (primera instalación) o desactualizado
    //     (nuevo APK con asset nuevo), se lee y parsea el JSON, se siembra
    //     Room (limpiando antes si había datos viejos) y queda en RAM.
    //
    // Todo corre en Dispatchers.IO — nunca bloquea el main thread.
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun getExerciseList(): List<FreeExerciseDto>? {
        // Fast-path: ya está en memoria, sin lock.
        _exerciseList?.let { return it }

        return withContext(Dispatchers.IO) {
            loadMutex.withLock {
                // Re-check dentro del lock (otra corrutina puede haber cargado).
                _exerciseList?.let { return@withLock it }

                try {
                    // Nivel 2: caché persistente en Room (si es de esta versión del APK)
                    val cachedCount = exerciseCacheDao.getCount()
                    if (cachedCount > 0) {
                        val latestCachedAt = exerciseCacheDao.getLatestCachedAt() ?: 0L
                        if (latestCachedAt >= apkLastUpdateTime()) {
                            val entities = exerciseCacheDao.getAll()
                            if (entities.isNotEmpty()) {
                                val list = entities.map { it.toDto() }
                                _exerciseList = list
                                _exerciseMap  = list.associateBy { it.id }
                                Log.d(TAG, "Caché Room cargada: ${list.size} ejercicios en memoria")
                                return@withLock list
                            }
                        } else {
                            Log.d(TAG, "Caché Room vieja ($cachedCount filas de un APK anterior) — re-sembrando desde asset")
                            exerciseCacheDao.clearAll()
                        }
                    }

                    // Nivel 3: leer + parsear el asset y sembrar Room
                    Log.d(TAG, "Cargando $ASSET_FILE desde assets...")
                    val jsonString = context.assets
                        .open(ASSET_FILE)
                        .bufferedReader()
                        .use { it.readText() }

                    val type = object : TypeToken<List<FreeExerciseDto>>() {}.type
                    val list: List<FreeExerciseDto> = gson.fromJson(jsonString, type)

                    val now = System.currentTimeMillis()
                    exerciseCacheDao.insertAll(list.map { it.toEntity(now) })

                    _exerciseList = list
                    _exerciseMap  = list.associateBy { it.id }

                    Log.d(TAG, "Asset cargado y cacheado en Room: ${list.size} ejercicios")
                    list

                } catch (e: Exception) {
                    Log.e(TAG, "Error leyendo $ASSET_FILE: ${e.message}", e)
                    null
                }
            }
        }
    }

    // Timestamp de instalación/última actualización del APK. Una caché sembrada
    // ANTES de este instante corresponde a un asset viejo y debe re-sembrarse.
    private fun apkLastUpdateTime(): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer lastUpdateTime: ${e.message}")
        0L
    }

    // ── Mapeo ExerciseCacheEntity <-> FreeExerciseDto ────────────────────────
    // Las listas (músculos, instrucciones) se serializan como JSON string porque
    // Room no soporta List<String> sin TypeConverter. images[0]/images[1] cubren
    // el 100% del asset: los 873 ejercicios tienen exactamente 2 imágenes.
    // force/mechanic no se persisten — verificado: cero usos externos.
    // equipment es String? en el DTO y NOT NULL en la tabla: "" <-> null.

    private fun FreeExerciseDto.toEntity(cachedAt: Long) = ExerciseCacheEntity(
        id               = id,
        name             = name,
        primaryMuscles   = gson.toJson(primaryMuscles),
        secondaryMuscles = gson.toJson(secondaryMuscles),
        equipment        = equipment ?: "",
        level            = level,
        category         = category,
        imageUrl         = images.getOrNull(0) ?: "",
        imageUrl2        = images.getOrNull(1) ?: "",
        instructions     = gson.toJson(instructions),
        instructionsEs   = gson.toJson(instructionsEs),
        cachedAt         = cachedAt
    )

    private fun ExerciseCacheEntity.toDto() = FreeExerciseDto(
        id               = id,
        name             = name,
        level            = level,
        equipment        = equipment.ifEmpty { null },
        primaryMuscles   = fromJsonStringList(primaryMuscles),
        secondaryMuscles = fromJsonStringList(secondaryMuscles),
        category         = category,
        images           = listOfNotNull(
            imageUrl.takeIf { it.isNotBlank() },
            imageUrl2.takeIf { it.isNotBlank() }
        ),
        instructions     = fromJsonStringList(instructions),
        instructionsEs   = fromJsonStringList(instructionsEs)
    )

    private fun fromJsonStringList(json: String): List<String> = try {
        gson.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
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
                        queryToken.length >= 3 && nameToken.length >= 3 &&
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

    // Normaliza un nombre para comparar igual a ambos lados: minúsculas, los
    // separadores (guión, barra, paréntesis, comas, puntos) → espacios, y
    // stemming básico inglés para resolver plural/singular (flyes→fly, dips→dip).
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
            .split(" ")
            .joinToString(" ") { stemEnglish(it) }

    // Stemming básico para resolver plural/singular en nombres de ejercicios.
    // No es un stemmer completo — solo cubre los patrones comunes del asset:
    //   flyes→fly, crunches→crunch, dips→dip, curls→curl, presses→press,
    //   rows→row, raises→raise, extensions→extension, etc.
    private fun stemEnglish(word: String): String {
        if (word.length <= 3) return word
        return when {
            word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"   // crunches→crunche → no; lefties→left
            word.endsWith("es")  && word.length > 4 -> word.dropLast(2)         // flyes→fly, dips→di (handled below)
            word.endsWith("ses") && word.length > 5 -> word.dropLast(2)         // presses→press (not presse)
            word.endsWith("xes") && word.length > 5 -> word.dropLast(2)         // boxes→box (not boxe)
            word.endsWith("zes") && word.length > 5 -> word.dropLast(2)         // quizzes→quiz
            word.endsWith("ches") && word.length > 5 -> word.dropLast(2)        // watches→watch
            word.endsWith("shes") && word.length > 5 -> word.dropLast(2)        // dishes→dish
            word.endsWith("sses") && word.length > 5 -> word.dropLast(2)        // passes→pass
            word.endsWith("s") && !word.endsWith("ss") && word.length > 3
                && !word.endsWith("us") && !word.endsWith("is")                 // dips→dip, reps→rep
                    -> word.dropLast(1)
            else -> word
        }
    }

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
    // Lee de RAM, Room o asset según corresponda (ver getExerciseList) — sin red.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getAllExercises(): List<FreeExerciseDto> {
        return getExerciseList() ?: emptyList()
    }

    // ── getMuscleGroups ───────────────────────────────────────────────────────
    // Devuelve la lista de grupos musculares únicos y ordenados alfabéticamente.
    // Se usa para poblar el desplegable de filtros en SearchScreen.
    // Se deriva de primaryMuscles de todos los ejercicios — sin llamada extra.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getMuscleGroups(): List<String> {
        return getExerciseList()
            ?.flatMap { it.primaryMuscles }
            ?.map { it.lowercase() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }
}