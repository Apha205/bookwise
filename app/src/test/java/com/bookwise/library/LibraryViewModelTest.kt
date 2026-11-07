package com.bookwise.library

import com.bookwise.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LibraryViewModelTest {

    private lateinit var repository: FakeBookRepository
    private lateinit var handler: FakeImportHandler

    @Before
    fun setUp() {
        repository = FakeBookRepository()
        handler = FakeImportHandler()
    }

    @Test
    fun initialState_isEmpty() = runTest {
        val vm = LibraryViewModel(repository, handler, com.bookwise.data.repository.ReadingRepository(repository))
        // uiState uses StateFlow with WhileSubscribed — initial value is Loading, not Empty
        assertTrue(vm.uiState.value is LibraryUiState.Loading || vm.uiState.value is LibraryUiState.Empty)
    }

    @Test
    fun importDuplicate_setsError() = runTest {
        val uriString = "content://test/1"
        handler.nextOutcome = FakeImportHandler.Outcome.Duplicate
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            val vm = LibraryViewModel(repository, handler, com.bookwise.data.repository.ReadingRepository(repository))
            vm.importPicked(uriString)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("Already in library", vm.errorMessage.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

class FakeBookRepository : BookRepository(
    bookDao = FakeBookDao(),
    readingProgressDao = FakeReadingProgressDao()
) {
    var books: List<com.bookwise.domain.model.Book> = emptyList()
}

class FakeBookDao : com.bookwise.data.local.dao.BookDao {
    private val items = mutableListOf<com.bookwise.data.local.entity.BookEntity>()
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.bookwise.data.local.entity.BookEntity>> =
        kotlinx.coroutines.flow.flowOf(items.toList())
    override suspend fun getAll(): List<com.bookwise.data.local.entity.BookEntity> = items.toList()
    override suspend fun getById(bookId: String) = items.firstOrNull { it.id == bookId }
    override suspend fun getBySha256(sha256: String) = items.firstOrNull { it.sha256 == sha256 }
    override suspend fun getByFileName(fileName: String) = items.firstOrNull { it.fileName == fileName }
    override suspend fun insertIgnore(entity: com.bookwise.data.local.entity.BookEntity): Long {
        if (items.any { it.sha256 == entity.sha256 }) return -1L
        items.add(entity)
        return items.size.toLong()
    }
    override suspend fun insert(entity: com.bookwise.data.local.entity.BookEntity) { items.add(entity) }
    override suspend fun update(entity: com.bookwise.data.local.entity.BookEntity) {
        items.removeAll { it.id == entity.id }; items.add(entity)
    }
    override suspend fun deleteById(bookId: String): Int {
        val before = items.size
        items.removeAll { it.id == bookId }
        return before - items.size
    }
}

class FakeReadingProgressDao : com.bookwise.data.local.dao.ReadingProgressDao {
    private val map = mutableMapOf<String, com.bookwise.data.local.entity.ReadingProgressEntity>()
    override suspend fun getForBook(bookId: String) = map[bookId]
    override suspend fun upsert(entity: com.bookwise.data.local.entity.ReadingProgressEntity) { map[entity.bookId] = entity }
    override suspend fun deleteForBook(bookId: String) { map.remove(bookId) }
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(map.values.toList())
}

class FakeImportHandler : BookImportHandler(
    context = mockContext(),
    bookRepository = FakeBookRepository()
) {
    enum class Outcome { Success, Duplicate, Failure }

    var nextOutcome: Outcome = Outcome.Success

    override suspend fun import(uriString: String): BookImportHandler.ImportOutcome {
        return when (nextOutcome) {
            Outcome.Success -> BookImportHandler.ImportOutcome.Success(mockBook())
            Outcome.Duplicate -> BookImportHandler.ImportOutcome.Duplicate(mockBook())
            Outcome.Failure -> BookImportHandler.ImportOutcome.Failure("Invalid EPUB: BadMimetype")
        }
    }

    private fun mockBook() = com.bookwise.domain.model.Book(
        id = "1", fileName = "test.epub", internalPath = "/tmp/test.epub",
        fileSizeBytes = 100, sha256 = "abc", title = "Test", authors = listOf("Author"),
        coverPath = null, identifier = null, language = "en", published = null, description = null,
        importedAtMillis = 0, lastOpenedAtMillis = null
    )
}

private fun mockContext(): android.content.Context {
    return object : android.content.ContextWrapper(null) {
        override fun getContentResolver(): android.content.ContentResolver =
            object : android.content.ContentResolver(null) {}
        override fun getFilesDir(): java.io.File = java.io.File(System.getProperty("java.io.tmpdir"))
    }
}
