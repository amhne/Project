package com.example.project.utils

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.project.NoteActivity
import com.example.project.model.Note
import com.example.project.R
import com.example.project.data.NoteEntity

class NotesAdapter : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val notes = mutableListOf<Note>()

    class NotesDiffCallback(
        private val oldList: List<Note>,
        private val newList: List<Note>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    fun setNotes(newNotes: List<Note>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setNotesFromEntities(noteEntities: List<NoteEntity>) {
        val notes = noteEntities.map { entity ->
            Note(
                id = entity.serverId ?: -entity.localId,
                title = entity.title,
                description = entity.description,
                createdAt = entity.createdAt ?: "",
                updatedAt = entity.updatedAt ?: "",
                creatorName = entity.creatorName ?: "",
                creatorUsername = entity.creatorUsername ?: ""
            )
        }
        setNotes(notes)
    }

    fun appendNotes(newNotes: List<Note>) {
        val newList = ArrayList(notes)
        newList.addAll(newNotes)
        val diffCallback = NotesDiffCallback(notes, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun appendNotesFromEntities(noteEntities: List<NoteEntity>) {
        val newNotes = noteEntities.map { entity ->
            Note(
                id = entity.serverId ?: -entity.localId,
                title = entity.title,
                description = entity.description,
                createdAt = entity.createdAt ?: "",
                updatedAt = entity.updatedAt ?: "",
                creatorName = entity.creatorName ?: "",
                creatorUsername = entity.creatorUsername ?: ""
            )
        }
        appendNotes(newNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun getItemCount(): Int = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position], position)
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.title)
        private val descriptionTv: TextView = itemView.findViewById(R.id.description)

        fun bind(note: Note, position: Int) {
            titleTv.text = note.title

            val desc = note.description
            val preview = if (desc.length > 50) {
                desc.substring(0, 50) + "..."
            } else {
                desc
            }
            descriptionTv.text = preview

            val bgRes = if (position % 2 == 0) R.drawable.note_light_bg else R.drawable.note_dark_bg
            itemView.setBackgroundResource(bgRes)

            itemView.setOnClickListener {
                val context = it.context
                val intent = Intent(context, NoteActivity::class.java)
                intent.putExtra("noteId", note.id)
                context.startActivity(intent)
            }
        }
    }
}
