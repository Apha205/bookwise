package com.bookwise.data.repository

import com.bookwise.domain.model.ReadingProgress

class ReadingRepository(
    private val bookRepository: BookRepository
) {
    suspend fun getProgress(bookId: String): ReadingProgress? = bookRepository.getProgress(bookId)

    suspend fun saveProgress(progress: ReadingProgress) {
        require(progress.bookId.isNotBlank()) { "bookId must not be blank" }
        bookRepository.saveProgress(progress)
    }
}
