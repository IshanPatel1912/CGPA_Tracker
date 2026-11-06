package com.cgpatracker.app.utils

import com.cgpatracker.app.data.entity.Subject

data class TargetScoreInfo(
    val grade: String,                  // "O", "A+", "A", "B+", "B", "C", "P"
    val gradePoint: Int,               // 10, 9, 8, 7, 6, 5, 4
    val targetFinalMarks: Int,          // 80, 70, 60, 55, 50, 45, 40
    val requiredEndSem: Int,           // Minimum marks needed in End Sem exam
    val isPossible: Boolean,           // true if requiredEndSem <= maxEndSemMarks
    val isPassingFloorApplied: Boolean,// true if 35 (Theory) or 18 (Practical) was enforced because needed was lower
    val maxEndSemMarks: Int,           // 100 for Theory, 50 for Practical
    val projectedSgpa: Double          // Projected semester SGPA if this grade is achieved
)

object GradeCalculator {

    fun getGrade(finalMarks: Int): String {
        return when {
            finalMarks >= 80 -> "O"
            finalMarks in 70..79 -> "A+"
            finalMarks in 60..69 -> "A"
            finalMarks in 55..59 -> "B+"
            finalMarks in 50..54 -> "B"
            finalMarks in 45..49 -> "C"
            finalMarks in 40..44 -> "P"
            else -> "F"
        }
    }

    fun getGradePoint(grade: String): Int {
        return when (grade) {
            "O" -> 10
            "A+" -> 9
            "A" -> 8
            "B+" -> 7
            "B" -> 6
            "C" -> 5
            "P" -> 4
            "F", "NA" -> 0
            else -> 0
        }
    }

    fun calculateSubjectResult(
        midSem: Int,
        internal: Int,
        endSem: Int,
        isAbsent: Boolean,
        isPractical: Boolean
    ): Triple<Int, String, Int> {

        if (isAbsent) {
            return Triple(0, "NA", 0)
        }

        val finalMarks: Int

        if (isPractical) {
            // PRACTICAL RULE:
            // Internal (50) + End Sem (50) = 100 Total
            finalMarks = internal + endSem
        } else {
            // THEORY RULE:
            // Mid (25) + Internal (25) + End Sem (100 -> converted to 50)
            val endSemConverted = endSem / 2
            finalMarks = midSem + internal + endSemConverted
        }

        // FAIL RULES
        // 1. Practical: If End Sem < 18 (approx 35% of 50), Fail.
        // 2. Theory: If End Sem < 35 (out of 100), Fail.
        val isFailed = if (isPractical) {
            endSem < 18
        } else {
            endSem < 35
        }

        if (isFailed) {
            return Triple(finalMarks, "F", 0)
        }

        val grade = getGrade(finalMarks)
        val gradePoint = getGradePoint(grade)

        return Triple(finalMarks, grade, gradePoint)
    }

    /**
     * Calculates the minimum End Sem exam marks required to obtain each passing grade
     * (O, A+, A, B+, B, C, P), along with the resulting Semester SGPA.
     */
    fun calculateEndSemTargets(
        subject: Subject,
        allSubjectsInSemester: List<Subject>
    ): List<TargetScoreInfo> {
        val targets = listOf(
            Pair("O", 80),
            Pair("A+", 70),
            Pair("A", 60),
            Pair("B+", 55),
            Pair("B", 50),
            Pair("C", 45),
            Pair("P", 40)
        )

        val isPractical = subject.isPractical
        val maxEndSem = if (isPractical) 50 else 100
        val minPassingEndSem = if (isPractical) 18 else 35

        val totalCredits = allSubjectsInSemester.sumOf { it.credits }
        val otherPoints = allSubjectsInSemester
            .filter { it.subjectId != subject.subjectId }
            .sumOf { it.gradePoint * it.credits.toDouble() }

        return targets.map { (grade, targetMarks) ->
            val gp = getGradePoint(grade)
            val neededMarks: Int = if (isPractical) {
                targetMarks - subject.internalMarks
            } else {
                val internalSum = subject.midSemMarks + subject.internalMarks
                2 * (targetMarks - internalSum)
            }

            val requiredEndSem = maxOf(minPassingEndSem, neededMarks)
            val isPossible = requiredEndSem <= maxEndSem
            val isFloor = neededMarks < minPassingEndSem

            val projectedPoints = otherPoints + (gp * subject.credits)
            val projectedSgpa = if (totalCredits > 0) projectedPoints / totalCredits else gp.toDouble()

            TargetScoreInfo(
                grade = grade,
                gradePoint = gp,
                targetFinalMarks = targetMarks,
                requiredEndSem = requiredEndSem,
                isPossible = isPossible,
                isPassingFloorApplied = isFloor && isPossible,
                maxEndSemMarks = maxEndSem,
                projectedSgpa = projectedSgpa
            )
        }
    }
}
