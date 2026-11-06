package com.cgpatracker.app.ui

import android.content.res.ColorStateList
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
    private val onTargetClick: (Subject) -> Unit, // View End Sem Targets
    private val onEditClick: (Subject) -> Unit,   // Edit Action
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
        private val btnTarget: ImageButton = itemView.findViewById(R.id.btnTargetSubject)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditSubject)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSubject)

        fun bind(subject: Subject) {
            val typeStr = if (subject.isPractical) "Practical" else "Theory"
            tvName.text = subject.subjectName
            tvDetails.text = "$typeStr • ${subject.credits} Credits • ${subject.finalMarks} Marks"
            tvGrade.text = subject.grade

            if (subject.grade == "F" || subject.grade == "NA") {
                tvGrade.setTextColor(Color.parseColor("#E11D48"))
                tvGrade.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF1F2"))
            } else {
                tvGrade.setTextColor(Color.parseColor("#059669"))
                tvGrade.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
            }

            // Click row or target button opens End Sem targets
            itemView.setOnClickListener { onTargetClick(subject) }
            btnTarget.setOnClickListener { onTargetClick(subject) }

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
