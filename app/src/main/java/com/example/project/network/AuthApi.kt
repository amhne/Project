package com.example.project.network

import com.example.project.model.ChangePasswordResponse
import com.example.project.model.LoginResponse
import com.example.project.model.RegisterResponse
import com.example.project.model.UserInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/token/")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("api/auth/userinfo/")
    suspend fun getUserInfo(): Response<UserInfoResponse>

    @POST("api/auth/register/")
    suspend fun register(@Body body: Map<String, String>): Response<RegisterResponse>

    @POST("/api/auth/change-password/")
    suspend fun changePassword(@Body request: Map<String, String>): Response<ChangePasswordResponse>
}
