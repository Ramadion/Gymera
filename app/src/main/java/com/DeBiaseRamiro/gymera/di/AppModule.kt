package com.DeBiaseRamiro.gymera.di

import android.content.Context
import com.DeBiaseRamiro.gymera.data.remote.api.GeminiApi
import com.DeBiaseRamiro.gymera.data.remote.api.GroqApi
import com.DeBiaseRamiro.gymera.data.repository.ai.FailoverRoutineGenerator
import com.DeBiaseRamiro.gymera.data.repository.ai.GeminiAIProvider
import com.DeBiaseRamiro.gymera.data.repository.ai.GroqAIProvider
import com.DeBiaseRamiro.gymera.data.repository.ai.RoutineAIProvider
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

// ── @GroqRetrofit ───────────────────────────────────────────────────────────
// Qualifier para identificar el Retrofit de Groq.
// Antes existía también @FreeExerciseDbRetrofit, pero se eliminó al pasar
// los datos de ejercicios a un asset bundleado (gymera_exercises.json).
// Groq sigue usando Retrofit porque es una API HTTP real con request/response.
// ─────────────────────────────────────────────────────────────────────────────
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class GroqRetrofit

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // OkHttpClient compartido con timeouts generosos para Groq.
    // 30 segundos de read timeout porque la generación de rutinas puede tardar
    // varios segundos aunque Groq es mucho más rápido que Gemini.
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // ── Groq API (Retrofit) ─────────────────────────────────────────────────
    // Único Retrofit que queda en el proyecto tras eliminar FreeExerciseDbRetrofit.
    // Se usa exclusivamente para POST a Groq y generar rutinas personalizadas.
    //
    // Por qué se mantiene Retrofit (y no fetch directo con OkHttp):
    //   - Convierte automáticamente GroqChatRequest → JSON y JSON → GroqChatResponse
    //   - Integra con Hilt sin boilerplate adicional
    //   - Cumple con el requisito de la materia de uso de Retrofit
    // ─────────────────────────────────────────────────────────────────────────
    @Provides @Singleton @GroqRetrofit
    fun provideGroqRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideGroqApi(@GroqRetrofit retrofit: Retrofit): GroqApi =
        retrofit.create(GroqApi::class.java)

    // ── Gemini API (Retrofit) ───────────────────────────────────────────────
    // Segundo proveedor de IA para el fallback multicanal: si Groq se queda
    // sin cupo (rate limit), la app salta a Gemini automáticamente.
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

    // ── Fallback multicanal de IA ──────────────────────────────────────────
    // Cadena de proveedores gratis en orden de prioridad. Si uno falla
    // (rate limit 429, timeout o JSON inválido), FailoverRoutineGenerator
    // salta automáticamente al siguiente.
    // ─────────────────────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideRoutineAIProviders(groqApi: GroqApi, geminiApi: GeminiApi): List<RoutineAIProvider> =
        listOf(
            GroqAIProvider(groqApi, modelId = "openai/gpt-oss-120b",          maxTokens = 5000),
            GroqAIProvider(groqApi, modelId = "llama-3.3-70b-versatile",      maxTokens = 5000),
            GeminiAIProvider(geminiApi),
            GroqAIProvider(groqApi, modelId = "openai/gpt-oss-20b",           maxTokens = 5000),
            GroqAIProvider(groqApi, modelId = "llama-3.1-8b-instant",         maxTokens = 4000)
        )

    @Provides @Singleton
    fun provideFailoverRoutineGenerator(providers: List<@JvmSuppressWildcards RoutineAIProvider>): FailoverRoutineGenerator =
        FailoverRoutineGenerator(providers)

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