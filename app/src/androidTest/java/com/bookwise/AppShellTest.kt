package com.bookwise

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppShellTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun libraryScreen_showsEmptyState() {
        composeRule.onNodeWithText("Your library is empty").assertExists()
        composeRule.onNodeWithText("Add book").assertExists()
    }
}
