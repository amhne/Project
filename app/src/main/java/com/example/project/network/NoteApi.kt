package com.example.project.network

import com.example.project.model.CreateRequest
import com.example.project.model.Note
import com.example.project.model.NotesResponse
import com.example.project.model.PartialUpdateRequest
import com.example.project.model.UpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface NoteApi {
    @GET
    suspend fun getNotesByUrl(@Url url: String): Response<NotesResponse>

    @GET("api/notes/")
    suspend fun getNotes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 4
    ): Response<NotesResponse>

    @GET("api/notes/filter/")
    suspend fun getFilteredNotes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 4,
        @Query("title") title: String? = null
    ): Response<NotesResponse>

    @POST("api/notes/bulk")
    suspend fun bulkCreate(@Body notes: List<CreateRequest>): Response<List<Note>>

    @DELETE("api/notes/{id}/")
    suspend fun deleteNote(
        @Path("id") id: Int
    ): Response<Void>

    @PUT("api/notes/{id}/")
    suspend fun updateNote(
        @Path("id") id: Int,
        @Body updateRequest: UpdateRequest
    ): Response<Note>

    @GET("api/notes/{id}/")
    suspend fun getNote(
        @Path("id") id: Int
    ): Response<Note>

    @PATCH("api/notes/{id}/")
    suspend fun partialUpdateNote(
        @Path("id") id: Int,
        @Body partialUpdateRequest: PartialUpdateRequest
    ): Response<Note>

    @POST("api/notes")
    suspend fun create(@Body createRequest: CreateRequest): Response<Note>
}