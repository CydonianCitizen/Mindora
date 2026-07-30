package com.cydoniancitizen.mindora.core.reminder.di

import com.cydoniancitizen.mindora.core.reminder.AlarmManagerMindfulnessReminderScheduler
import com.cydoniancitizen.mindora.core.reminder.MindfulnessReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    @Singleton
    abstract fun bindMindfulnessReminderScheduler(
        scheduler: AlarmManagerMindfulnessReminderScheduler,
    ): MindfulnessReminderScheduler
}
