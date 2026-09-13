package com.cydoniancitizen.mindora.feature.library

import androidx.annotation.StringRes
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.content.model.MeditationLevel
import com.cydoniancitizen.mindora.core.content.model.MeditationTerm

data class MeditationCardUiModel(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val goal: String,
    val durationMinutes: Int,
    val level: MeditationLevel,
)

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Error : LibraryUiState

    /**
     * One selector, on the category. A library this size is read by scrolling it, so filtering by
     * goal, duration and level only added rows of chips above a list short enough to see whole.
     * Those three still describe each meditation on its card and on its page.
     *
     * [meditations] is already filtered; [categories] is not, so every choice stays reachable.
     */
    data class Content(
        val meditations: List<MeditationCardUiModel>,
        val categories: List<MeditationTerm>,
        val selectedCategoryId: String? = null,
    ) : LibraryUiState
}

@get:StringRes
val MeditationLevel.labelResId: Int
    get() = when (this) {
        MeditationLevel.BEGINNER -> R.string.meditation_level_beginner
        MeditationLevel.INTERMEDIATE -> R.string.meditation_level_intermediate
        MeditationLevel.ADVANCED -> R.string.meditation_level_advanced
    }
