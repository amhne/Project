package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.auth.ApiClient
import com.example.project.data.DatabaseProvider
import com.example.project.model.NoteState
import com.example.project.model.PartialUpdateRequest
import com.example.project.model.UpdateRequest
import com.example.project.databinding.ActivityNoteBinding
import com.example.project.utils.scaleViewsToScreen
import com.example.project.utils.parseApiErrorMessage
import com.example.project.utils.windowInsetsListener
import com.example.project.network.NoteApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class NoteActivity: AppCompatActivity() {
    private lateinit var tokenManager: TokenManager
    private var isUpdating = false
    private var isDeleting = false
    private lateinit var title: EditText
    private lateinit var description: EditText
    private lateinit var progressBar: ProgressBar
    private var originalNoteState: NoteState = NoteState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNoteBinding.inflate(layoutInflater)
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

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)
        val id = intent.getIntExtra("noteId", Int.MIN_VALUE)
        if (id == Int.MIN_VALUE) {
            Toast.makeText(this, "Invalid note id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (id < 0) {
            val localId = -id
            getNoteById(localId, binding.textDate)
        } else {
            getNoteById(id, binding.textDate)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTitle = title.text.toString()
                val currentDescription = description.text.toString()
                val isTitleChanged = currentTitle != originalNoteState.title
                val isTextChanged = currentDescription != originalNoteState.text
                if (!isTitleChanged && !isTextChanged) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    saveNoteChanges(id, originalNoteState, currentTitle, currentDescription)
                    val intent = Intent(this@NoteActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        })

        binding.headerWrapper.setOnClickListener {
            val currentTitle = title.text.toString()
            val currentDescription = description.text.toString()
            val isTitleChanged = currentTitle != originalNoteState.title
            val isTextChanged = currentDescription != originalNoteState.text
            if (!isTitleChanged && !isTextChanged) {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                saveNoteChanges(id, originalNoteState, currentTitle, currentDescription)
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
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
            deleteNote(id)
            lifecycleScope.launch {
                val noteDao = DatabaseProvider.getDatabase(this@NoteActivity).noteDao()
                val count = withContext(Dispatchers.IO) {
                    noteDao.getNotesCount(username)
                }
                if (count == 1) {
                    val intent = Intent(this@NoteActivity, HomeZeroActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this@NoteActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun getNoteById(id: Int, lastEditedTextView: TextView) {
        val api = ApiClient.getInstance(this).create(NoteApi::class.java)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@NoteActivity).noteDao()
            try {
                val localNote = withContext(Dispatchers.IO) {
                    noteDao.getNoteByLocalId(id)
                }

                if (localNote != null) {
                    progressBar.visibility = View.GONE

                    title.setText(localNote.title)
                    description.setText(localNote.description)
                    originalNoteState = NoteState(title = localNote.title, text = localNote.description)

                    try {
                        val createdAt = localNote.createdAt?.let { Instant.parse(it) }
                        val updatedAt = localNote.updatedAt?.let { Instant.parse(it) }

                        val lastEdited = when {
                            createdAt != null && updatedAt != null -> if (updatedAt.isAfter(createdAt)) updatedAt else createdAt
                            createdAt != null -> createdAt
                            updatedAt != null -> updatedAt
                            else -> null
                        }

                        val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm", Locale.ENGLISH)
                            .withZone(ZoneId.systemDefault())

                        lastEditedTextView.text = lastEdited?.let {
                            "Last Edited: ${outputFormatter.format(it)}"
                        } ?: "Invalid date555"

                    } catch (_: Exception) {
                        lastEditedTextView.text = lastEditedTextView.context.getString(R.string.invalid_date)
                    }
                } else {
                    val response = api.getNote(id)
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val note = response.body()
                        if (note != null) {
                            title.setText(note.title)
                            description.setText(note.description)
                            originalNoteState = NoteState(title = note.title, text = note.description)

                            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                            formatter.timeZone = TimeZone.getTimeZone("UTC")

                            try {
                                val createdAt = formatter.parse(note.createdAt)
                                val updatedAt = formatter.parse(note.updatedAt)

                                val lastEdited = when {
                                    createdAt != null && updatedAt != null -> {
                                        if (updatedAt.after(createdAt)) updatedAt else createdAt
                                    }
                                    createdAt != null -> createdAt
                                    updatedAt != null -> updatedAt
                                    else -> null
                                }

                                val outputFormat = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.ENGLISH)
                                outputFormat.timeZone = TimeZone.getDefault()

                                lastEditedTextView.text = lastEdited?.let {
                                    "Last Edited: ${outputFormat.format(it)}"
                                } ?: "Invalid date"

                            } catch (_: Exception) {
                                lastEditedTextView.text = lastEditedTextView.context.getString(R.string.invalid_date)
                            }

                        } else {
                            val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                            Toast.makeText(this@NoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@NoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

            } catch (_: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@NoteActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun deleteNote(id: Int) {
        if (isDeleting) return

        isDeleting = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@NoteActivity).noteDao()
            if (id >= 0) {
                try {
                    val api = ApiClient.getInstance(this@NoteActivity).create(NoteApi::class.java)
                    val response = api.deleteNote(id)
                    if (response.isSuccessful) {
                        withContext(Dispatchers.IO) {
                            noteDao.deleteByServerId(id)
                        }
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@NoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception){
                    withContext(Dispatchers.IO) {
                        val note = noteDao.getNoteByServerId(id)
                        if (note != null) {
                            val deletedNote = note.copy(isDeleted = true, isSynced = false)
                            noteDao.update(deletedNote)
                        }
                    }
                } finally {
                    isDeleting = false
                    progressBar.visibility = View.GONE
                }
            } else {
                val localId = -id
                withContext(Dispatchers.IO) {
                    val note = noteDao.getNoteByLocalId(localId)
                    if (note != null) {
                        val deletedNote = note.copy(isDeleted = true, isSynced = false)
                        noteDao.update(deletedNote)
                    }
                }
                isDeleting = false
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun updateNote(id: Int, updateRequest: UpdateRequest) {
        if (isUpdating) return

        isUpdating = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@NoteActivity).noteDao()
            if (id >= 0) {
                try {
                    val api = ApiClient.getInstance(applicationContext).create(NoteApi::class.java)
                    val response = api.updateNote(id, updateRequest)
                    if (response.isSuccessful) {
                        withContext(Dispatchers.IO) {
                            val note = noteDao.getNoteByServerId(id)
                            if (note != null) {
                                val updatedNote = note.copy(
                                    title = updateRequest.title,
                                    description = updateRequest.description,
                                    isSynced = true
                                )
                                noteDao.update(updatedNote)
                            }
                        }
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@NoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) {
                        val note = noteDao.getNoteByServerId(id)
                        val currentTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                        if (note != null) {
                            val updatedNote = note.copy(
                                title = updateRequest.title,
                                description = updateRequest.description,
                                isSynced = false,
                                updatedAt = currentTime
                            )
                            noteDao.update(updatedNote)
                        }
                    }
                } finally {
                    isUpdating = false
                    progressBar.visibility = View.GONE
                }
            } else {
                val localId = -id
                withContext(Dispatchers.IO) {
                    val note = noteDao.getNoteByLocalId(localId)
                    val currentTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    if (note != null) {
                        val updatedNote = note.copy(
                            title = updateRequest.title,
                            description = updateRequest.description,
                            isSynced = false,
                            updatedAt = currentTime
                        )
                        noteDao.update(updatedNote)
                    }
                }
                isUpdating = false
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun partialUpdateNote(id: Int, partialUpdateRequest: PartialUpdateRequest) {
        if (isUpdating) return

        isUpdating = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@NoteActivity).noteDao()
            if (id >= 0) {
                try {
                    val api = ApiClient.getInstance(applicationContext).create(NoteApi::class.java)
                    val response = api.partialUpdateNote(id, partialUpdateRequest)
                    if (response.isSuccessful) {
                        withContext(Dispatchers.IO) {
                            val note = noteDao.getNoteByServerId(id)
                            if (note != null) {
                                val updatedNote = note.copy(
                                    title = partialUpdateRequest.title ?: note.title,
                                    description = partialUpdateRequest.description ?: note.description,
                                    isSynced = true
                                )
                                noteDao.update(updatedNote)
                            }
                        }
                    } else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@NoteActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) {
                        val note = noteDao.getNoteByServerId(id)
                        if (note != null) {
                            val updatedNote = note.copy(
                                title = partialUpdateRequest.title ?: note.title,
                                description = partialUpdateRequest.description ?: note.description,
                                isSynced = false
                            )
                            noteDao.update(updatedNote)
                        }
                    }
                } finally {
                    isUpdating = false
                    progressBar.visibility = View.GONE
                }
            } else {
                withContext(Dispatchers.IO) {
                    val localId = -id
                    val note = noteDao.getNoteByLocalId(localId)
                    if (note != null) {
                        val updatedNote = note.copy(
                            title = partialUpdateRequest.title ?: note.title,
                            description = partialUpdateRequest.description ?: note.description,
                            isSynced = false
                        )
                        noteDao.update(updatedNote)
                    }
                }
                isUpdating = false
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun saveNoteChanges(id: Int, originalNoteState: NoteState, newTitle: String, newText: String) {
        val isTitleChanged = newTitle != originalNoteState.title
        val isTextChanged = newText != originalNoteState.text

        if (isTitleChanged && isTextChanged) {
            val updateRequest = UpdateRequest(title = newTitle, description = newText)
            updateNote(id, updateRequest)
        } else {
            val partialUpdateRequest = PartialUpdateRequest(
                title = if (isTitleChanged) newTitle else null,
                description = if (isTextChanged) newText else null
            )
            partialUpdateNote(id, partialUpdateRequest)
        }
    }
}