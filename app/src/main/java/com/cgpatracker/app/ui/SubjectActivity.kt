package com.cgpatracker.app.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cgpatracker.app.MainViewModel
import com.cgpatracker.app.R
import com.cgpatracker.app.data.entity.Subject
import com.cgpatracker.app.utils.GradeCalculator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class SubjectActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SubjectAdapter
    private lateinit var pieChart: PieChart
    private var semesterId: Int = 0
    private var semesterName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        // --- FIX STATUS BAR COLOR (Purple Background + White Icons) ---
        window.statusBarColor = Color.parseColor("#6200EE") // Matches Header
        // Make Time/Battery icons WHITE (because background is dark)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        semesterId = intent.getIntExtra("SEMESTER_ID", 0)
        semesterName = intent.getStringExtra("SEMESTER_NAME") ?: "Semester"

        findViewById<TextView>(R.id.tvSemesterTitle).text = semesterName

        pieChart = findViewById(R.id.barChart)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.rvSubjects)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SubjectAdapter(
            onEditClick = { subject -> showAddOrEditSubjectDialog(subject) },
            onDeleteClick = { subject ->
                viewModel.deleteSubject(subject)
                Toast.makeText(this, "Deleted ${subject.subjectName}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        viewModel.getSubjectsForSemester(semesterId).observe(this) { subjects ->
            adapter.submitList(subjects)
            calculateSemesterGPA(subjects)
            updatePieChart(subjects)
        }

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddSubject).setOnClickListener {
            showAddOrEditSubjectDialog(null)
        }
    }

    private fun showAddOrEditSubjectDialog(subject: Subject?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_subject, null)
        val builder = AlertDialog.Builder(this).setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val rbTheory = dialogView.findViewById<RadioButton>(R.id.rbTheory)
        val rbPractical = dialogView.findViewById<RadioButton>(R.id.rbPractical)
        val layoutTheory = dialogView.findViewById<LinearLayout>(R.id.layoutTheoryMarks)
        val layoutPractical = dialogView.findViewById<LinearLayout>(R.id.layoutPracticalMarks)
        val tvHint = dialogView.findViewById<TextView>(R.id.tvMarksHint)
        val etEndSem = dialogView.findViewById<TextInputEditText>(R.id.etEndSem)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etSubjectName)
        val etCredits = dialogView.findViewById<TextInputEditText>(R.id.etCredits)
        val etMid = dialogView.findViewById<TextInputEditText>(R.id.etMidSem)
        val etInternal = dialogView.findViewById<TextInputEditText>(R.id.etInternal)
        val etInternalPrac = dialogView.findViewById<TextInputEditText>(R.id.etInternalPractical)
        val cbAbsent = dialogView.findViewById<CheckBox>(R.id.cbAbsent)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveSubject)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelSubject)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        rbTheory.setOnClickListener {
            layoutTheory.visibility = View.VISIBLE
            layoutPractical.visibility = View.GONE
            etEndSem.hint = "End Sem Exam (100)"
            tvHint.text = "*Theory: End Sem (100) / 2 = 50"
        }

        rbPractical.setOnClickListener {
            layoutTheory.visibility = View.GONE
            layoutPractical.visibility = View.VISIBLE
            etEndSem.hint = "End Sem Exam (50)"
            tvHint.text = "*Practical: Internal (50) + End Sem (50) = 100"
        }

        if (subject != null) {
            tvTitle.text = "Edit Subject"
            etName.setText(subject.subjectName)
            etCredits.setText(subject.credits.toString())
            etEndSem.setText(subject.endSemMarks.toString())
            cbAbsent.isChecked = subject.isAbsent
            btnSave.text = "Update"

            if (subject.isPractical) {
                rbPractical.isChecked = true
                rbPractical.performClick()
                etInternalPrac.setText(subject.internalMarks.toString())
            } else {
                rbTheory.isChecked = true
                rbTheory.performClick()
                etMid.setText(subject.midSemMarks.toString())
                etInternal.setText(subject.internalMarks.toString())
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val credits = etCredits.text.toString().toIntOrNull() ?: 0
            val isPractical = rbPractical.isChecked
            val isAbsent = cbAbsent.isChecked
            val end = etEndSem.text.toString().toIntOrNull() ?: 0

            var mid = 0
            var internal = 0

            if (isPractical) {
                internal = etInternalPrac.text.toString().toIntOrNull() ?: 0
            } else {
                mid = etMid.text.toString().toIntOrNull() ?: 0
                internal = etInternal.text.toString().toIntOrNull() ?: 0
            }

            if (name.isNotEmpty() && credits > 0) {
                val result = GradeCalculator.calculateSubjectResult(mid, internal, end, isAbsent, isPractical)

                val newSubject = Subject(
                    subjectId = subject?.subjectId ?: 0,
                    semesterId = semesterId,
                    subjectName = name,
                    credits = credits,
                    isPractical = isPractical,
                    midSemMarks = mid,
                    internalMarks = internal,
                    endSemMarks = end,
                    isAbsent = isAbsent,
                    finalMarks = result.first,
                    grade = result.second,
                    gradePoint = result.third
                )

                if (subject == null) {
                    viewModel.addSubject(newSubject)
                } else {
                    viewModel.updateSubject(newSubject)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter Name and Credits", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun updatePieChart(subjects: List<Subject>) {
        if (subjects.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = ""
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val colorPalette = listOf(
            Color.parseColor("#EF5350"), Color.parseColor("#42A5F5"), Color.parseColor("#66BB6A"),
            Color.parseColor("#FFA726"), Color.parseColor("#AB47BC"), Color.parseColor("#26C6DA"),
            Color.parseColor("#EC407A"), Color.parseColor("#7E57C2"), Color.parseColor("#D4E157"),
            Color.parseColor("#8D6E63")
        )

        for (i in subjects.indices) {
            val sub = subjects[i]
            entries.add(PieEntry(sub.gradePoint.toFloat(), sub.subjectName))
            val colorIndex = i % colorPalette.size
            colors.add(colorPalette[colorIndex])
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data

        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.holeRadius = 58f

        pieChart.setCenterTextSize(18f)
        pieChart.setCenterTextColor(Color.parseColor("#6200EE"))
        pieChart.centerText = "SGPA"

        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)

        val l = pieChart.legend
        l.isEnabled = true
        l.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        l.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        l.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        l.setDrawInside(false)
        l.isWordWrapEnabled = true
        l.textSize = 12f
        l.xEntrySpace = 10f
        l.yEntrySpace = 5f

        pieChart.animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        pieChart.invalidate()
    }

    private fun calculateSemesterGPA(subjects: List<Subject>) {
        if (subjects.isEmpty()) {
            findViewById<TextView>(R.id.tvSemesterGpa).text = "SGPA: 0.00"
            pieChart.centerText = "0.00"
            viewModel.updateSemesterGpa(semesterId, 0.0)
            return
        }

        var totalPoints = 0.0
        var totalCredits = 0

        for (sub in subjects) {
            totalPoints += (sub.gradePoint * sub.credits)
            totalCredits += sub.credits
        }

        val sgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val formattedSgpa = String.format("%.2f", sgpa)

        findViewById<TextView>(R.id.tvSemesterGpa).text = "SGPA: $formattedSgpa"
        pieChart.centerText = "SGPA\n$formattedSgpa"
        viewModel.updateSemesterGpa(semesterId, sgpa)
    }
}