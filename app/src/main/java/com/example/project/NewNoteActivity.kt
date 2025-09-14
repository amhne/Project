package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.project.auth.ApiClient
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.data.DatabaseProvider
import com.example.project.data.NoteEntity
import com.example.project.model.CreateRequest
import com.example.project.databinding.ActivityNewNoteBinding
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.windowInsetsListener
import com.example.project.network.NoteApi
import com.example.project.utils.parseApiErrorMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NewNoteActivity: AppCompatActivity() {
    private lateinit var tokenManager: TokenManager
    private var isSubmitting = false
    private lateinit var title: EditText
    private lateinit var description: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNewNoteBinding.inflate(layoutInflater)
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

        progressBar = binding.progressBar
        title = binding.title
        description = binding.description

        val currentDate = Date()
        val outputFormat = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.ENGLISH)
        outputFormat.timeZone = TimeZone.getDefault()
        binding.textDate.text = currentDate.let {
            "Last Edited: ${outputFormat.format(it)}"
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                createNote()
            }
        })

        binding.headerWrapper.setOnClickListener {
            createNote()
        }

        binding.imageDelete.setOnClickListener {
            binding.dimView.visibility = View.VISIBLE
            binding.confirmLayout.visibility = View.VISIBLE
        }

        binding.dimView.setOnClickListener {
            binding.dimView.visibility = View.GONE
            binding.confirmLayout.visibility = View.GONE
        }

        binding.confirmLayout.setOnClickListener {}

        binding.closeButton.setOnClickListener {
            binding.dimView.visibility = View.GONE
            binding.confirmLayout.visibility = View.GONE
        }

        binding.deleteButton.setOnClickListener {
            val count = intent.getIntExtra("count", 1)
            if (count == 0) {
                val intent = Intent(this, HomeZeroActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun createNote() {
        if (isSubmitting) return

        val titleText = title.text.toString()
        val descriptionText = description.text.toString()

        if (titleText.isBlank() || descriptionText.isBlank()) {
            Toast.makeText(this, "Title and Description can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@NewNoteActivity).noteDao()
            val api = ApiClient.getInstance(applicationContext).create(NoteApi::class.java)
            try {
                val response = api.create(CreateRequest(titleText, descriptionText))
                if (response.isSuccessful) {
                    val body = response.body()
                    noteDao.insert(
                        NoteEntity(
                            serverId = body?.id,
                            title = body?.title ?: titleText,
                            description = body?.description ?: descriptionText,
                            createdAt = body?.createdAt,
                            updatedAt = body?.updatedAt,
                            creatorName = body?.creatorName,
                            creatorUsername = body?.creatorUsername,
                            isSynced = true
                        )
                    )
                    val intent = Intent(this@NewNoteActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                    Toast.makeText(this@NewNoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val username = sharedPref.getString("username", null)
                val currentTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                noteDao.insert(
                    NoteEntity(
                        title = titleText,
                        description = descriptionText,
                        creatorUsername = username,
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        isSynced = false
                    )
                )
                val intent = Intent(this@NewNoteActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }

            progressBar.visibility = View.GONE
            isSubmitting = false
        }
    }
}

