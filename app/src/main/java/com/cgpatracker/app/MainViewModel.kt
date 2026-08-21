package com.cgpatracker.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.cgpatracker.app.data.AppDatabase
import com.cgpatracker.app.data.entity.Semester
import com.cgpatracker.app.data.entity.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).appDao()

    val allSemesters: LiveData<List<Semester>> = dao.getAllSemesters()

    // --- Semester Actions ---
    fun addSemester(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSemester(Semester(semesterName = name))
        }
    }

    fun updateSemesterName(semester: Semester, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSemester(semester.copy(semesterName = newName))
        }
    }

    fun updateSemesterGpa(id: Int, gpa: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSemesterGpa(id, gpa)
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSemester(semester)
        }
    }

    // --- Subject Actions ---
    fun addSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSubject(subject)
        }
    }

    // This fixes the EDIT feature
    fun updateSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSubject(subject)
        }
    }

    // This fixes the DELETE CRASH
    fun deleteSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSubject(subject)
        }
    }

    fun getSubjectsForSemester(semId: Int): LiveData<List<Subject>> {
        return dao.getSubjectsForSemester(semId)
    }
}