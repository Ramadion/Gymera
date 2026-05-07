package com.DeBiaseRamiro.gymera.data.local.dao

import androidx.room.*
import com.DeBiaseRamiro.gymera.data.local.entity.ExerciseCacheEntity

@Dao
interface ExerciseCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseCacheEntity>)

    // Devuelve el total de ejercicios cacheados.
    // Si es > 0, el JSON ya fue descargado y no hay que volver a hacerlo.
    @Query("SELECT COUNT(*) FROM exercise_cache")
    suspend fun getCount(): Int

    // Búsqueda por nombre exacto (case-insensitive) — nivel 1 del fuzzy matching
    @Query("SELECT * FROM exercise_cache WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByExactName(name: String): ExerciseCacheEntity?

    // Búsqueda por nombre que contiene el query — niveles 2 y 3
    @Query("SELECT * FROM exercise_cache WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' LIMIT 1")
    suspend fun findByNameContains(query: String): ExerciseCacheEntity?

    // Devuelve todos para el fuzzy matching en memoria (nivel 4)
    // Solo se llama si los niveles 1-3 fallaron
    @Query("SELECT * FROM exercise_cache")
    suspend fun getAll(): List<ExerciseCacheEntity>
}