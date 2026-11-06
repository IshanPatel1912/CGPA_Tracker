package com.cgpatracker.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cgpatracker.app.data.entity.Semester
import com.cgpatracker.app.ui.SemesterAdapter
import com.cgpatracker.app.ui.SubjectActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SemesterAdapter
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Status bar matches light slate background
        window.statusBarColor = Color.parseColor("#F8FAFC")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        barChart = findViewById(R.id.chartSemesters)

        val recyclerView = findViewById<RecyclerView>(R.id.rvSemesters)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SemesterAdapter(
            onClick = { semester ->
                val intent = android.content.Intent(this, SubjectActivity::class.java)
                intent.putExtra("SEMESTER_ID", semester.semesterId)
                intent.putExtra("SEMESTER_NAME", semester.semesterName)
                startActivity(intent)
            },
            onEditClick = { semester -> showAddOrEditDialog(semester) },
            onDeleteClick = { semester -> showDeleteConfirmation(semester) }
        )
        recyclerView.adapter = adapter

        val fabAdd = findViewById<ExtendedFloatingActionButton>(R.id.fabAddSemester)
        fabAdd.setOnClickListener { showAddOrEditDialog(null) }

        viewModel.allSemesters.observe(this) { semesters ->
            adapter.submitList(semesters)

            if (semesters.isNotEmpty()) {
                var totalGpa = 0.0
                for (sem in semesters) {
                    totalGpa += sem.semesterGpa
                }
                val cgpa = totalGpa / semesters.size
                findViewById<TextView>(R.id.tvCgpaValue).text = String.format("%.2f", cgpa)
                updateSemesterChart(semesters)
            } else {
                findViewById<TextView>(R.id.tvCgpaValue).text = "0.00"
                barChart.clear()
            }
        }
    }

    private fun updateSemesterChart(semesters: List<Semester>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        for (i in semesters.indices) {
            val sem = semesters[i]
            entries.add(BarEntry(i.toFloat(), sem.semesterGpa.toFloat()))
            labels.add(sem.semesterName)

            when {
                sem.semesterGpa >= 8.0 -> colors.add(Color.parseColor("#10B981")) // Emerald
                sem.semesterGpa >= 6.0 -> colors.add(Color.parseColor("#F59E0B")) // Amber
                else -> colors.add(Color.parseColor("#EF4444")) // Rose
            }
        }

        val dataSet = BarDataSet(entries, "SGPA")
        dataSet.colors = colors
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#0F172A")

        val data = BarData(dataSet)
        data.barWidth = 0.45f

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(800)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.parseColor("#64748B")

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 10f
        barChart.axisLeft.textColor = Color.parseColor("#64748B")
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisLeft.gridColor = Color.parseColor("#E2E8F0")
        barChart.axisRight.isEnabled = false

        barChart.invalidate()
    }

    private fun showDeleteConfirmation(semester: Semester) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null)
        val builder = AlertDialog.Builder(this).setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btnCancelDelete).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnConfirmDelete).setOnClickListener {
            viewModel.deleteSemester(semester)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAddOrEditDialog(semester: Semester?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_semester, null)
        val builder = AlertDialog.Builder(this).setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etSemesterName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        if (semester != null) {
            tvTitle.text = "Edit Semester"
            etName.setText(semester.semesterName)
            btnSave.text = "Update"
        } else {
            tvTitle.text = "Add Semester"
            btnSave.text = "Save"
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            if (name.isNotEmpty()) {
                if (semester == null) {
                    viewModel.addSemester(name)
                } else {
                    viewModel.updateSemesterName(semester, name)
                }
                dialog.dismiss()
            } else {
                etName.error = "Please enter a name"
            }
        }
        dialog.show()
    }
}
