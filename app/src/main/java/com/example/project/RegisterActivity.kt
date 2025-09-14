package com.example.project


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.project.auth.TokenManager
import com.example.project.auth.ApiClient
import com.example.project.auth.TokenApp
import com.example.project.network.AuthApi
import com.example.project.model.UserInfoResponse
import com.example.project.databinding.ActivityRegisterBinding
import com.example.project.utils.parseApiErrorMessage
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.windowInsetsListener
import kotlinx.coroutines.launch




class RegisterActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = binding.rootLayout
        scaleViewsToScreen(rootView, this)

        val originalTopMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val originalBottomMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(rootView, windowInsetsListener(originalTopMargin, originalBottomMargin))

        tokenManager = (application as TokenApp).tokenManager

        val goToLoginListener = View.OnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.headerWrapper.setOnClickListener(goToLoginListener)
        binding.loginText.setOnClickListener(goToLoginListener)

        binding.buttonLayout.setOnClickListener {
            val username = binding.inputUname.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val firstName = binding.inputFname.text.toString().trim()
            val lastName = binding.inputLname.text.toString().trim()
            val retypePassword = binding.inputRePassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || retypePassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (retypePassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != retypePassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val registerBody = mapOf(
                "username" to username,
                "email" to email,
                "password" to password,
                "first_name" to firstName,
                "last_name" to lastName
            )

            val authApi = ApiClient.getInstance(this).create(AuthApi::class.java)
            lifecycleScope.launch {
                try {
                    val response = authApi.register(registerBody)
                    if (response.isSuccessful && response.body() != null) {
                        val registeredUser = response.body()!!
                        Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                        loginUser(authApi, registeredUser.username, password)
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@RegisterActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loginUser(authApi: AuthApi, username: String, password: String) {
        lifecycleScope.launch {
            try {
                val loginBody = mapOf("username" to username, "password" to password)
                val response = authApi.login(loginBody)
                if (response.isSuccessful && response.body() != null) {
                    val access = response.body()!!.access
                    val refresh = response.body()!!.refresh
                    tokenManager.saveTokens(access, refresh)
                    fetchUserInfo(authApi)
                    val intent = Intent(this@RegisterActivity, HomeZeroActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Login failed", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@RegisterActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserInfo(userInfo: UserInfoResponse) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putInt("id", userInfo.id)
            putString("username", userInfo.username)
            putString("email", userInfo.email)
            putString("first_name", userInfo.firstName)
            putString("last_name", userInfo.lastName)
            apply()
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
                Toast.makeText(this@RegisterActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
        }
    }
}