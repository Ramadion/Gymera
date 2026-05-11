package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.data.local.dao.ExerciseCacheDao
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseCacheEntity
import com.DeBiaseRamiro.gymera.data.remote.api.FreeExerciseDbApi
import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseImageRepository @Inject constructor(
    private val freeExerciseDbApi: FreeExerciseDbApi,
    private val exerciseCacheDao: ExerciseCacheDao   // ← NUEVO
) {
    companion object {
        const val IMAGE_BASE_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    }

    // Cache en memoria para la sesión actual — evita hits a Room en cada frame
    private var _exerciseList: List<FreeExerciseDto>? = null

    // ── API pública ───────────────────────────────────────────────────────

    suspend fun getImageUrl(nameEn: String): String? {
        if (nameEn.isBlank()) return null
        val cleanName = nameEn.replace("+", " ").trim().lowercase()
        val exercises = getExerciseList() ?: return null
        val match = findBestMatch(cleanName, exercises) ?: return null
        return match.images.firstOrNull()?.let { IMAGE_BASE_URL + it }
    }

    suspend fun getExerciseDetail(nameEn: String): FreeExerciseDto? {
        if (nameEn.isBlank()) return null
        val cleanName = nameEn.replace("+", " ").trim().lowercase()
        val exercises = getExerciseList() ?: return null
        return findBestMatch(cleanName, exercises)
    }

    fun getImageUrlFromDto(dto: FreeExerciseDto): String? =
        dto.images.firstOrNull()?.let { IMAGE_BASE_URL + it }

    // ── Carga de la lista con cache en 3 niveles ──────────────────────────
    // Nivel 1: memoria RAM (más rápido, dura lo que dura el proceso)
    // Nivel 2: Room/SQLite (persiste entre reinicios, nunca más descarga)
    // Nivel 3: red (solo la primera vez en la vida de la app)

    private suspend fun getExerciseList(): List<FreeExerciseDto>? {
        // Nivel 1: ya está en RAM
        _exerciseList?.let { return it }

        // Nivel 2: está en Room — convertimos entities a DTOs y cargamos en RAM
        val cachedCount = exerciseCacheDao.getCount()
        if (cachedCount > 0) {
            val fromRoom = exerciseCacheDao.getAll().map { it.toDto() }
            _exerciseList = fromRoom
            return fromRoom
        }

        // Nivel 3: primera vez — descargamos de GitHub y guardamos en Room
        return try {
            val downloaded = freeExerciseDbApi.getAllExercises()
            // Guardamos en Room para que nunca más se necesite la red
            saveToRoom(downloaded)
            _exerciseList = downloaded
            downloaded
        } catch (e: Exception) {
            android.util.Log.e("GYM_DEBUG", "Error descargando ejercicios: ${e.message}")
            null
        }
    }

    // ── Persistencia en Room ──────────────────────────────────────────────

    private suspend fun saveToRoom(exercises: List<FreeExerciseDto>) {
        val entities = exercises.map { dto ->
            ExerciseCacheEntity(
                id               = dto.id,
                name             = dto.name,
                primaryMuscles   = dto.primaryMuscles.joinToString(","),
                secondaryMuscles = dto.secondaryMuscles.joinToString(","),
                equipment        = dto.equipment ?: "",
                level            = dto.level,
                category         = dto.category,
                // Guardamos las primeras 2 URLs de imagen ya construidas
                imageUrl         = dto.images.getOrNull(0)?.let { IMAGE_BASE_URL + it } ?: "",
                imageUrl2        = dto.images.getOrNull(1)?.let { IMAGE_BASE_URL + it } ?: "",
                instructions     = dto.instructions.joinToString("||"), // separador especial
                cachedAt         = System.currentTimeMillis()
            )
        }
        exerciseCacheDao.insertAll(entities)
    }

    // ── Conversión Entity → DTO ───────────────────────────────────────────
    // Reconstruimos el DTO desde Room para que el resto del código
    // siga funcionando igual sin saber si vino de red o de Room

    private fun ExerciseCacheEntity.toDto(): FreeExerciseDto {
        // Reconstruimos la ruta relativa de imagen desde la URL completa
        val relativeImage1 = imageUrl.removePrefix(IMAGE_BASE_URL)
        val relativeImage2 = imageUrl2.removePrefix(IMAGE_BASE_URL)

        return FreeExerciseDto(
            id               = id,
            name             = name,
            primaryMuscles   = if (primaryMuscles.isBlank()) emptyList()
            else primaryMuscles.split(","),
            secondaryMuscles = if (secondaryMuscles.isBlank()) emptyList()
            else secondaryMuscles.split(","),
            equipment        = equipment.ifBlank { null },
            level            = level,
            category         = category,
            instructions     = if (instructions.isBlank()) emptyList()
            else instructions.split("||"),
            // El DTO espera rutas relativas en images[], igual que el JSON original
            images           = listOfNotNull(
                relativeImage1.ifBlank { null },
                relativeImage2.ifBlank { null }
            )
        )
    }

    // ── Fuzzy matching —──────────────────────────────────────

    private fun findBestMatch(query: String, exercises: List<FreeExerciseDto>): FreeExerciseDto? {
        exercises.firstOrNull { it.name.lowercase() == query }?.let { return it }
        exercises.firstOrNull { it.name.lowercase().contains(query) }?.let { return it }
        exercises.firstOrNull { query.contains(it.name.lowercase()) }?.let { return it }

        val stopWords = setOf("the", "a", "an", "with", "on", "at", "to", "of", "in", "and")
        val queryWords = query.split(" ")
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
        if (queryWords.size < 2) return null

        return exercises
            .map { exercise ->
                val exerciseWords = exercise.name.lowercase()
                    .split(" ")
                    .filter { it.length > 2 && it !in stopWords }
                    .toSet()
                exercise to queryWords.intersect(exerciseWords).size
            }
            .filter { (_, count) -> count >= 2 }
            .maxByOrNull { (_, count) -> count }
            ?.first
    }


    // Parte del SEARCH

    // Devuelve la lista completa para que SearchViewModel pueda filtrar en memoria.
    // Usa el mismo cache de 3 niveles — si ya está en RAM no toca nada.
    suspend fun getAllExercises(): List<FreeExerciseDto> {
        return getExerciseList() ?: emptyList()
    }

    // Devuelve los grupos musculares únicos para el desplegable de filtros.
    // Los extrae de primaryMuscles de todos los ejercicios, los ordena alfabéticamente.
    suspend fun getMuscleGroups(): List<String> {
        return getExerciseList()
            ?.flatMap { it.primaryMuscles }
            ?.map { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }
}