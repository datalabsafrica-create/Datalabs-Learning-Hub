package com.example.data.model

data class CertificationReadiness(
  val totalLessons: Int,
  val completedLessons: Int,
  val lessonsComplete: Boolean,
  val moduleQuiz: Quiz?,
  val moduleQuizPassed: Boolean,
  val finalQuiz: Quiz?,
  val finalQuizPassed: Boolean,
  val requiredAssignment: Assignment?,
  val assignmentSubmitted: Boolean,
  val isEligible: Boolean,
  val blockingReasons: List<String>
)

/**
 * Pure certification eligibility policy shared across CourseDetailScreen,
 * LessonPlayerScreen, MainViewModel, and unit tests.
 */
object CourseCertificationPolicy {

  fun evaluateReadiness(
    lessons: List<Lesson>,
    completedLessonIds: Set<String>,
    quizzes: List<Quiz>,
    quizAttempts: List<QuizAttempt>,
    assignments: List<Assignment>,
    assignmentSubmissions: Map<String, AssignmentSubmission?>
  ): CertificationReadiness {
    val totalLessons = lessons.size
    val completedCount = lessons.count { completedLessonIds.contains(it.id) }
    val lessonsDone = totalLessons > 0 && completedCount >= totalLessons

    val moduleQuiz = quizzes.firstOrNull { it.assessmentType == "MODULE_QUIZ" }
    val moduleQuizPassed = moduleQuiz == null ||
      quizAttempts.any { it.quizId == moduleQuiz.id && it.passed }

    val finalQuiz = quizzes.firstOrNull { it.assessmentType == "FINAL_EXAM" }
      ?: quizzes.lastOrNull { it.id != moduleQuiz?.id }
    val finalQuizPassed = finalQuiz == null ||
      quizAttempts.any { it.quizId == finalQuiz.id && it.passed }

    val requiredAssignment = assignments.firstOrNull()
    val assignmentSubmitted = requiredAssignment == null ||
      assignmentSubmissions[requiredAssignment.id]?.status in setOf("SUBMITTED", "REVIEWED")

    val blocking = buildList {
      if (!lessonsDone) {
        add("Complete all $totalLessons curriculum lessons ($completedCount/$totalLessons finished)")
      }
      if (moduleQuiz != null && !moduleQuizPassed) {
        add("Pass ${moduleQuiz.title} (${moduleQuiz.passingScorePercent}%+ required)")
      }
      if (finalQuiz != null && !finalQuizPassed) {
        add("Pass ${finalQuiz.title} (${finalQuiz.passingScorePercent}%+ required)")
      }
      if (requiredAssignment != null && !assignmentSubmitted) {
        add("Submit Practical Assignment: ${requiredAssignment.title}")
      }
    }

    return CertificationReadiness(
      totalLessons = totalLessons,
      completedLessons = completedCount,
      lessonsComplete = lessonsDone,
      moduleQuiz = moduleQuiz,
      moduleQuizPassed = moduleQuizPassed,
      finalQuiz = finalQuiz,
      finalQuizPassed = finalQuizPassed,
      requiredAssignment = requiredAssignment,
      assignmentSubmitted = assignmentSubmitted,
      isEligible = blocking.isEmpty(),
      blockingReasons = blocking
    )
  }
}
