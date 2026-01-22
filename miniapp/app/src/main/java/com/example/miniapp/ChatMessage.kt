package com.example.miniapp

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val content: String,
    val isImage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)