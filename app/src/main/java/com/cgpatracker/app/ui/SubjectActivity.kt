package com.cgpatracker.app.ui

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cgpatracker.app.MainViewModel
import com.cgpatracker.app.R
import com.cgpatracker.app.data.entity.Subject
import com.cgpatracker.app.utils.GradeCalculator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class SubjectActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SubjectAdapter
    private lateinit var pieChart: PieChart
    private var semesterId: Int = 0
    private var semesterName: String = ""
    private var currentSubjectsList: List<Subject> = emptyList()

    private val modernPalette = listOf(
        Color.parseColor("#4F46E5"), // Indigo
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#10B981"), // Emerald
        Color.parseColor("#F59E0B"), // Amber
        Color.parseColor("#8B5CF6"), // Violet
        Color.parseColor("#EC4899"), // Pink
        Color.parseColor("#3B82F6"), // Blue
        Color.parseColor("#14B8A6"), // Teal
        Color.parseColor("#F97316"), // Orange
        Color.parseColor("#6366F1")  // Light Indigo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        // Minimal Light Status Bar
        window.statusBarColor = Color.parseColor("#F8FAFC")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        semesterId = intent.getIntExtra("SEMESTER_ID", 0)
        semesterName = intent.getStringExtra("SEMESTER_NAME") ?: "Semester"

        findViewById<TextView>(R.id.tvSemesterTitle).text = semesterName

        // Back button navigation
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        pieChart = findViewById(R.id.barChart)
        setupPieChart()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.rvSubjects)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SubjectAdapter(
            onTargetClick = { subject -> showTargetMarksBottomSheet(subject) },
            onEditClick = { subject -> showAddOrEditSubjectDialog(subject) },
            onDeleteClick = { subject ->
                viewModel.deleteSubject(subject)
                Toast.makeText(this, "Deleted ${subject.subjectName}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        // Setup swipe left to open End Sem Target Table
        setupSwipeToTargetHelper(recyclerView)

        val tvSwipeHint = findViewById<TextView>(R.id.tvSwipeHint)

        viewModel.getSubjectsForSemester(semesterId).observe(this) { subjects ->
            currentSubjectsList = subjects
            adapter.submitList(subjects)
            calculateSemesterGPA(subjects)
            updatePieChart(subjects)
            tvSwipeHint.visibility = if (subjects.isNotEmpty()) View.VISIBLE else View.GONE
        }

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddSubject).setOnClickListener {
            showAddOrEditSubjectDialog(null)
        }
    }

    private fun setupPieChart() {
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 70f
        pieChart.holeRadius = 65f
        pieChart.setTransparentCircleColor(Color.parseColor("#F1F5F9"))
        pieChart.setTransparentCircleAlpha(120)

        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.parseColor("#0F172A"))

        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.isWordWrapEnabled = true
        legend.textSize = 11f
        legend.textColor = Color.parseColor("#64748B")
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 8f
        legend.xEntrySpace = 12f
        legend.yEntrySpace = 6f

        // Interactive slice selection listener
        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val sub = e?.data as? Subject
                if (sub != null) {
                    val color = (e as? PieEntry)?.let {
                        val index = currentSubjectsList.indexOfFirst { it.subjectId == sub.subjectId }
                        if (index >= 0) modernPalette[index % modernPalette.size] else Color.parseColor("#4F46E5")
                    } ?: Color.parseColor("#4F46E5")
                    showSelectedSubjectDetail(sub, color)
                }
            }

            override fun onNothingSelected() {
                clearSelectedSubjectDetail()
            }
        })
    }

    private fun showSelectedSubjectDetail(sub: Subject, color: Int) {
        val totalCredits = currentSubjectsList.sumOf { it.credits }
        val totalPoints = currentSubjectsList.sumOf { it.gradePoint * it.credits.toDouble() }
        val subPoints = sub.gradePoint * sub.credits.toDouble()

        val pointPercentage = if (totalPoints > 0) (subPoints / totalPoints) * 100.0 else 0.0
        val sgpaContribution = if (totalCredits > 0) subPoints / totalCredits else 0.0

        // Update Center Text of Donut Chart
        pieChart.centerText = "${sub.subjectName}\n${sub.grade} (${sub.gradePoint}.0)\n${String.format("%.1f%%", pointPercentage)}"
        pieChart.setCenterTextSize(13f)

        // Show detailed inspection card
        findViewById<View>(R.id.tvChartDefaultPrompt).visibility = View.GONE
        findViewById<View>(R.id.layoutChartActiveDetail).visibility = View.VISIBLE

        findViewById<View>(R.id.viewSliceColorIndicator).backgroundTintList = ColorStateList.valueOf(color)
        findViewById<TextView>(R.id.tvSelectedSubjectTitle).text = sub.subjectName
        val typeStr = if (sub.isPractical) "Practical" else "Theory"
        findViewById<TextView>(R.id.tvSelectedSubjectType).text = "$typeStr • ${sub.credits} Credits"

        findViewById<TextView>(R.id.tvSelectedGradeValue).text = "${sub.grade} (${sub.gradePoint}.0)"
        findViewById<TextView>(R.id.tvSelectedMarksValue).text = "${sub.finalMarks} / 100"

        val contributionText = "+${String.format("%.2f", sgpaContribution)} (${String.format("%.1f", pointPercentage)}%)"
        val tvContrib = findViewById<TextView>(R.id.tvSelectedContributionValue)
        tvContrib.text = contributionText
        if (sub.gradePoint == 0) {
            tvContrib.setTextColor(Color.parseColor("#EF4444"))
        } else {
            tvContrib.setTextColor(Color.parseColor("#10B981"))
        }
    }

    private fun clearSelectedSubjectDetail() {
        val totalCredits = currentSubjectsList.sumOf { it.credits }
        val totalPoints = currentSubjectsList.sumOf { it.gradePoint * it.credits.toDouble() }
        val sgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val formattedSgpa = String.format("%.2f", sgpa)

        pieChart.centerText = "SGPA\n$formattedSgpa"
        pieChart.setCenterTextSize(16f)

        findViewById<View>(R.id.tvChartDefaultPrompt).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutChartActiveDetail).visibility = View.GONE
    }

    private fun setupSwipeToTargetHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                    val subject = adapter.currentList[position]
                    // Snap the swiped card back into place
                    adapter.notifyItemChanged(position)
                    showTargetMarksBottomSheet(subject)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                if (dX != 0f) {
                    val cornerRadius = 32f
                    val bgPaint = Paint().apply {
                        color = Color.parseColor("#4F46E5") // Indigo primary
                        isAntiAlias = true
                    }

                    if (dX > 0) {
                        c.drawRoundRect(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left.toFloat() + dX + cornerRadius,
                            itemView.bottom.toFloat(),
                            cornerRadius,
                            cornerRadius,
                            bgPaint
                        )
                    } else {
                        c.drawRoundRect(
                            itemView.right.toFloat() + dX - cornerRadius,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat(),
                            cornerRadius,
                            cornerRadius,
                            bgPaint
                        )
                    }

                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 34f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    val label = "🎯 End Sem Targets"
                    val bounds = Rect()
                    textPaint.getTextBounds(label, 0, label.length, bounds)
                    val textY = itemView.top + (itemView.height + bounds.height()) / 2f

                    if (dX > 120) {
                        val textX = itemView.left + 48f
                        c.drawText(label, textX, textY, textPaint)
                    } else if (dX < -120) {
                        val textX = itemView.right - bounds.width() - 48f
                        c.drawText(label, textX, textY, textPaint)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showTargetMarksBottomSheet(subject: Subject) {
        val dialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_target_marks, null)
        dialog.setContentView(sheetView)

        // Configure BottomSheetBehavior to enable swipe down to dismiss
        dialog.behavior.apply {
            isHideable = true
            isDraggable = true
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        sheetView.findViewById<TextView>(R.id.tvSheetSubtitle).text =
            "${subject.subjectName} • ${subject.credits} Credits"

        val tvTypeBadge = sheetView.findViewById<TextView>(R.id.tvSubjectTypeBadge)
        val tvBreakdown = sheetView.findViewById<TextView>(R.id.tvCurrentBreakdown)
        val tvRuleNote = sheetView.findViewById<TextView>(R.id.tvExamRuleNote)

        if (subject.isPractical) {
            tvTypeBadge.text = "PRACTICAL"
            tvTypeBadge.setTextColor(Color.parseColor("#059669"))
            tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
            tvBreakdown.text = "Internal Assessment: ${subject.internalMarks} / 50"
            tvRuleNote.text = "* Practical End Sem is out of 50. Minimum 18/50 required to pass."
        } else {
            tvTypeBadge.text = "THEORY"
            tvTypeBadge.setTextColor(Color.parseColor("#4F46E5"))
            tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF2FF"))
            val internalTotal = subject.midSemMarks + subject.internalMarks
            tvBreakdown.text = "Mid Sem: ${subject.midSemMarks}/25  |  Internal: ${subject.internalMarks}/25  (Internal Total: $internalTotal/50)"
            tvRuleNote.text = "* Theory End Sem is out of 100 (converted to 50). Minimum 35/100 required to pass."
        }

        val targets = GradeCalculator.calculateEndSemTargets(subject, currentSubjectsList)
        val rowsContainer = sheetView.findViewById<LinearLayout>(R.id.llTargetRows)
        rowsContainer.removeAllViews()

        for (target in targets) {
            val rowView = LayoutInflater.from(this).inflate(R.layout.item_target_row, rowsContainer, false)

            val tvBadge = rowView.findViewById<TextView>(R.id.tvTargetGradeBadge)
            val tvLabel = rowView.findViewById<TextView>(R.id.tvTargetGradeLabel)
            val tvPoint = rowView.findViewById<TextView>(R.id.tvTargetGradePoint)
            val tvRequired = rowView.findViewById<TextView>(R.id.tvRequiredMarks)
            val tvNote = rowView.findViewById<TextView>(R.id.tvRequiredNote)

            tvBadge.text = target.grade
            tvLabel.text = "Grade ${target.grade}"
            tvPoint.text = "${target.gradePoint}.0 Grade Point"

            val badgeColor = when (target.grade) {
                "O" -> Color.parseColor("#4F46E5")
                "A+" -> Color.parseColor("#059669")
                "A" -> Color.parseColor("#10B981")
                "B+" -> Color.parseColor("#0284C7")
                "B" -> Color.parseColor("#0EA5E9")
                "C" -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#64748B")
            }
            tvBadge.backgroundTintList = ColorStateList.valueOf(badgeColor)

            if (target.isPossible) {
                val maxMarks = target.maxEndSemMarks
                tvRequired.text = "≥ ${target.requiredEndSem} / $maxMarks"
                tvRequired.setTextColor(Color.parseColor("#059669"))

                if (target.isPassingFloorApplied) {
                    tvNote.visibility = View.VISIBLE
                    tvNote.text = "Min pass cutoff applied"
                } else {
                    tvNote.visibility = View.GONE
                }
            } else {
                tvRequired.text = "Not Possible"
                tvRequired.setTextColor(Color.parseColor("#EF4444"))
                tvNote.visibility = View.VISIBLE
                tvNote.text = "Needs > ${target.maxEndSemMarks} marks"
                tvNote.setTextColor(Color.parseColor("#EF4444"))
            }

            rowsContainer.addView(rowView)
        }

        // Dismiss listeners
        sheetView.findViewById<View>(R.id.btnCloseIcon).setOnClickListener { dialog.dismiss() }
        sheetView.findViewById<View>(R.id.btnCloseSheet).setOnClickListener { dialog.dismiss() }
        sheetView.findViewById<View>(R.id.dragHandle).setOnClickListener { dialog.dismiss() }

        dialog.show()
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
            clearSelectedSubjectDetail()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for (i in subjects.indices) {
            val sub = subjects[i]
            // Credit-weighted contribution for accurate visual representation
            val sliceValue = if (sub.gradePoint > 0) (sub.gradePoint * sub.credits).toFloat() else 0.5f
            entries.add(PieEntry(sliceValue, sub.subjectName, sub))
            val colorIndex = i % modernPalette.size
            colors.add(modernPalette[colorIndex])
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3.5f
        dataSet.selectionShift = 10f // Smooth outward shift when a slice is clicked
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data

        clearSelectedSubjectDetail()

        pieChart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        pieChart.invalidate()
    }

    private fun calculateSemesterGPA(subjects: List<Subject>) {
        if (subjects.isEmpty()) {
            findViewById<TextView>(R.id.tvSemesterGpa).text = "SGPA: 0.00"
            pieChart.centerText = "SGPA\n0.00"
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
        clearSelectedSubjectDetail()
        viewModel.updateSemesterGpa(semesterId, sgpa)
    }
}
