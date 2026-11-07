package com.bookwise.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS explanations (
                    id TEXT NOT NULL PRIMARY KEY,
                    bookId TEXT NOT NULL,
                    locatorJson TEXT,
                    selectedText TEXT NOT NULL,
                    action TEXT NOT NULL,
                    prompt TEXT,
                    response TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_explanations_bookId ON explanations(bookId)")
        }
    }
}
