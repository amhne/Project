package com.example.project.model

data class UserInfoResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?
)