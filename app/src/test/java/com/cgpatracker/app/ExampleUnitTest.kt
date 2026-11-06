package com.cgpatracker.app

import com.cgpatracker.app.data.entity.Subject
import com.cgpatracker.app.utils.GradeCalculator
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {

    @Test
    fun testTheorySubjectEndSemTargets() {
        val subject = Subject(
            subjectId = 1,
            semesterId = 1,
            subjectName = "Physics",
            credits = 4,
            isPractical = false,
            midSemMarks = 20,
            internalMarks = 20,
            endSemMarks = 0,
            isAbsent = false,
            finalMarks = 40,
            grade = "P",
            gradePoint = 4
        )

        val targets = GradeCalculator.calculateEndSemTargets(subject, listOf(subject))
        assertEquals(7, targets.size)

        // Target O (80): needed = 2 * (80 - 40) = 80
        val targetO = targets.first { it.grade == "O" }
        assertEquals(80, targetO.requiredEndSem)
        assertTrue(targetO.isPossible)
        assertFalse(targetO.isPassingFloorApplied)
        assertEquals(10.0, targetO.projectedSgpa, 0.001)

        // Target A+ (70): needed = 2 * (70 - 40) = 60
        val targetAPlus = targets.first { it.grade == "A+" }
        assertEquals(60, targetAPlus.requiredEndSem)
        assertTrue(targetAPlus.isPossible)
        assertFalse(targetAPlus.isPassingFloorApplied)
        assertEquals(9.0, targetAPlus.projectedSgpa, 0.001)

        // Target P (40): needed = 2 * (40 - 40) = 0; min passing is 35
        val targetP = targets.first { it.grade == "P" }
        assertEquals(35, targetP.requiredEndSem)
        assertTrue(targetP.isPossible)
        assertTrue(targetP.isPassingFloorApplied)
        assertEquals(4.0, targetP.projectedSgpa, 0.001)
    }

    @Test
    fun testTheoryImpossibleGrade() {
        val subject = Subject(
            subjectId = 1,
            semesterId = 1,
            subjectName = "Maths",
            credits = 4,
            isPractical = false,
            midSemMarks = 5,
            internalMarks = 5,
            endSemMarks = 0,
            isAbsent = false,
            finalMarks = 10,
            grade = "F",
            gradePoint = 0
        )

        val targets = GradeCalculator.calculateEndSemTargets(subject, listOf(subject))
        // Target O (80): needed = 2 * (80 - 10) = 140 > 100
        val targetO = targets.first { it.grade == "O" }
        assertFalse(targetO.isPossible)
        assertEquals(140, targetO.requiredEndSem)
    }

    @Test
    fun testPracticalSubjectEndSemTargets() {
        val subject = Subject(
            subjectId = 1,
            semesterId = 1,
            subjectName = "Chemistry Lab",
            credits = 2,
            isPractical = true,
            midSemMarks = 0,
            internalMarks = 35,
            endSemMarks = 0,
            isAbsent = false,
            finalMarks = 35,
            grade = "F",
            gradePoint = 0
        )

        val targets = GradeCalculator.calculateEndSemTargets(subject, listOf(subject))

        // Target O (80): needed = 80 - 35 = 45 out of 50
        val targetO = targets.first { it.grade == "O" }
        assertEquals(45, targetO.requiredEndSem)
        assertTrue(targetO.isPossible)
        assertFalse(targetO.isPassingFloorApplied)

        // Target P (40): needed = 40 - 35 = 5; floor is 18
        val targetP = targets.first { it.grade == "P" }
        assertEquals(18, targetP.requiredEndSem)
        assertTrue(targetP.isPossible)
        assertTrue(targetP.isPassingFloorApplied)
    }

    @Test
    fun testProjectedSemesterSgpa() {
        val sub1 = Subject(
            subjectId = 1,
            semesterId = 1,
            subjectName = "Sub1",
            credits = 4,
            isPractical = false,
            midSemMarks = 20,
            internalMarks = 20,
            endSemMarks = 80,
            isAbsent = false,
            finalMarks = 80,
            grade = "O",
            gradePoint = 10
        )
        val sub2 = Subject(
            subjectId = 2,
            semesterId = 1,
            subjectName = "Sub2",
            credits = 4,
            isPractical = false,
            midSemMarks = 20,
            internalMarks = 20,
            endSemMarks = 60,
            isAbsent = false,
            finalMarks = 70,
            grade = "A+",
            gradePoint = 9
        )

        // Check targets for sub2 in a 2-subject semester
        val targets = GradeCalculator.calculateEndSemTargets(sub2, listOf(sub1, sub2))
        // If sub2 gets grade A (grade point 8):
        // Total points = (10 * 4) + (8 * 4) = 40 + 32 = 72
        // Total credits = 8
        // SGPA = 72 / 8 = 9.0
        val targetA = targets.first { it.grade == "A" }
        assertEquals(9.0, targetA.projectedSgpa, 0.001)

        // If sub2 gets grade B (grade point 6):
        // Total points = 40 + 24 = 64
        // SGPA = 64 / 8 = 8.0
        val targetB = targets.first { it.grade == "B" }
        assertEquals(8.0, targetB.projectedSgpa, 0.001)
    }
}
