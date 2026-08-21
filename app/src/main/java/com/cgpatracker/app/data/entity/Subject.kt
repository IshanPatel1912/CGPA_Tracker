package com.cgpatracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subject_table",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["semesterId"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semesterId"])]
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val subjectId: Int = 0,
    val semesterId: Int,
    val subjectName: String,
    val credits: Int,

    // New Flag: Is this a Practical Subject?
    val isPractical: Boolean = false,

    // Marks
    val midSemMarks: Int,
    val internalMarks: Int,
    val endSemMarks: Int,
    val isAbsent: Boolean,

    // Results
    val finalMarks: Int,
    val grade: String,
    val gradePoint: Int
)