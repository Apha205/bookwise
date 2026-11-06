package com.bookwise.domain.model

data class Book(
    val id: String,
    val fileName: String,
    val internalPath: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val title: String,
    val authors: List<String>,
    val coverPath: String?,
    val identifier: String?,
    val language: String?,
    val published: String?,
    val description: String?,
    val importedAtMillis: Long,
    val lastOpenedAtMillis: Long?,
    val isFavorite: Boolean = false,
    val citation: String? = null
)
