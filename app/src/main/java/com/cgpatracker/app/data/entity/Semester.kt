package com.cgpatracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semester_table")
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val semesterId: Int = 0,
    val semesterName: String, // e.g., "Semester 1"
    val semesterGpa: Double = 0.0
)