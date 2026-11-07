package com.bookwise.data.repository

import com.bookwise.domain.model.ReadingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.test.runTest

class ReadingRepositoryTest {

    private val repo = BookRepository(FakeBookDao2(), FakeProgressDao2())
    private val readingRepo = ReadingRepository(repo)

    @Test
    fun saveAndGetProgress_roundTrips() = runTest {
        val progress = ReadingProgress(
            bookId = "book-1",
            locatorJson = """{"href":"chap1.xhtml"}""",
            progression = 0.5f,
            chapterHref = "chap1.xhtml",
            chapterTitle = "Chapter 1",
            totalProgress = 0.25,
            updatedAtMillis = 12345
        )
        readingRepo.saveProgress(progress)
        val loaded = readingRepo.getProgress("book-1")
        assertEquals(progress.bookId, loaded?.bookId)
        assertEquals(progress.progression, loaded?.progression)
        assertEquals(progress.locatorJson, loaded?.locatorJson)
    }

    @Test
    fun getProgress_unknownBook_returnsNull() = runTest {
        assertNull(readingRepo.getProgress("nope"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveProgress_blankBookId_throws() = runTest {
        readingRepo.saveProgress(ReadingProgress(" ", null, null, null, null, null, 0))
    }
}

private class FakeBookDao2 : com.bookwise.data.local.dao.BookDao {
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(emptyList<com.bookwise.data.local.entity.BookEntity>())
    override suspend fun getAll() = emptyList<com.bookwise.data.local.entity.BookEntity>()
    override suspend fun getById(bookId: String) = null
    override suspend fun getBySha256(sha256: String) = null
    override suspend fun getByFileName(fileName: String) = null
    override suspend fun insertIgnore(entity: com.bookwise.data.local.entity.BookEntity) = 1L
    override suspend fun insert(entity: com.bookwise.data.local.entity.BookEntity) {}
    override suspend fun update(entity: com.bookwise.data.local.entity.BookEntity) {}
    override suspend fun deleteById(bookId: String) = 0
}
private class FakeProgressDao2 : com.bookwise.data.local.dao.ReadingProgressDao {
    private val map = mutableMapOf<String, com.bookwise.data.local.entity.ReadingProgressEntity>()
    override suspend fun getForBook(bookId: String) = map[bookId]
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(map.values.toList())
    override suspend fun upsert(entity: com.bookwise.data.local.entity.ReadingProgressEntity) { map[entity.bookId] = entity }
    override suspend fun deleteForBook(bookId: String) { map.remove(bookId) }
}
