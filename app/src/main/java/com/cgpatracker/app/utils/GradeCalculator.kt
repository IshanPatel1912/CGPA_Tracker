package com.cgpatracker.app.utils

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
        isPractical: Boolean // New Parameter
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
            endSem < 18 // Strict rule for practicals (adjust if needed)
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
}