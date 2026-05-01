package com.DeBiaseRamiro.gymera.di

import com.DeBiaseRamiro.gymera.data.repository.AuthRepositoryImpl
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Este módulo es ABSTRACTO porque usa @Binds (no puede tener @Provides)
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}