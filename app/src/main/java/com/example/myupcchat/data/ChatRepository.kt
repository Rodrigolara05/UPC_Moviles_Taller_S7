package com.example.myupcchat.data

import com.example.myupcchat.model.ChatMessage

interface ChatRepository {
    suspend fun generateReply(history: List<ChatMessage>): String
}

/** Mensaje apto para mostrar en pantalla, sin cuerpos HTTP ni credenciales. */
class ChatException(message: String) : Exception(message)
