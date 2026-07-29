package com.cydoniancitizen.mindora.core.content.model

data class MindfulnessPath(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<MindfulnessStep>,
)
