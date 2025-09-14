package com.example.project


import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.project.auth.TokenApp
import com.example.project.utils.parseApiErrorMessage
import com.example.project.utils.scaleViewsToScreen
import com.example.project.databinding.ActivityLoginBinding
import com.example.project.utils.windowInsetsListener
import com.example.project.auth.ApiClient
import com.example.project.auth.TokenManager
import com.example.project.network.AuthApi
import com.example.project.network.NoteApi
import com.example.project.model.UserInfoResponse
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = binding.rootLayout
        scaleViewsToScreen(rootView, this)

        val originalTopMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val originalBottomMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(rootView, windowInsetsListener(originalTopMargin, originalBottomMargin))

        tokenManager = (application as TokenApp).tokenManager

        binding.registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.buttonLayout.setOnClickListener {
            val username = binding.inputUsername.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = mapOf("username" to username, "password" to password)
            val authApi = ApiClient.getInstance(this).create(AuthApi::class.java)

            lifecycleScope.launch {
                println(body)
                try {
                    val response = authApi.login(body)
                    if (response.isSuccessful && response.body() != null) {
                        val access = response.body()!!.access
                        val refresh = response.body()!!.refresh
                        tokenManager.saveTokens(access, refresh)
                        fetchUserInfo(authApi)
                        checkNotes()
                    } else {
                        val errorBodyString = response.errorBody()?.string()
                        val errorMsg = parseApiErrorMessage(errorBodyString)
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@LoginActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUserInfo(userInfo: UserInfoResponse) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit {
            putInt("id", userInfo.id)
                .putString("username", userInfo.username)
                .putString("email", userInfo.email)
                .putString("first_name", userInfo.firstName)
                .putString("last_name", userInfo.lastName)
                .apply()
        }
    }

    private fun fetchUserInfo(authApi: AuthApi) {
        lifecycleScope.launch {
            try {
                val response = authApi.getUserInfo()
                if (response.isSuccessful && response.body() != null) {
                    saveUserInfo(response.body()!!)
                }
            } catch (_: Exception) {
                Toast.makeText(this@LoginActivity, "Failed to connect2", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkNotes() {
        val notesApi = ApiClient.getInstance(this).create(NoteApi::class.java)

        lifecycleScope.launch {
            try {
                val response = notesApi.getNotes(page = 1, pageSize = 1)
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()!!.count
                    if (count == 0) {
                        val intent = Intent(this@LoginActivity, HomeZeroActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@LoginActivity, "Failed to connect3", Toast.LENGTH_SHORT).show()
            }
        }
    }
}