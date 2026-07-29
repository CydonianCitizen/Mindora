package com.cydoniancitizen.mindora.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class GuidedPlaybackModule {
    @Binds
    abstract fun bindGuidedMeditationPlayback(
        connection: GuidedPlaybackConnection,
    ): GuidedMeditationPlayback
}
