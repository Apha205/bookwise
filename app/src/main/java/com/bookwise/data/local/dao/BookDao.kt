package com.bookwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookwise.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastOpenedAtMillis DESC, importedAtMillis DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastOpenedAtMillis DESC, importedAtMillis DESC")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE sha256 = :sha256 LIMIT 1")
    suspend fun getBySha256(sha256: String): BookEntity?

    @Query("SELECT * FROM books WHERE fileName = :fileName LIMIT 1")
    suspend fun getByFileName(fileName: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: BookEntity): Long

    @Insert
    suspend fun insert(entity: BookEntity)

    @Update
    suspend fun update(entity: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: String): Int
}
