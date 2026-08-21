package com.cgpatracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cgpatracker.app.data.dao.AppDao
import com.cgpatracker.app.data.entity.Semester
import com.cgpatracker.app.data.entity.Subject

@Database(entities = [Semester::class, Subject::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cgpa_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}