package com.cydoniancitizen.mindora.core.preferences.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.data.DataStoreMindoraPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindMindoraPreferencesRepository(
        repository: DataStoreMindoraPreferencesRepository,
    ): MindoraPreferencesRepository

    companion object {
        private const val PREFERENCES_FILE_NAME = "mindora_preferences"

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) },
        )
    }
}
