package com.bookwise.domain.model

data class ReadingProgress(
    val bookId: String,
    val locatorJson: String?,
    val progression: Float?,
    val chapterHref: String?,
    val chapterTitle: String?,
    val totalProgress: Double?,
    val updatedAtMillis: Long
)
