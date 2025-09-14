package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.databinding.ActivityHomeZeroBinding
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.windowInsetsListener

class HomeZeroActivity: AppCompatActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeZeroBinding.inflate(layoutInflater)
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

        binding.centerContainer.setOnClickListener{
            val intent = Intent(this, NewNoteActivity::class.java)
            intent.putExtra("count", 0)
            startActivity(intent)
            finish()
        }

        binding.clickRight.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("count", 0)
            startActivity(intent)
        }
    }
}