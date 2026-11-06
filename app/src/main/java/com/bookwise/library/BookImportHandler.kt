package com.bookwise.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bookwise.data.local.BookwiseDatabase
import com.bookwise.data.repository.BookRepository
import com.bookwise.domain.model.Book
import com.bookwise.library.epub.EpubValidator
import com.bookwise.library.epub.ImportResult
import java.io.File
import java.util.UUID

open class BookImportHandler(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val validator: EpubValidator = EpubValidator()
) {
    sealed interface ImportOutcome {
        data class Success(val book: Book) : ImportOutcome
        data class Duplicate(val book: Book) : ImportOutcome
        data class Failure(val message: String) : ImportOutcome
    }

    open suspend fun import(uriString: String): ImportOutcome {
        if (uriString.isBlank()) return ImportOutcome.Failure("No file selected")
        val uri = android.net.Uri.parse(uriString)
        val displayName = queryDisplayName(uri) ?: "unknown.epub"
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        val input = try {
            context.contentResolver.openInputStream(uri) ?: return ImportOutcome.Failure("Could not open file")
        } catch (e: Exception) {
            return ImportOutcome.Failure("Could not open file: ${e.message}")
        }

        val result = input.use { validator.validate(it) }
        when (result) {
            is ImportResult.Failure -> return ImportOutcome.Failure("Invalid EPUB: ${result.reason.name}")
            is ImportResult.Success -> Unit
        }

        val success = (result as ImportResult.Success).validated
        try {
            val bookId = UUID.randomUUID().toString()
            val internalFile = File(booksDir, "$bookId.epub")
            context.contentResolver.openInputStream(uri)?.use { ins ->
                internalFile.outputStream().use { out -> ins.copyTo(out) }
            } ?: return ImportOutcome.Failure("Could not copy file")

            var coverPath: String? = null
            success.metadata.coverBytes?.let { bytes ->
                val ext = when (success.metadata.coverMimeType) {
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val coverFile = File(coversDir, "$bookId.$ext")
                coverFile.writeBytes(bytes)
                coverPath = coverFile.absolutePath
            }

            val book = Book(
                id = bookId,
                fileName = displayName,
                internalPath = internalFile.absolutePath,
                fileSizeBytes = success.fileSizeBytes,
                sha256 = success.sha256,
                title = success.metadata.title ?: displayName.removeSuffix(".epub"),
                authors = success.metadata.authors,
                coverPath = coverPath,
                identifier = success.metadata.identifier,
                language = success.metadata.language,
                published = success.metadata.published,
                description = success.metadata.description,
                importedAtMillis = System.currentTimeMillis(),
                lastOpenedAtMillis = null,
                isFavorite = false,
                citation = null
            )
            val inserted = bookRepository.insert(book)
            if (!inserted) {
                internalFile.delete()
                return ImportOutcome.Failure("Already in library")
            }
            return ImportOutcome.Success(book)
        } catch (e: Exception) {
            return ImportOutcome.Failure("Import failed: ${e.message}")
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun create(context: Context): BookImportHandler {
            val db = BookwiseDatabase.getInstance(context)
            val repo = BookRepository(db.bookDao(), db.readingProgressDao())
            return BookImportHandler(context, repo)
        }
    }
}
