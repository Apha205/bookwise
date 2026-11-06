package com.bookwise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bookwise.BookwiseApp
import com.bookwise.data.repository.BookRepository
import com.bookwise.library.BookImportHandler
import com.bookwise.library.LibraryScreen
import com.bookwise.library.LibraryViewModel
import com.bookwise.reader.ReaderScreen
import com.bookwise.settings.SettingsScreen

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
                onImportClick = { navController.navigate(Routes.READER) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onBookClick = { navController.navigate(Routes.READER) },
                viewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                        require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
                        return LibraryViewModel(app.bookRepository, app.importHandler) as T
                    }
                })
            )
        }
        composable(Routes.READER) {
            ReaderScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
