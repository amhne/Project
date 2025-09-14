package com.example.project.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE creatorUsername = :userName AND isDeleted = 0 LIMIT :pageSize OFFSET :offset")
    suspend fun getActiveNotes(pageSize: Int, offset: Int, userName: String?): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getPendingDeletes(): List<NoteEntity>

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE serverId = :serverId LIMIT 1")
    suspend fun getNoteByServerId(serverId: Int): NoteEntity?

    @Query("SELECT * FROM notes WHERE localId = :localId LIMIT 1")
    suspend fun getNoteByLocalId(localId: Int): NoteEntity?

    @Query("DELETE FROM notes WHERE serverId = :serverId")
    suspend fun deleteByServerId(serverId: Int)

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND isDeleted = 0 AND serverId IS NOT NULL")
    suspend fun getUnsyncedUpdates(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND creatorUsername = :userName AND title LIKE '%' || :title || '%' ORDER BY createdAt DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getNotesFilteredByTitle(title: String,pageSize: Int, offset: Int, userName: String?): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE creatorUsername = :userName")
    fun getNotesCount(userName: String?): Int
}
