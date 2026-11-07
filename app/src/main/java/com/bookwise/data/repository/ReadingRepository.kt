package com.bookwise.data.repository

import com.bookwise.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

class ReadingRepository(
    private val bookRepository: BookRepository
) {
    fun observeAllProgress(): Flow<List<ReadingProgress>> = bookRepository.observeAllProgress()

    suspend fun getProgress(bookId: String): ReadingProgress? = bookRepository.getProgress(bookId)

    suspend fun saveProgress(progress: ReadingProgress) {
        require(progress.bookId.isNotBlank()) { "bookId must not be blank" }
        bookRepository.saveProgress(progress)
    }
}
