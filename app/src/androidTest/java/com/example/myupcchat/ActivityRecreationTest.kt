package com.example.myupcchat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Rule
import org.junit.Test

class ActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun draftSurvivesActivityRecreation() {
        compose.onNodeWithTag("draft").performTextInput("Mi borrador antes de girar")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("draft").assertTextContains("Mi borrador antes de girar")
        captureWorkshopScreenshot("activity-recreated")
    }

    @Test fun editorRemainsVisibleWithSoftwareKeyboard() {
        compose.onNodeWithTag("draft").performClick().performTextInput("Mensaje con teclado abierto")
        compose.runOnUiThread {
            WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
                .show(WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("draft").assertIsDisplayed()
        compose.onNodeWithText("Enviar").assertIsDisplayed()
        captureWorkshopScreenshot("activity-keyboard")
    }
}
