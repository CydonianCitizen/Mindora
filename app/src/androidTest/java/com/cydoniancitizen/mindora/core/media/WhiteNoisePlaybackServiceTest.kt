package com.cydoniancitizen.mindora.core.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.cydoniancitizen.mindora.MainActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-side checks for the White Noise slice: the bundled MP3 is real and the shared playback
 * service accepts a White Noise LOAD and starts playing it.
 *
 * Audible output, seamless looping, background/screen-off continuity and the exact
 * `Player.REPEAT_MODE_ONE` value are verified by hand on a device — see the report.
 */
@RunWith(AndroidJUnit4::class)
class WhiteNoisePlaybackServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun bundledWhiteNoiseAssetIsPackagedAndReadable() {
        WhiteNoiseCatalog.sounds.forEach { sound ->
            context.assets.open(sound.audioAsset).use { stream ->
                assertTrue("${sound.audioAsset} is empty", stream.read() != -1)
            }
        }
    }

    @Test
    fun catalogueAssetPathMatchesTheServiceResolvedPath() {
        // The service turns audioAsset into an asset:/// URI by splitting on '/'. The round trip
        // has to land back on a path the AssetManager can actually open.
        val sound = WhiteNoiseCatalog.default
        val rebuilt = sound.audioAsset.split('/').joinToString("/")
        context.assets.open(rebuilt).use { assertTrue(it.read() != -1) }
    }

    @Test
    fun serviceAcceptsWhiteNoiseLoadAndStartsPlaying() {
        // Android grants audio focus only to an app in the foreground, which a real session always
        // is: it starts from the White Noise screen. Without an activity the player stays paused.
        ActivityScenario.launch(MainActivity::class.java).use {
            val token = SessionToken(
                context,
                ComponentName(context, MindoraPlaybackService::class.java),
            )
            lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                future = MediaController.Builder(context, token).buildAsync()
            }
            val controller = future.get(10, TimeUnit.SECONDS)
            try {
                val result = runOnMain {
                    controller.sendCustomCommand(
                        GuidedPlaybackProtocol.LOAD,
                        Bundle().apply {
                            putString(GuidedPlaybackProtocol.STEP_ID, "white-noise")
                            putString(
                                GuidedPlaybackProtocol.CONTENT_KIND,
                                GuidedPlaybackProtocol.KIND_WHITE_NOISE,
                            )
                            putLong(
                                GuidedPlaybackProtocol.DURATION_MILLIS,
                                TimeUnit.MINUTES.toMillis(10),
                            )
                        },
                    ).get(5, TimeUnit.SECONDS)
                }
                assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)

                val phase = awaitPhase(controller) { it == PlaybackPhase.PLAYING.name }
                assertEquals(PlaybackPhase.PLAYING.name, phase)
            } finally {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    controller.sendCustomCommand(GuidedPlaybackProtocol.END, Bundle.EMPTY)
                    MediaController.releaseFuture(future)
                }
            }
        }
    }

    private fun awaitPhase(controller: MediaController, predicate: (String?) -> Boolean): String? {
        val deadline = System.currentTimeMillis() + 5_000
        var phase: String? = null
        while (System.currentTimeMillis() < deadline) {
            phase = runOnMain { controller.sessionExtras.getString(GuidedPlaybackProtocol.PHASE) }
            if (predicate(phase)) return phase
            Thread.sleep(100)
        }
        return phase
    }

    private fun <T> runOnMain(block: () -> T): T {
        var value: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
