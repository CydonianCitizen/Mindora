package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.content.model.MindfulnessStep
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class LocatedMindfulnessStep(
    val path: MindfulnessPath,
    val step: MindfulnessStep,
)

fun List<MindfulnessPath>.findPath(pathId: String): MindfulnessPath? =
    firstOrNull { it.id == pathId }

fun List<MindfulnessPath>.findStep(stepId: String): LocatedMindfulnessStep? {
    forEach { path ->
        path.steps.firstOrNull { it.id == stepId }?.let { step ->
            return LocatedMindfulnessStep(path, step)
        }
    }
    return null
}

fun decodeContentRouteId(value: String): String? = try {
    URLDecoder.decode(
        value.replace("+", "%2B"),
        StandardCharsets.UTF_8,
    ).takeIf(String::isNotBlank)
} catch (_: IllegalArgumentException) {
    null
}
