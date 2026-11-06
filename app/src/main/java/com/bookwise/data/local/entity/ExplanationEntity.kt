package com.bookwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "explanations",
    indices = [Index(value = ["bookId"])]
)
data class ExplanationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val locatorJson: String?,
    val selectedText: String,
    val action: String,
    val prompt: String?,
    val response: String,
    val createdAtMillis: Long
)
