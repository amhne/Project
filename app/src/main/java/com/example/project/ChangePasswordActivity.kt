package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.project.auth.ApiClient
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.network.AuthApi
import com.example.project.databinding.ActivityChangePasswordBinding
import com.example.project.utils.parseApiErrorMessage
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.windowInsetsListener
import kotlinx.coroutines.launch


class ChangePasswordActivity:AppCompatActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = binding.rootLayout
        scaleViewsToScreen(binding.rootLayout, this)

        val originalTopMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val originalBottomMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(rootView, windowInsetsListener(originalTopMargin, originalBottomMargin))

        tokenManager = (application as TokenApp).tokenManager
        tokenManager.logoutEvent.observe(this) { isLoggedOut ->
            if (isLoggedOut == true) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        val count = intent.getIntExtra("count", -1)

        binding.headerWrapper.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("count", count)
            startActivity(intent)
            finish()
        }

        binding.buttonLayout.setOnClickListener {
            val currentPassword = binding.inputCurrent.text.toString().trim()
            val newPassword = binding.inputNew.text.toString().trim()
            val rePassword = binding.inputRe.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || rePassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rePassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != rePassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val changePasswordBody = mapOf(
                "old_password" to currentPassword,
                "new_password" to newPassword,
            )

            lifecycleScope.launch {
                try {
                    val changeApi = ApiClient.getInstance(this@ChangePasswordActivity).create(AuthApi::class.java)
                    val response = changeApi.changePassword(changePasswordBody)
                    if (response.isSuccessful) {
                        val changePasswordResponse = response.body()
                        Toast.makeText(this@ChangePasswordActivity, changePasswordResponse?.detail, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@ChangePasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@ChangePasswordActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}