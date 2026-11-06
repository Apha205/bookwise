package com.bookwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bookwise.data.local.dao.BookDao
import com.bookwise.data.local.dao.ExplanationDao
import com.bookwise.data.local.dao.ReadingProgressDao
import com.bookwise.data.local.entity.ExplanationEntity
import com.bookwise.data.local.entity.BookEntity
import com.bookwise.data.local.entity.ReadingProgressEntity

@androidx.room.Database(
    entities = [BookEntity::class, ReadingProgressEntity::class, ExplanationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BookwiseDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        const val DB_NAME = "bookwise.db"

        @Volatile
        private var INSTANCE: BookwiseDatabase? = null

        fun getInstance(context: Context): BookwiseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookwiseDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
