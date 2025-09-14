package com.example.project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val serverId: Int? = null,
    val title: String,
    val description: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val creatorName: String? = null,
    val creatorUsername: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)