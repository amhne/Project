package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.databinding.ActivitySettingsBinding
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.windowInsetsListener

class SettingsActivity: AppCompatActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = binding.rootLayout
        scaleViewsToScreen(rootView, this)

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
            when (count) {
                0 -> {
                    val intent = Intent(this, HomeZeroActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                1 -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Failed to go back.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Username not found")
        val email = sharedPref.getString("email", "Email not found")
        binding.name.text = username
        binding.email.text = email

        binding.changePassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("count", count)
            startActivity(intent)
        }

        binding.logout.setOnClickListener {
            binding.dimView.visibility = View.VISIBLE
            binding.confirmLayout.visibility = View.VISIBLE
        }

        binding.confirmLayout.setOnClickListener {}

        binding.dimView.setOnClickListener {
            binding.dimView.visibility = View.GONE
            binding.confirmLayout.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener {
            binding.dimView.visibility = View.GONE
            binding.confirmLayout.visibility = View.GONE
        }

        binding.yesButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }
}