package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.LibraryMeditation

/**
 * Reads the standalone meditation library.
 *
 * Separate from [MindfulnessContentRepository] because the library and the ordered paths are read
 * by different screens: the library feature has no reason to depend on paths, or the reverse.
 */
interface MeditationLibraryRepository {
    /** In catalogue order. */
    suspend fun getMeditations(): List<LibraryMeditation>
}

fun List<LibraryMeditation>.findMeditation(meditationId: String): LibraryMeditation? =
    firstOrNull { it.id == meditationId }
