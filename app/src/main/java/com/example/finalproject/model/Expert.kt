package com.example.finalproject.model

data class Expert(
    val expertId: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val assignedUserIds: List<String> = emptyList()
)
