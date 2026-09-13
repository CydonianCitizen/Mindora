package com.cydoniancitizen.mindora.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The one set of measurements the breathing exercise and the free meditation are both built on.
 *
 * They are fixed rather than proportional on purpose. Every band that could grow with its content
 * would move the disc with it, and the disc has to stand in the same place in every state of both
 * screens: choosing a duration, running, paused. Change a number here and both screens move
 * together — that is the whole reason it is one object and not two screens' worth of padding.
 */
internal object SessionStageMetrics {
    val HorizontalPadding = 24.dp
    val BottomPadding = 24.dp

    /** The gap above the first line of text. Small: the practice belongs high on the screen. */
    val TopGap = 8.dp

    /**
     * The fixed slot the heading band lives in, bottom-aligned inside it and scrolling if the text
     * outgrows it. Fixed because a two-line title and a one-line status have to leave the disc at
     * the same height.
     */
    val HeaderHeight = 128.dp

    /** Breathing room on each side of the disc. */
    val AroundCircle = 20.dp

    /** Text stops widening here, so a line stays readable on a wide screen. */
    val ContentWidth = 480.dp

    /** The disc the breathing exercise scales between [MIN_CIRCLE] and this. */
    val BreathingCircle = 220.dp
    val MinBreathingCircle = 128.dp

    /** The ring the free meditation breathes, with its countdown inside it. */
    val MeditationRing = 320.dp

    /** Where the circle slot starts, for anything drawn behind the stage rather than inside it. */
    val CircleTop = TopGap + HeaderHeight + AroundCircle
}

/**
 * The skeleton every session screen is laid out on: a heading band, the circle, then everything
 * else, anchored to the top of the screen rather than centred in it.
 *
 * The circle sits at a height that depends on nothing but [SessionStageMetrics], so moving between
 * setup and a running session never moves it. Both bands scroll on their own, so a large font scale
 * loses nothing.
 */
@Composable
internal fun SessionStage(
    circle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    above: @Composable ColumnScope.() -> Unit,
    below: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = SessionStageMetrics.HorizontalPadding,
                end = SessionStageMetrics.HorizontalPadding,
                top = SessionStageMetrics.TopGap,
                bottom = SessionStageMetrics.BottomPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .height(SessionStageMetrics.HeaderHeight)
                .widthIn(max = SessionStageMetrics.ContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            content = above,
        )
        Spacer(modifier = Modifier.height(SessionStageMetrics.AroundCircle))
        circle()
        Spacer(modifier = Modifier.height(SessionStageMetrics.AroundCircle))
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = SessionStageMetrics.ContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            content = below,
        )
    }
}
