package com.DeBiaseRamiro.gymera.data.local.dao

import androidx.room.*
import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    // Flow para que ProfileScreen se actualice reactivamente
    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    fun getProfileFlow(uid: String): Flow<UserProfileEntity?>

    // Lectura puntual para el prompt de Gemini
    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getProfile(uid: String): UserProfileEntity?

    // REPLACE: si ya existe, lo actualiza; si no, lo inserta
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)
}