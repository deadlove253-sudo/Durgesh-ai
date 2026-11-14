package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `app name matches Durgesh AI`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Durgesh AI", appName)
  }

  @Test
  fun `tapping mic without permission shows permission rationale dialog`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mic_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("grant_permission_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismiss_permission_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `typed offline command creates chat history and navigates tabs`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("command_text_input").performTextInput("25 जमा 17")
    composeTestRule.onNodeWithTag("send_command_button").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("25 + 17 का उत्तर 42 है।").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("chat_history_list").performScrollToNode(hasText("25 + 17 का उत्तर 42 है।"))
    composeTestRule.onNodeWithText("25 + 17 का उत्तर 42 है।").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_COMMANDS").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("clear_voice_history_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_SETTINGS").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("toggle_auto_speak").assertIsDisplayed()
  }
}
