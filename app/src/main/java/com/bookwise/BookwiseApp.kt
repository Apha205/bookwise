package com.bookwise

import android.app.Application
import com.bookwise.data.local.BookwiseDatabase
import com.bookwise.data.preferences.AppPreferences
import com.bookwise.data.preferences.SettingsRepository
import com.bookwise.data.repository.BookRepository
import com.bookwise.data.repository.ReadingRepository
import com.bookwise.library.BookImportHandler
import com.bookwise.library.epub.EpubValidator
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class BookwiseApp : Application() {
    lateinit var bookRepository: BookRepository
        private set
    lateinit var readingRepository: ReadingRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var epubValidator: EpubValidator
        private set
    lateinit var importHandler: BookImportHandler
        private set
    val defaultHttpClient: DefaultHttpClient by lazy { DefaultHttpClient() }
    val assetRetriever: AssetRetriever by lazy {
        AssetRetriever(contentResolver, defaultHttpClient)
    }
    val publicationOpener: PublicationOpener by lazy {
        PublicationOpener(
            DefaultPublicationParser(
                context = this,
                httpClient = defaultHttpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
    }

    override fun onCreate() {
        super.onCreate()
        val database = BookwiseDatabase.getInstance(this)
        bookRepository = BookRepository(database.bookDao(), database.readingProgressDao())
        readingRepository = ReadingRepository(bookRepository)
        settingsRepository = SettingsRepository(AppPreferences(this))
        epubValidator = EpubValidator()
        importHandler = BookImportHandler(this, bookRepository, epubValidator)
    }
}
