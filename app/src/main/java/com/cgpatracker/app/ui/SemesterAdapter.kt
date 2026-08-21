package com.cgpatracker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cgpatracker.app.R
import com.cgpatracker.app.data.entity.Semester

class SemesterAdapter(
    private val onClick: (Semester) -> Unit,       // Click on the card (to open subjects)
    private val onEditClick: (Semester) -> Unit,   // Click on Edit Button
    private val onDeleteClick: (Semester) -> Unit  // Click on Delete Button
) : ListAdapter<Semester, SemesterAdapter.SemesterViewHolder>(SemesterComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemesterViewHolder {
        // Here we use our new custom layout "item_semester"
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_semester, parent, false)
        return SemesterViewHolder(view)
    }

    override fun onBindViewHolder(holder: SemesterViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    inner class SemesterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSemName)
        private val tvGpa: TextView = itemView.findViewById(R.id.tvSemGpa)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(semester: Semester) {
            tvName.text = semester.semesterName
            tvGpa.text = "GPA: ${semester.semesterGpa}"

            // 1. Click on the whole row -> Open Subject Screen
            itemView.setOnClickListener {
                onClick(semester)
            }

            // 2. Click on Edit Button
            btnEdit.setOnClickListener {
                onEditClick(semester)
            }

            // 3. Click on Delete Button
            btnDelete.setOnClickListener {
                onDeleteClick(semester)
            }
        }
    }

    class SemesterComparator : DiffUtil.ItemCallback<Semester>() {
        override fun areItemsTheSame(oldItem: Semester, newItem: Semester) = oldItem.semesterId == newItem.semesterId
        override fun areContentsTheSame(oldItem: Semester, newItem: Semester) = oldItem == newItem
    }
}