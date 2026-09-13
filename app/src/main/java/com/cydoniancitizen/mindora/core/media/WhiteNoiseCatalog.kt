package com.cydoniancitizen.mindora.core.media

import androidx.annotation.StringRes
import com.cydoniancitizen.mindora.R

/**
 * One ambient sound the user can play as a standalone White Noise session.
 *
 * [audioAsset] is a path relative to `src/main/assets`, exactly like a guided meditation step's
 * audio: the same asset-resolution code in MindoraPlaybackService turns it into an `asset:///` URI
 * and opens it to verify it is packaged.
 */
data class WhiteNoiseSound(
    val id: String,
    @param:StringRes val titleResId: Int,
    val audioAsset: String,
)

/**
 * The bundled ambient sounds, in display order.
 *
 * A flat in-code list on purpose: there is one sound today and no reason for a repository or a
 * content file yet. Adding `brown_noise.mp3`, `pink_noise.mp3` or `rain.mp3` later is one entry
 * each here and one asset file — the playback service does not change per sound.
 */
object WhiteNoiseCatalog {
    val sounds: List<WhiteNoiseSound> = listOf(
        WhiteNoiseSound(
            id = "white-noise",
            titleResId = R.string.white_noise,
            audioAsset = "audio/white_noise.mp3",
        ),
    )

    val default: WhiteNoiseSound get() = sounds.first()

    fun find(id: String): WhiteNoiseSound? = sounds.firstOrNull { it.id == id }
}
