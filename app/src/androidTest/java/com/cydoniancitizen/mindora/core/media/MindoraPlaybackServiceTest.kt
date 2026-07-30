package com.cydoniancitizen.mindora.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindoraPlaybackServiceTest {
    @Test
    fun applicationControllersShareOneRestrictedMediaSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = SessionToken(
            context,
            ComponentName(context, MindoraPlaybackService::class.java),
        )
        lateinit var firstFuture: ListenableFuture<MediaController>
        lateinit var secondFuture: ListenableFuture<MediaController>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            firstFuture = MediaController.Builder(context, token).buildAsync()
            secondFuture = MediaController.Builder(context, token).buildAsync()
        }

        try {
            val first = firstFuture.get(10, TimeUnit.SECONDS)
            val second = secondFuture.get(10, TimeUnit.SECONDS)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(first.isConnected)
                assertTrue(second.isConnected)
                assertEquals(first.connectedToken, second.connectedToken)
                assertTrue(first.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
                assertTrue(first.isCommandAvailable(Player.COMMAND_GET_METADATA))
                assertFalse(first.isCommandAvailable(Player.COMMAND_STOP))
                assertFalse(first.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
                assertFalse(first.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
                assertFalse(first.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM))
                GuidedPlaybackProtocol.applicationCommands.forEach {
                    assertTrue(first.isSessionCommandAvailable(it))
                }
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                MediaController.releaseFuture(firstFuture)
                MediaController.releaseFuture(secondFuture)
            }
        }
    }
}
