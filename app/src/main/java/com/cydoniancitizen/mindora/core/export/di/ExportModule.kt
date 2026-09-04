package com.cydoniancitizen.mindora.core.export.di

import com.cydoniancitizen.mindora.core.export.ContentResolverDataExporter
import com.cydoniancitizen.mindora.core.export.MindoraDataExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    @Binds
    abstract fun bindMindoraDataExporter(
        exporter: ContentResolverDataExporter,
    ): MindoraDataExporter
}
