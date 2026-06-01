package com.DeBiaseRamiro.gymera.di

import android.content.Context
import com.DeBiaseRamiro.gymera.data.remote.api.GeminiApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import androidx.room.Room
import com.DeBiaseRamiro.gymera.data.local.dao.ExerciseCacheDao
import com.DeBiaseRamiro.gymera.data.local.dao.RoutineDao
import com.DeBiaseRamiro.gymera.data.local.dao.UserProfileDao
import com.DeBiaseRamiro.gymera.data.local.database.GymeraDatabase
import dagger.hilt.android.qualifiers.ApplicationContext

// ── @GeminiRetrofit ───────────────────────────────────────────────────────────
// Qualifier para identificar el Retrofit de Gemini.
// Antes existía también @FreeExerciseDbRetrofit, pero se eliminó al pasar
// los datos de ejercicios a un asset bundleado (gymera_exercises.json).
// Gemini sigue usando Retrofit porque es una API HTTP real con request/response.
// ─────────────────────────────────────────────────────────────────────────────
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // OkHttpClient compartido con timeouts generosos para Gemini.
    // 60 segundos de read timeout porque la generación de rutinas puede tardar
    // varios segundos dependiendo de la carga del servidor de Google.
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    // ── Gemini API (Retrofit) ─────────────────────────────────────────────────
    // Único Retrofit que queda en el proyecto tras eliminar FreeExerciseDbRetrofit.
    // Se usa exclusivamente para POST a Gemini y generar rutinas personalizadas.
    //
    // Por qué se mantiene Retrofit (y no fetch directo con OkHttp):
    //   - Convierte automáticamente GeminiRequest → JSON y JSON → GeminiResponse
    //   - Integra con Hilt sin boilerplate adicional
    //   - Cumple con el requisito de la materia de uso de Retrofit
    // ─────────────────────────────────────────────────────────────────────────
    @Provides @Singleton @GeminiRetrofit
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideGeminiApi(@GeminiRetrofit retrofit: Retrofit): GeminiApi =
        retrofit.create(GeminiApi::class.java)

    // ── Room Database ─────────────────────────────────────────────────────────
    // fallbackToDestructiveMigration() está como red de seguridad, pero ahora
    // también registramos MIGRATION_3_4 explícitamente para usuarios con v3
    // instalada — Room la aplica automáticamente sin borrar datos.
    // ─────────────────────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideGymeraDatabase(@ApplicationContext context: Context): GymeraDatabase =
        Room.databaseBuilder(
            context,
            GymeraDatabase::class.java,
            "gymera_database"
        )
            .addMigrations(GymeraDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideRoutineDao(db: GymeraDatabase): RoutineDao = db.routineDao()

    @Provides @Singleton
    fun provideExerciseCacheDao(db: GymeraDatabase): ExerciseCacheDao = db.exerciseCacheDao()

    @Provides @Singleton
    fun provideUserProfileDao(db: GymeraDatabase): UserProfileDao = db.userProfileDao()
}