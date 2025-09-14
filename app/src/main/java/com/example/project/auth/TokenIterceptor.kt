package com.example.project.auth


import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class TokenInterceptor(context: Context) : Interceptor {
    private val tokenManager = TokenManager(context)
    interface AuthApi {
        @POST("api/auth/token/refresh/")
        fun refresh(@Body body: Map<String, String>): retrofit2.Call<TokenRefreshResponse>
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val accessToken = tokenManager.getAccessToken()
        if (accessToken != null) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                val newAccessToken = runCatching {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("http://10.0.2.2:8000")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val authApi = retrofit.create(AuthApi::class.java)
                    val body = mapOf("refresh" to refreshToken)
                    val refreshResponse = authApi.refresh(body).execute()
                    if (refreshResponse.isSuccessful) {
                        val newAccess = refreshResponse.body()?.access
                        if (newAccess != null) {
                            tokenManager.saveTokens(newAccess, refreshToken)
                        }
                        newAccess
                    } else {
                        null
                    }
                }.getOrNull()

                if (newAccessToken != null) {
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    tokenManager.clearTokens()
                }
            } else {
                tokenManager.clearTokens()
            }
        }
        return response
    }
}

data class TokenRefreshResponse(val access: String)
