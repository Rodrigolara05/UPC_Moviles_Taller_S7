package com.example.myupcchat.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myupcchat.R
import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import com.example.myupcchat.ui.theme.MyUpcChatTheme

/** Vista sin red ni ViewModel: recibe estado y devuelve acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(state: ChatUiState, onDraftChange: (String) -> Unit, onSend: () -> Unit, onNewChat: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isLoading) {
        val count = state.messages.size + if (state.isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.chat_subtitle), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }, actions = { TextButton(onClick = onNewChat) { Text(stringResource(R.string.new_chat)) } })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            if (!state.isConfigured) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.missing_key), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.messages.isEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                    item { Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    item { Text(stringResource(R.string.welcome_body), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(listOf(R.string.suggestion_one, R.string.suggestion_two, R.string.suggestion_three)) { resource ->
                        val suggestion = stringResource(resource)
                        OutlinedButton(onClick = { onDraftChange(suggestion) }, modifier = Modifier.fillMaxWidth()) {
                            Text(suggestion, Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().testTag("messages"),
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.messages, key = { it.id }) { MessageBubble(it) }
                    if (state.isLoading) item(key = "loading") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.thinking), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            state.error?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, Modifier.weight(1f).padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onSend, enabled = state.canSend) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = state.draft, onValueChange = onDraftChange,
                            modifier = Modifier.weight(1f).testTag("draft"), enabled = !state.isLoading,
                            placeholder = { Text(stringResource(R.string.message_placeholder)) },
                            maxLines = 4, shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { if (state.canSend) onSend() }))
                        Button(onClick = onSend, enabled = state.canSend, modifier = Modifier.heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.send))
                        }
                    }
                    Text(stringResource(R.string.ai_notice), Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.author == Author.USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Text(stringResource(if (isUser) R.string.you else R.string.assistant), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(if (isUser) 0.85f else 0.94f),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp, 20.dp, if (isUser) 4.dp else 20.dp, if (isUser) 20.dp else 4.dp)) {
            SelectionContainer { Text(message.text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConversationPreview() {
    MyUpcChatTheme(dynamicColor = false) {
        ChatScreen(ChatUiState(messages = listOf(
            ChatMessage(Author.USER, "¿Qué puedo crear con Kotlin?"),
            ChatMessage(Author.ASSISTANT, "Puedes crear aplicaciones Android. Con Compose describes la interfaz con funciones y el estado determina qué aparece en pantalla."),
        )), {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    MyUpcChatTheme(dynamicColor = false) { ChatScreen(ChatUiState(isConfigured = false), {}, {}, {}) }
}
