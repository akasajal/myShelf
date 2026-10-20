package com.myshelf.app.core.model

data class Season(
    val number: Int,
    val progress: Progress = Progress.PLANNED,
    val rating: Int = 0,
    val watched: Int = 0,
    val episodes: Int = 0,
    val name: String = ""
)
