package com.wildwatch.app.feature.report

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.wildwatch.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReportIncidentUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun sightingReport_validationWorks() {
        // Navigate to Sighting Report (Assuming there's a way to get there, 
        // or we just set the content if it's a isolated test. 
        // For now, let's assume we start at the screen or use a test activity)
        
        // Find the "Share Sighting" button
        val submitButton = composeTestRule.onNodeWithText("Share Sighting")
        
        // Verify button is disabled initially (Description is empty)
        submitButton.assertIsNotEnabled()

        // Fill in description
        composeTestRule.onNodeWithText("Describe the situation…").performTextInput("Spotted a family of gorillas.")
        
        // Verify button is now enabled
        submitButton.assertIsEnabled()
    }

    @Test
    fun sightingReport_submissionShowsLoading() {
        composeTestRule.onNodeWithText("Describe the situation…").performTextInput("Valid report")
        
        composeTestRule.onNodeWithText("Share Sighting").performClick()
        
        // Check for loading state (this might be fast, but we can verify the text changes)
        composeTestRule.onNodeWithText("Submitting...").assertExists()
    }
}
