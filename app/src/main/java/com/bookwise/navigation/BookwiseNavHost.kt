package com.bookwise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookwise.BookwiseApp
import com.bookwise.data.repository.BookRepository
import com.bookwise.library.BookImportHandler
import com.bookwise.library.LibraryScreen
import com.bookwise.library.LibraryViewModel
import com.bookwise.reader.ReaderScreen
import com.bookwise.reader.ReaderViewModel
import com.bookwise.tutor.TutorViewModel
import com.bookwise.settings.SettingsScreen
import com.bookwise.settings.SettingsViewModel

@Composable
fun BookwiseNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as BookwiseApp

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onImportClick = { },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onBookClick = { book -> navController.navigate(Routes.reader(book.id)) },
                viewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                        require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
                        return LibraryViewModel(app.bookRepository, app.importHandler) as T
                    }
                })
            )
        }
        composable(
            route = "${Routes.READER}/{${Routes.READER_ARG_BOOK_ID}}",
            arguments = listOf(navArgument(Routes.READER_ARG_BOOK_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString(Routes.READER_ARG_BOOK_ID)
                ?: return@composable
            ReaderScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel(factory = ReaderViewModel.factory(
                    application = app,
                    bookId = bookId,
                    bookRepository = app.bookRepository,
                    readingRepository = app.readingRepository
                )),
                tutorViewModel = viewModel<TutorViewModel>(factory = TutorViewModel.factory(app.tutorRepository))
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel(factory = SettingsViewModel.factory(
                    providerRepository = app.providerRepository,
                    chatRepository = app.chatRepository,
                    secureKeyRepository = app.secureKeyRepository
                ))
            )
        }
    }
}
