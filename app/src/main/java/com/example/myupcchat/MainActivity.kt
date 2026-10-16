package com.example.myupcchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myupcchat.data.GeminiChatRepository
import com.example.myupcchat.ui.chat.ChatScreen
import com.example.myupcchat.ui.chat.ChatViewModel
import com.example.myupcchat.ui.theme.MyUpcChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyUpcChatTheme {
                val factory = remember {
                    ChatViewModel.Factory(
                        GeminiChatRepository(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL),
                        BuildConfig.GEMINI_API_KEY.isNotBlank(),
                    )
                }
                val viewModel: ChatViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ChatScreen(state, viewModel::updateDraft, viewModel::sendMessage, viewModel::newChat)
            }
        }
    }
}

