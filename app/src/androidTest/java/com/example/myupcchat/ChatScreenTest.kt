package com.example.myupcchat

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import com.example.myupcchat.ui.chat.ChatScreen
import com.example.myupcchat.ui.chat.ChatUiState
import com.example.myupcchat.ui.theme.MyUpcChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun composingAndSendingEmitsTheDraft() {
        var sent = ""
        compose.setContent {
            var state by remember { mutableStateOf(ChatUiState()) }
            MyUpcChatTheme {
                ChatScreen(state, { state = state.copy(draft = it) }, { sent = state.draft }, {})
            }
        }
        compose.onNodeWithText("Enviar").assertIsNotEnabled()
        saveScreenshot("welcome-light")
        compose.onNodeWithTag("draft").performTextInput("Hola Gemini")
        saveScreenshot("keyboard-light")
        compose.onNodeWithText("Enviar").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("Hola Gemini", sent) }
    }

    @Test fun missingKeyDisablesSending() {
        compose.setContent {
            MyUpcChatTheme { ChatScreen(ChatUiState(draft = "Hola", isConfigured = false), {}, {}, {}) }
        }
        compose.onNodeWithText("Configura GEMINI_API_KEY", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Enviar").assertIsNotEnabled()
    }

    @Test fun darkThemeShowsLongMessagesAndAllowsReset() {
        compose.setContent {
            var state by remember { mutableStateOf(ChatUiState(messages = listOf(
                ChatMessage(Author.USER, "Mensaje de prueba"),
                ChatMessage(Author.ASSISTANT, "Una respuesta larga. ".repeat(50)),
            ))) }
            MyUpcChatTheme(darkTheme = true, dynamicColor = false) {
                ChatScreen(state, {}, {}, { state = ChatUiState() })
            }
        }
        compose.onNodeWithText("Una respuesta larga.", substring = true).assertIsDisplayed()
        saveScreenshot("conversation-dark")
        compose.onNodeWithText("Nuevo chat").performClick()
        compose.onNodeWithText("¿Qué vamos a descubrir?").assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        captureWorkshopScreenshot(name)
    }
}
