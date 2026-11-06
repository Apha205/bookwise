package com.bookwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookwise.data.local.entity.ExplanationEntity

@Dao
interface ExplanationDao {
    @Query("SELECT * FROM explanations WHERE bookId = :bookId ORDER BY createdAtMillis DESC")
    suspend fun getForBook(bookId: String): List<ExplanationEntity>

    @Query("SELECT * FROM explanations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExplanationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExplanationEntity)

    @Query("DELETE FROM explanations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM explanations WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
