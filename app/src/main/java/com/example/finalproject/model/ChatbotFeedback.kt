package com.example.finalproject.model

data class ChatbotFeedback(
    val feedbackId: String = "",
    val userId: String = "",
    val expertId: String = "",
    val chatbotMessage: String = "",
    val userComment: String = "",
    val timestamp: String = "",
    val resolved: Boolean = false,
    val expertResponse: String = ""
)
