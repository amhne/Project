package com.example.project.model

data class RegisterResponse(
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?
)