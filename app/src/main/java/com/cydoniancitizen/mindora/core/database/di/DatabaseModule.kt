package com.cydoniancitizen.mindora.core.database.di

import android.content.Context
import androidx.room.Room
import com.cydoniancitizen.mindora.core.database.MindoraDatabase
import com.cydoniancitizen.mindora.core.database.dao.MindfulnessSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "mindora.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MindoraDatabase = Room.databaseBuilder(
        context,
        MindoraDatabase::class.java,
        DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideMindfulnessSessionDao(
        database: MindoraDatabase,
    ): MindfulnessSessionDao = database.mindfulnessSessionDao()
}
