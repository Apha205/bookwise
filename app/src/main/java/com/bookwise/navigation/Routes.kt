package com.bookwise.navigation

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader"
    const val READER_ARG_BOOK_ID = "bookId"
    const val SETTINGS = "settings"

    fun reader(bookId: String): String = "$READER/$bookId"
}
