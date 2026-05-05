package com.DeBiaseRamiro.gymera.di

import com.DeBiaseRamiro.gymera.data.remote.api.FreeExerciseDbApi
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

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class FreeExerciseDbRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    // ── Gemini ────────────────────────────────────────────────────────────────

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

    // ── Free Exercise DB (GitHub) ─────────────────────────────────────────────

    @Provides @Singleton @FreeExerciseDbRetrofit
    fun provideFreeExerciseDbRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideFreeExerciseDbApi(@FreeExerciseDbRetrofit retrofit: Retrofit): FreeExerciseDbApi =
        retrofit.create(FreeExerciseDbApi::class.java)
}