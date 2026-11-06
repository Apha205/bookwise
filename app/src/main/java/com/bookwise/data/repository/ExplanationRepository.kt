package com.bookwise.data.repository

import com.bookwise.data.local.dao.ExplanationDao
import com.bookwise.data.local.entity.ExplanationEntity
import com.bookwise.domain.model.Explanation
import com.bookwise.domain.model.TutorAction
import java.util.UUID

interface ExplanationRepository {
    suspend fun getForBook(bookId: String): List<Explanation>
    suspend fun getById(id: String): Explanation?
    suspend fun save(
        bookId: String,
        locatorJson: String?,
        selectedText: String,
        action: TutorAction,
        prompt: String?,
        response: String
    ): String
    suspend fun delete(id: String)
    suspend fun deleteForBook(bookId: String)
}

class ExplanationRepositoryImpl(private val dao: ExplanationDao) : ExplanationRepository {
    override suspend fun getForBook(bookId: String): List<Explanation> =
        dao.getForBook(bookId).map { it.toDomain() }

    override suspend fun getById(id: String): Explanation? =
        dao.getById(id)?.toDomain()

    override suspend fun save(
        bookId: String,
        locatorJson: String?,
        selectedText: String,
        action: TutorAction,
        prompt: String?,
        response: String
    ): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            ExplanationEntity(
                id = id,
                bookId = bookId,
                locatorJson = locatorJson,
                selectedText = selectedText,
                action = action.name,
                prompt = prompt,
                response = response,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        return id
    }

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun deleteForBook(bookId: String) = dao.deleteForBook(bookId)

    private fun ExplanationEntity.toDomain() = Explanation(
        id = id,
        bookId = bookId,
        locatorJson = locatorJson,
        selectedText = selectedText,
        action = TutorAction.valueOf(action),
        prompt = prompt,
        response = response,
        createdAtMillis = createdAtMillis
    )
}
