package com.bookwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bookwise.data.preferences.AppPreferences
import com.bookwise.navigation.BookwiseNavHost
import com.bookwise.ui.theme.BookwiseTheme

class MainActivity : ComponentActivity() {

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        // Persistable read access — we copy into private storage, so short-lived grant is sufficient.
        try {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        // Forward to the active LibraryViewModel via a simple Intent extra is unnecessary:
        // LibraryScreen uses its own picker launcher. This launcher is kept as a fallback
        // for future deep-link handling. Actual import is triggered from LibraryScreen.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appPreferences = AppPreferences(this)
        setContent {
            val themeMode by appPreferences.themeModeFlow().collectAsState(initial = "system")
            val darkTheme = themeMode == "dark"
            BookwiseTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookwiseNavHost()
                }
            }
        }
    }
}
