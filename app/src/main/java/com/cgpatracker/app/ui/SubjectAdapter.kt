package com.cgpatracker.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cgpatracker.app.R
import com.cgpatracker.app.data.entity.Subject

class SubjectAdapter(
    private val onEditClick: (Subject) -> Unit,   // New Edit Action
    private val onDeleteClick: (Subject) -> Unit  // Delete Action
) : ListAdapter<Subject, SubjectAdapter.SubjectViewHolder>(SubjectComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvSubjectDetails)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGrade)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditSubject)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSubject)

        fun bind(subject: Subject) {
            tvName.text = subject.subjectName
            tvDetails.text = "Credits: ${subject.credits} | Marks: ${subject.finalMarks}"
            tvGrade.text = subject.grade

            if (subject.grade == "F" || subject.grade == "NA") {
                tvGrade.setTextColor(Color.RED)
            } else {
                tvGrade.setTextColor(Color.parseColor("#4CAF50"))
            }

            // Edit Click
            btnEdit.setOnClickListener { onEditClick(subject) }

            // Delete Click
            btnDelete.setOnClickListener { onDeleteClick(subject) }
        }
    }

    class SubjectComparator : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject) = oldItem.subjectId == newItem.subjectId
        override fun areContentsTheSame(oldItem: Subject, newItem: Subject) = oldItem == newItem
    }
}