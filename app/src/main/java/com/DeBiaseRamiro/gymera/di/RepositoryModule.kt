package com.DeBiaseRamiro.gymera.di

import com.DeBiaseRamiro.gymera.data.repository.AuthRepositoryImpl
import com.DeBiaseRamiro.gymera.data.repository.RoutineRepositoryImpl
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import com.DeBiaseRamiro.gymera.domain.repository.RoutineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.DeBiaseRamiro.gymera.data.repository.FirestoreRepositoryImpl
import com.DeBiaseRamiro.gymera.domain.repository.FirestoreRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        impl: RoutineRepositoryImpl
    ): RoutineRepository

    @Binds @Singleton
    abstract fun bindFirestoreRepository(impl: FirestoreRepositoryImpl): FirestoreRepository
}