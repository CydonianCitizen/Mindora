package com.cydoniancitizen.mindora.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlin.coroutines.coroutineContext

/**
 * Eases like a breath: still at both turns, quickest in between.
 *
 * The zero slope at each end is what lets an animation start without anyone noticing. Leaving the
 * resting value at zero velocity means there is no frame where motion visibly begins.
 */
internal val BreathEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * Whether the system is animating at all right now.
 *
 * Only a duration scale of exactly zero means "Remove animations". Any fraction in between means
 * the user wants animations *faster*, not gone, so treating 0.5x as "off" would silently strip
 * motion from someone who asked for more of it.
 *
 * The value is re-read on resume, because the setting is changed in system settings — that is, from
 * outside the app, while it is in the background.
 */
@Composable
internal fun systemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(animatorDurationScale(context) != 0f) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = animatorDurationScale(context) != 0f
    }
    return enabled
}

private fun animatorDurationScale(context: Context): Float = Settings.Global.getFloat(
    context.contentResolver,
    Settings.Global.ANIMATOR_DURATION_SCALE,
    1f,
)

/**
 * Whether motion that never settles is allowed here.
 *
 * Compose's test framework installs an [InfiniteAnimationPolicy] so a test waiting for the UI to go
 * idle is not blocked forever by an animation that, by design, never finishes. Anything that
 * animates for as long as its screen is open has to ask before starting, and cut straight to its
 * target when the answer is no — which also keeps the value a test reads deterministic.
 */
internal suspend fun endlessMotionAllowed(): Boolean =
    coroutineContext[InfiniteAnimationPolicy] == null

/**
 * A radial gradient that dissolves into whatever sits behind it.
 *
 * Every stop is the same colour at a different alpha, never [Color.Transparent]: transparent is
 * transparent *black*, so interpolating towards it drags the mid-tones through grey and leaves a
 * dirty halo exactly where the edge is supposed to disappear.
 *
 * @param stops radius fraction to alpha, from the centre out.
 */
internal fun softGlow(color: Color, vararg stops: Pair<Float, Float>): Brush =
    Brush.radialGradient(
        *stops.map { (position, alpha) -> position to color.copy(alpha = alpha) }.toTypedArray(),
    )
