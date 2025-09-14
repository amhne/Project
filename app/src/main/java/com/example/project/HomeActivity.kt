package com.example.project

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.project.auth.ApiClient
import com.example.project.auth.TokenApp
import com.example.project.auth.TokenManager
import com.example.project.data.DatabaseProvider
import com.example.project.utils.GridSpacingItemDecoration
import com.example.project.utils.NotesAdapter
import com.example.project.utils.parseApiErrorMessage
import com.example.project.utils.scaleViewsToScreen
import com.example.project.model.NotesResponse
import com.example.project.databinding.ActivityHomeBinding
import com.example.project.model.CreateRequest
import com.example.project.network.NoteApi
import com.example.project.model.UpdateRequest
import com.example.project.utils.windowInsetsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


class HomeActivity : AppCompatActivity() {
    private lateinit var tokenManager: TokenManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isLoading = false
    private var hasError = false
    private var currentTitle: String? = null
    private var nextPageUrl: String? = null
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
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

        val searchView = binding.searchEditText

        binding.centerContainer.setOnClickListener {
            val intent = Intent(this, NewNoteActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.clickRight.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("count", 1)
            startActivity(intent)
        }

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        val spacingInPixels = (16 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels, true))
        notesAdapter = NotesAdapter()
        recyclerView.adapter = notesAdapter

        swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            syncNotes()
            syncDeletes()
            syncUpdates()
            if (hasError) {
                fetchNotes(null, currentTitle, 0)
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        searchView.isFocusable = true
        searchView.requestFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentTitle = query
                nextPageUrl = null
                notesAdapter.setNotes(emptyList())
                fetchNotes(null, currentTitle, 0)
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    fetchNotes(null, null,0)
                } else {
                    return true
                }
                return true
            }
        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val layoutManager = rv.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (!isLoading && nextPageUrl != null) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 6
                    ) {
                        fetchNotes(nextPageUrl, currentTitle, currentPage)
                    }
                } else {
                    currentPage++
                    fetchNotes(nextPageUrl, currentTitle, currentPage)
                }
            }
        })
        fetchNotes(null, currentTitle, 0)
    }

    override fun onStart() {
        super.onStart()
        syncNotes()
        syncDeletes()
        syncUpdates()
    }

    private fun syncNotes() {
        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@HomeActivity).noteDao()
            val api = ApiClient.getInstance(applicationContext).create(NoteApi::class.java)

            val unsyncedNotes = noteDao.getUnsyncedNotes()
            if (unsyncedNotes.isEmpty()) return@launch

            val createRequests = unsyncedNotes.map {
                CreateRequest(it.title, it.description)
            }

            try {
                val response = api.bulkCreate(createRequests)
                if (response.isSuccessful) {
                    val responseNotes = response.body() ?: emptyList()
                    responseNotes.forEachIndexed { index, serverNote ->
                        val localNote = unsyncedNotes.getOrNull(index) ?: return@forEachIndexed
                        val updatedNote = localNote.copy(
                            serverId = serverNote.id,
                            createdAt = serverNote.createdAt,
                            updatedAt = serverNote.updatedAt,
                            creatorName = serverNote.creatorName,
                            creatorUsername = serverNote.creatorUsername,
                            isSynced = true
                        )
                        noteDao.update(updatedNote)
                    }
                } else {
                    val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                    Toast.makeText(this@HomeActivity, "Failed to sync notes with server: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {}
        }
    }

    private fun syncDeletes() {
        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@HomeActivity).noteDao()
            val api = ApiClient.getInstance(this@HomeActivity).create(NoteApi::class.java)
            val pendingDeletes = withContext(Dispatchers.IO){
                noteDao.getPendingDeletes()
            }
            for (note in pendingDeletes) {
                try {
                    val serverId = note.serverId ?: continue
                    val response = api.deleteNote(serverId)
                    if (response.isSuccessful) {
                        withContext(Dispatchers.IO) {
                            noteDao.delete(note)
                        }
                    }
                    else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@HomeActivity, "Failed to sync notes with server: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun syncUpdates() {
        lifecycleScope.launch {
            val noteDao = DatabaseProvider.getDatabase(this@HomeActivity).noteDao()
            val api = ApiClient.getInstance(applicationContext).create(NoteApi::class.java)

            val unsyncedUpdates = noteDao.getUnsyncedUpdates()
            if (unsyncedUpdates.isEmpty()) return@launch

            for (note in unsyncedUpdates) {
                try {
                    val updateRequest = UpdateRequest(
                        title = note.title,
                        description = note.description
                    )
                    val response = api.updateNote(note.serverId!!, updateRequest)
                    if (response.isSuccessful) {
                        val updatedNote = note.copy(isSynced = true)
                        noteDao.update(updatedNote)
                    }
                    else {
                        val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                        Toast.makeText(this@HomeActivity, "Failed to sync notes with server: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {}
            }
        }
    }


    private fun fetchNotes(nextUrl: String?, title: String?, currentPage: Int, pageSize: Int = 6) {
        if (isLoading) return

        isLoading = true
        hasError = false
        swipeRefreshLayout.isRefreshing = true

        val notesApi = ApiClient.getInstance(this).create(NoteApi::class.java)
        val noteDao = DatabaseProvider.getDatabase(this@HomeActivity).noteDao()

        lifecycleScope.launch {
            try {
                val response: Response<NotesResponse> = withContext(Dispatchers.IO) {
                    when {
                        nextUrl != null -> notesApi.getNotesByUrl(nextUrl)
                        title.isNullOrEmpty() -> notesApi.getNotes(page = 1, pageSize = pageSize)
                        else -> notesApi.getFilteredNotes(page = 1, pageSize = pageSize, title = title)
                    }
                }

                isLoading = false
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    hasError = false
                    val notes = response.body()!!.results

                    if (nextUrl == null) {
                        notesAdapter.setNotes(notes)
                    } else {
                        notesAdapter.appendNotes(notes)
                    }

                    nextPageUrl = response.body()!!.next
                } else {
                    hasError = true
                    val errorMsg = parseApiErrorMessage(response.errorBody()?.string())
                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val username = sharedPref.getString("username", null)

                val offset = currentPage * pageSize
                val localNotes = withContext(Dispatchers.IO) {
                    if (title.isNullOrEmpty()) {
                        noteDao.getActiveNotes(pageSize, offset, username)
                    } else {
                        noteDao.getNotesFilteredByTitle(title, pageSize, offset, username)
                    }
                }
                isLoading = false
                swipeRefreshLayout.isRefreshing = false

                if (localNotes.isNotEmpty()) {
                    hasError = false
                    if (currentPage == 0) {
                        notesAdapter.setNotesFromEntities(localNotes)
                    } else {
                        notesAdapter.appendNotesFromEntities(localNotes)
                    }
                } else {
                    hasError = true
                }
            }
        }
    }
}
