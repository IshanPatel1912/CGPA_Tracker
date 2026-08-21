package com.cgpatracker.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cgpatracker.app.data.entity.Semester
import com.cgpatracker.app.data.entity.Subject

@Dao
interface AppDao {

    // --- Semester Operations ---
    @Insert
    suspend fun insertSemester(semester: Semester)

    @Query("SELECT * FROM semester_table ORDER BY semesterId ASC")
    fun getAllSemesters(): LiveData<List<Semester>>

    @Update
    suspend fun updateSemester(semester: Semester)

    @Query("UPDATE semester_table SET semesterGpa = :gpa WHERE semesterId = :id")
    suspend fun updateSemesterGpa(id: Int, gpa: Double)

    @Delete
    suspend fun deleteSemester(semester: Semester)

    // --- Subject Operations ---
    @Insert
    suspend fun insertSubject(subject: Subject)

    @Update
    suspend fun updateSubject(subject: Subject) // <--- ADDED THIS FOR EDITING

    @Query("SELECT * FROM subject_table WHERE semesterId = :semId")
    fun getSubjectsForSemester(semId: Int): LiveData<List<Subject>>

    @Delete
    suspend fun deleteSubject(subject: Subject)
}