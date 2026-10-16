package com.example.myupcchat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myupcchat.data.ChatException
import com.example.myupcchat.data.ChatRepository
import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = true,
) {
    val canSend: Boolean get() = isConfigured && !isLoading && draft.isNotBlank()
}

class ChatViewModel(
    private val repository: ChatRepository,
    private val isConfigured: Boolean = true,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState(isConfigured = isConfigured))
    val uiState = _uiState.asStateFlow()
    private var requestJob: Job? = null
    private var conversationVersion = 0

    fun updateDraft(text: String) {
        if (!_uiState.value.isLoading) _uiState.update { it.copy(draft = text, error = null) }
    }

    fun sendMessage() {
        val before = _uiState.value
        if (!before.canSend) return
        val question = before.draft.trim()
        val history = before.messages + ChatMessage(Author.USER, question)
        val version = conversationVersion
        // Actualizar antes de launch evita que una doble pulsación cree dos peticiones.
        _uiState.update { it.copy(messages = history, draft = "", isLoading = true, error = null) }
        requestJob = viewModelScope.launch {
            try {
                val answer = repository.generateReply(history)
                if (version == conversationVersion) _uiState.update {
                    it.copy(messages = history + ChatMessage(Author.ASSISTANT, answer), isLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Retirar el turno fallido y restaurar el borrador permite reintentar sin duplicarlo.
                if (version == conversationVersion) _uiState.update {
                    it.copy(
                        messages = before.messages, draft = question, isLoading = false,
                        error = (error as? ChatException)?.message ?: "Ocurrió un error. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun newChat() {
        conversationVersion++
        requestJob?.cancel()
        requestJob = null
        _uiState.value = ChatUiState(isConfigured = isConfigured)
    }

    class Factory(
        private val repository: ChatRepository,
        private val isConfigured: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java))
            return ChatViewModel(repository, isConfigured) as T
        }
    }
}
