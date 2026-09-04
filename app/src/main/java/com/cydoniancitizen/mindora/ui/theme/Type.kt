package com.cydoniancitizen.mindora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.cydoniancitizen.mindora.R

/**
 * Two faces, one voice.
 *
 * Fraunces names things — display, headline and title roles. Karla carries running text and every
 * control label. They pair because they share a skeleton: a large x-height, sturdy stems and warm
 * terminals, so moving from a card title to its description does not feel like changing document.
 *
 * Both are variable fonts, so each role gets the cut it needs out of a single file per family.
 */
@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int, opticalSize: Float, softness: Float) = FontFamily(
    Font(
        resId = R.font.fraunces,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            // Fraunces' optical size axis runs 9..144: match it to the role's point size so the
            // stroke contrast stays right instead of thinning out large or clogging up small.
            FontVariation.Setting("opsz", opticalSize),
            FontVariation.Setting("SOFT", softness),
            // The palette is botanical, not quirky, so the wonky axis stays off.
            FontVariation.Setting("WONK", 0f),
        ),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun karla(weight: Int) = FontFamily(
    Font(
        resId = R.font.karla,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    ),
)

private val FrauncesDisplay = fraunces(weight = 400, opticalSize = 72f, softness = 30f)
private val FrauncesHeadline = fraunces(weight = 600, opticalSize = 36f, softness = 26f)
private val FrauncesTitle = fraunces(weight = 600, opticalSize = 16f, softness = 22f)
private val KarlaBody = karla(weight = 400)
private val KarlaLabel = karla(weight = 500)

/**
 * For numerals that change in place, such as a countdown.
 *
 * Fraunces ships no tabular-figures feature at all and its digits differ in width by up to 30% of
 * the em — a "1" is two thirds of a "0" — so a countdown set in the display face re-flows on every
 * tick. Karla carries `tnum`, so asking for it there actually does something. The size, weight and
 * spacing of the role are kept; only the face and the figures change.
 */
fun TextStyle.tabularNumerals(): TextStyle = copy(
    fontFamily = KarlaBody,
    fontFeatureSettings = "tnum",
)

private val Default = Typography()

val MindoraTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = FrauncesDisplay),
    displayMedium = Default.displayMedium.copy(fontFamily = FrauncesDisplay),
    displaySmall = Default.displaySmall.copy(fontFamily = FrauncesDisplay),

    headlineLarge = Default.headlineLarge.copy(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineMedium = Default.headlineMedium.copy(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = Default.headlineSmall.copy(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.SemiBold,
    ),

    titleLarge = Default.titleLarge.copy(
        fontFamily = FrauncesTitle,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Default.titleMedium.copy(
        fontFamily = FrauncesTitle,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = Default.titleSmall.copy(
        fontFamily = FrauncesTitle,
        fontWeight = FontWeight.SemiBold,
    ),

    bodyLarge = Default.bodyLarge.copy(fontFamily = KarlaBody),
    bodyMedium = Default.bodyMedium.copy(fontFamily = KarlaBody),
    bodySmall = Default.bodySmall.copy(fontFamily = KarlaBody),

    labelLarge = Default.labelLarge.copy(fontFamily = KarlaLabel),
    labelMedium = Default.labelMedium.copy(fontFamily = KarlaLabel),
    labelSmall = Default.labelSmall.copy(fontFamily = KarlaLabel),
)
