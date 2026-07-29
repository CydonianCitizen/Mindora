package com.cydoniancitizen.mindora.core.session.di

import com.cydoniancitizen.mindora.core.session.AndroidSessionTimeSource
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.data.RoomMindfulnessSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindMindfulnessSessionRepository(
        repository: RoomMindfulnessSessionRepository,
    ): MindfulnessSessionRepository

    @Binds
    abstract fun bindSessionTimeSource(
        timeSource: AndroidSessionTimeSource,
    ): SessionTimeSource
}
