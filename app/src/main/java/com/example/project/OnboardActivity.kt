package com.example.project

import com.example.project.utils.scaleViewsToScreen
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import com.example.project.databinding.ActivityOnboardBinding
import com.example.project.utils.windowInsetsListener

class OnboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("onboard", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time", true)

        if (!isFirstTime) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val rootView = binding.rootLayout
        scaleViewsToScreen(rootView, this)

        val originalTopMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val originalBottomMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(rootView, windowInsetsListener(originalTopMargin, originalBottomMargin))

        val buttonLayout = binding.buttonLayout
        buttonLayout.setOnClickListener {
            prefs.edit { putBoolean("first_time", false) }
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}