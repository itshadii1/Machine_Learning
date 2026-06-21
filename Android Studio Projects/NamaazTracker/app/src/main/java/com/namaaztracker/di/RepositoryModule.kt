package com.namaaztracker.di

import com.namaaztracker.data.repository.PostureRepositoryImpl
import com.namaaztracker.domain.repository.PostureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostureRepository(impl: PostureRepositoryImpl): PostureRepository
}
