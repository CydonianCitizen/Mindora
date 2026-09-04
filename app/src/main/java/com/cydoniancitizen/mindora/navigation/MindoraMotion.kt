package com.cydoniancitizen.mindora.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import kotlin.math.roundToInt

/**
 * Material 3 motion for navigation.
 *
 * Three patterns, each carrying a different meaning: fade through between the bottom bar's peer
 * destinations, shared axis when moving down a hierarchy, and container transform when a card on
 * the practice screen becomes the screen it opens.
 */

// Material's asymmetric fade: the outgoing content leaves quickly, the incoming one takes its
// time, so the two never overlap at full opacity.
private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210
private const val AXIS_MILLIS = 300
private const val CONTAINER_MILLIS = 450

/** How far a screen travels along the shared axis, as a fraction of the window width. */
private const val AXIS_TRAVEL = 0.06f

/** Material's emphasized easing: a decisive start that settles softly. */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** True when both sides of the transition are bottom bar destinations, i.e. peers. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.isPeerSwitch(): Boolean {
    val routes = MindoraDestination.entries.map { it.route }
    return initialState.destination.route in routes && targetState.destination.route in routes
}

internal fun fadeThroughEnter(): EnterTransition =
    fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing)) +
        scaleIn(
            animationSpec = tween(
                durationMillis = FADE_IN_MILLIS,
                delayMillis = FADE_OUT_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
            initialScale = 0.92f,
        )

internal fun fadeThroughExit(): ExitTransition =
    fadeOut(tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))

internal fun sharedAxisEnter(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(AXIS_MILLIS, easing = Emphasized)) { width ->
        val travel = (width * AXIS_TRAVEL).roundToInt()
        if (forward) travel else -travel
    } + fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing))

internal fun sharedAxisExit(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(AXIS_MILLIS, easing = Emphasized)) { width ->
        val travel = (width * AXIS_TRAVEL).roundToInt()
        if (forward) -travel else travel
    } + fadeOut(tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))

/**
 * Container transform destinations only cross-fade their content: the morphing bounds carry the
 * movement, and a slide on top of them would fight it.
 */
internal fun containerEnter(): EnterTransition = fadeIn(tween(CONTAINER_MILLIS, easing = Emphasized))

internal fun containerExit(): ExitTransition = fadeOut(tween(CONTAINER_MILLIS, easing = Emphasized))

/** Keys pairing a card on the practice screen with the screen it grows into. */
internal object ContainerKeys {
    const val FREE_MEDITATION = "container:free-meditation"
    const val BREATHING_EXERCISE = "container:breathing-exercise"
}

@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

internal val LocalNavAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Tags a layout as one half of a container transform.
 *
 * Outside navigation — a preview, or a screen composable driven straight from a test — both scopes
 * are absent and this is a no-op, so screens stay callable without a navigation graph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.sharedContainer(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(CONTAINER_MILLIS, easing = Emphasized)),
            exit = fadeOut(tween(CONTAINER_MILLIS, easing = Emphasized)),
        )
    }
}
