package com.example.project.model
import com.google.gson.annotations.SerializedName

data class NotesResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Note>
)

data class Note(
    val id: Int,
    val title: String,
    val description: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("creator_name") val creatorName: String,
    @SerializedName("creator_username") val creatorUsername: String
)

data class UpdateRequest(
    val title: String,
    val description: String
)

data class PartialUpdateRequest(
    val title: String? = null,
    val description: String? = null
)

data class NoteState(
    val title: String = "",
    val text: String = ""
)

data class CreateRequest(
    val title: String,
    val description: String
)