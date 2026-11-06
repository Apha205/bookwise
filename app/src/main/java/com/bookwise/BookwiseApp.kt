package com.bookwise

import android.app.Application
import com.bookwise.data.local.BookwiseDatabase
import com.bookwise.data.preferences.AppPreferences
import com.bookwise.data.preferences.SettingsRepository
import com.bookwise.data.repository.BookRepository
import com.bookwise.data.repository.ExplanationRepositoryImpl
import com.bookwise.data.repository.ReadingRepository
import com.bookwise.data.provider.ChatRepository
import com.bookwise.data.provider.ChatRepositoryImpl
import com.bookwise.data.provider.ProviderRepository
import com.bookwise.data.provider.ProviderRepositoryImpl
import com.bookwise.data.secure.KeyStoreManager
import com.bookwise.data.secure.SecureKeyRepository
import com.bookwise.tutor.TutorRepository
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
    lateinit var appPreferences: AppPreferences
        private set
    lateinit var epubValidator: EpubValidator
        private set
    lateinit var importHandler: BookImportHandler
        private set
    lateinit var secureKeyRepository: SecureKeyRepository
        private set
    lateinit var providerRepository: ProviderRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var tutorRepository: TutorRepository
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
        appPreferences = AppPreferences(this)
        settingsRepository = SettingsRepository(appPreferences)
        epubValidator = EpubValidator()
        importHandler = BookImportHandler(this, bookRepository, epubValidator)
        secureKeyRepository = SecureKeyRepository(this, KeyStoreManager())
        providerRepository = ProviderRepositoryImpl(appPreferences, secureKeyRepository)
        chatRepository = ChatRepositoryImpl()
        tutorRepository = TutorRepository(chatRepository, providerRepository, ExplanationRepositoryImpl(database.explanationDao()))
    }
}
