package com.bookwise.data.repository

import com.bookwise.data.local.dao.BookDao
import com.bookwise.data.local.dao.ReadingProgressDao
import com.bookwise.data.local.entity.BookEntity
import com.bookwise.data.local.entity.ReadingProgressEntity
import com.bookwise.domain.model.Book
import com.bookwise.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

open class BookRepository(
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao
) {
    open fun observeBooks(): Flow<List<Book>> = bookDao.observeAll().map { it.map(::toDomain) }

    suspend fun getById(bookId: String): Book? = bookDao.getById(bookId)?.let(::toDomain)

    suspend fun insert(book: Book): Boolean {
        val rowId = bookDao.insertIgnore(toEntity(book))
        return rowId != -1L
    }

    suspend fun update(book: Book) = bookDao.update(toEntity(book))

    suspend fun delete(bookId: String): Int = bookDao.deleteById(bookId)

    suspend fun getProgress(bookId: String): ReadingProgress? =
        readingProgressDao.getForBook(bookId)?.let(::toDomain)

    suspend fun saveProgress(progress: ReadingProgress) =
        readingProgressDao.upsert(toEntity(progress))

    private fun toDomain(entity: BookEntity): Book {
        val authors = try {
            val arr = JSONArray(entity.authorsJson)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
        return Book(
            id = entity.id,
            fileName = entity.fileName,
            internalPath = entity.internalPath,
            fileSizeBytes = entity.fileSizeBytes,
            sha256 = entity.sha256,
            title = entity.title,
            authors = authors,
            coverPath = entity.coverPath,
            identifier = entity.identifier,
            language = entity.language,
            published = entity.published,
            description = entity.description,
            importedAtMillis = entity.importedAtMillis,
            lastOpenedAtMillis = entity.lastOpenedAtMillis,
            isFavorite = entity.isFavorite,
            citation = entity.citation
        )
    }

    private fun toEntity(book: Book): BookEntity {
        val json = JSONArray().apply { book.authors.forEach { put(it) } }.toString()
        return BookEntity(
            id = book.id,
            fileName = book.fileName,
            internalPath = book.internalPath,
            fileSizeBytes = book.fileSizeBytes,
            sha256 = book.sha256,
            title = book.title,
            authorsJson = json,
            coverPath = book.coverPath,
            identifier = book.identifier,
            language = book.language,
            published = book.published,
            description = book.description,
            importedAtMillis = book.importedAtMillis,
            lastOpenedAtMillis = book.lastOpenedAtMillis,
            isFavorite = book.isFavorite,
            citation = book.citation
        )
    }

    private fun toDomain(entity: ReadingProgressEntity): ReadingProgress = ReadingProgress(
        bookId = entity.bookId,
        locatorJson = entity.locatorJson,
        progression = entity.progression,
        chapterHref = entity.chapterHref,
        chapterTitle = entity.chapterTitle,
        totalProgress = entity.totalProgress,
        updatedAtMillis = entity.updatedAtMillis
    )

    private fun toEntity(progress: ReadingProgress): ReadingProgressEntity = ReadingProgressEntity(
        bookId = progress.bookId,
        locatorJson = progress.locatorJson,
        progression = progress.progression,
        chapterHref = progress.chapterHref,
        chapterTitle = progress.chapterTitle,
        totalProgress = progress.totalProgress,
        updatedAtMillis = progress.updatedAtMillis
    )
}
