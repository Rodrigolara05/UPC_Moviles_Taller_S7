package com.example.myupcchat.model

import java.util.UUID

enum class Author { USER, ASSISTANT }

data class ChatMessage(
    val author: Author,
    val text: String,
    val id: String = UUID.randomUUID().toString(),
)
