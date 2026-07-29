package com.cydoniancitizen.mindora.core.content.di

import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.data.BundledMindfulnessContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {
    @Binds
    abstract fun bindMindfulnessContentRepository(
        repository: BundledMindfulnessContentRepository,
    ): MindfulnessContentRepository
}
