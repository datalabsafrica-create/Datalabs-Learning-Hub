package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Datalabs Learning Hub", appName)
  }

  @Test
  fun `main activity launches and renders sign in screen without crashing`() {
    val activity = composeTestRule.activity
    assertNotNull(activity)
    composeTestRule.mainClock.advanceTimeBy(1000L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
  }
}

