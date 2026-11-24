package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val role: String = "STUDENT",
  val careerInterests: List<String> = emptyList(),
  val learningInterests: List<String> = emptyList(),
  val learningGoal: String = "",
  val weeklyTargetHours: Long = 5L,
  val streakDays: Long = 0L,
  val totalLearningMinutes: Long = 0L,
  val lastActivityDate: String? = null,
  val remindersEnabled: Boolean = true,
  val courseUpdatesEnabled: Boolean = true,
  val announcementsEnabled: Boolean = true,
  val achievementsEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Academy(
  @DocumentId val id: String = "",
  val name: String = "",
  val shortName: String = "",
  val description: String = "",
  val iconKey: String = "school",
  val accentHex: String = "#2563EB",
  val coverUrl: String? = null,
  val displayOrder: Long = 0L,
  val courseCount: Long = 0L,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Instructor(
  @DocumentId val id: String = "",
  val fullName: String = "",
  val title: String = "",
  val bio: String = "",
  val specialty: String = "",
  val avatarUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Course(
  @DocumentId val id: String = "",
  val academyId: String = "",
  val academyName: String = "",
  val title: String = "",
  val subtitle: String = "",
  val description: String = "",
  val instructorId: String = "",
  val instructorName: String = "",
  val level: String = "BEGINNER",
  val durationMinutes: Long = 120L,
  val lessonCount: Long = 0L,
  val accessTier: String = "FREE",
  val priceUsd: Double = 0.0,
  val priceUgx: Long = 0L,
  val certificateAvailable: Boolean = true,
  val published: Boolean = true,
  val featured: Boolean = false,
  val enrollmentCount: Long = 0L,
  val popularityScore: Long = 0L,
  val whatYouWillLearn: List<String> = emptyList(),
  val prerequisites: List<String> = emptyList(),
  val formats: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val category: String = "",
  val categories: List<String> = emptyList(),
  val moduleCount: Long = 4L,
  val coverUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val premiumOnly: Boolean get() = accessTier.equals("PREMIUM", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class Lesson(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val academyId: String = "",
  val moduleTitle: String = "",
  val title: String = "",
  val summary: String = "",
  val orderIndex: Long = 0L,
  val durationMinutes: Long = 15L,
  val format: String = "WRITTEN",
  val freePreview: Boolean = true,
  val premiumOnly: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LessonContent(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val freePreview: Boolean = true,
  val premiumOnly: Boolean = false,
  val notesMarkdown: String = "",
  val videoUrl: String? = null,
  val audioUrl: String? = null,
  val slidesText: String? = null,
  val downloadTitle: String? = null,
  val downloadUrl: String? = null,
  val downloadContent: String? = null,
  val exercisePrompt: String? = null,
  val keyTakeaways: List<String> = emptyList(),
  val caseStudyText: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Enrollment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val academyId: String = "",
  val courseTitle: String = "",
  val status: String = "ACTIVE",
  val progressPercent: Long = 0L,
  val completedLessonsCount: Long = 0L,
  val totalLessonsCount: Long = 0L,
  val lastLessonId: String? = null,
  val lastLessonTitle: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  val completedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LessonProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val lessonId: String = "",
  val completed: Boolean = false,
  val bookmarked: Boolean = false,
  val lastPositionSeconds: Long = 0L,
  val playbackSpeed: Double = 1.0,
  val studentNotes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningPath(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val academyId: String = "",
  val targetRole: String = "",
  val courseIds: List<String> = emptyList(),
  val courseTitles: List<String> = emptyList(),
  val estimatedHours: Long = 10L,
  val premiumOnly: Boolean = false,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningPathProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val pathId: String = "",
  val pathTitle: String = "",
  val completedCourseIds: List<String> = emptyList(),
  val progressPercent: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Serializable
data class QuizQuestion(
  val id: String = "",
  val type: String = "MULTIPLE_CHOICE", // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER
  val prompt: String = "",
  val options: List<String> = emptyList(),
  val correctAnswer: String = "",
  val explanation: String = "",
  val points: Int = 10
)

@Keep
@IgnoreExtraProperties
data class Quiz(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val lessonId: String? = null,
  val assessmentType: String = "FINAL_EXAM", // MODULE_QUIZ, FINAL_EXAM
  val moduleIndex: Long = 0L,
  val title: String = "",
  val description: String = "",
  val passingScorePercent: Long = 70L,
  val maxAttempts: Long = 5L,
  val allowRetakes: Boolean = true,
  val questionsJson: String = "[]",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
  @DocumentId val id: String = "",
  val userId: String = "",
  val quizId: String = "",
  val courseId: String = "",
  val scorePercent: Long = 0L,
  val earnedPoints: Long = 0L,
  val totalPoints: Long = 100L,
  val passed: Boolean = false,
  val attemptNumber: Long = 1L,
  val answersSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Assignment(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val title: String = "",
  val instructions: String = "",
  val deliverableFormat: String = "Written Analysis / Link",
  val maxPoints: Long = 100L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AssignmentSubmission(
  @DocumentId val id: String = "",
  val userId: String = "",
  val assignmentId: String = "",
  val courseId: String = "",
  val submissionText: String = "",
  val attachmentUrl: String? = null,
  val status: String = "SUBMITTED",
  val feedback: String? = null,
  val score: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Certificate(
  @DocumentId val id: String = "",
  val certificateCode: String = "",
  val userId: String = "",
  val studentName: String = "",
  val courseId: String = "",
  val courseTitle: String = "",
  val academyName: String = "",
  val issuedDateText: String = "",
  val verificationUrl: String = "",
  val status: String = "VALID",
  val issuedBy: String = "SYSTEM_COMPLETION",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
  @DocumentId val id: String = "",
  val name: String = "",
  val billingCycle: String = "MONTHLY",
  val priceUsd: Double = 0.0,
  val priceUgx: Long = 0L,
  val description: String = "",
  val features: List<String> = emptyList(),
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subscription(
  @DocumentId val id: String = "",
  val userId: String = "",
  val planId: String = "",
  val planName: String = "",
  val billingCycle: String = "NONE",
  val currency: String = "USD",
  val amount: Double = 0.0,
  val status: String = "EXPIRED",
  val paymentProvider: String = "NONE",
  val expiresAtText: String? = null,
  val productId: String? = null,
  val purchaseToken: String? = null,
  val autoRenewing: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val activeEntitlement: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class CoursePurchase(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val courseTitle: String = "",
  val amount: Double = 0.0,
  val currency: String = "USD",
  val paymentProvider: String = "GOOGLE_PLAY",
  val productId: String? = null,
  val purchaseToken: String? = null,
  val status: String = "PENDING_PAYMENT", // ACTIVE, PENDING_PAYMENT, REVOKED
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val activeUnlock: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class SavedCourse(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val courseTitle: String = "",
  val academyId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val type: String = "REMINDER",
  val targetRoute: String? = null,
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
  @DocumentId val id: String = "",
  val title: String = "",
  val message: String = "",
  val category: String = "Platform",
  val academyId: String? = null,
  val courseId: String? = null,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class AchievementBadge(
  val id: String,
  val title: String,
  val description: String,
  val category: String,
  val unlocked: Boolean,
  val progressLabel: String
)

object AchievementsCalculator {
  fun computeBadges(
    profile: UserProfile?,
    enrollments: List<Enrollment>,
    lessonProgress: List<LessonProgress>,
    quizAttempts: List<QuizAttempt>,
    certificates: List<Certificate>
  ): List<AchievementBadge> {
    val completedLessons = lessonProgress.count { it.completed }
    val completedCourses = enrollments.count { it.status == "COMPLETED" }
    val distinctCourses = enrollments.map { it.courseId }.distinct().size
    val passedQuizzes = quizAttempts.count { it.passed }
    val highScoreCount = quizAttempts.count { it.passed && it.scorePercent >= 85L }
    val streak = profile?.streakDays ?: 0L
    val certs = certificates.size

    return listOf(
      AchievementBadge(
        id = "first_lesson",
        title = "First Step",
        description = "Complete your first structured lesson",
        category = "Momentum",
        unlocked = completedLessons >= 1,
        progressLabel = "${completedLessons.coerceAtMost(1)}/1 lesson"
      ),
      AchievementBadge(
        id = "lesson_streak_5",
        title = "Focused Builder",
        description = "Complete 5 lessons across AI Academy courses",
        category = "Consistency",
        unlocked = completedLessons >= 5,
        progressLabel = "${completedLessons.coerceAtMost(5)}/5 lessons"
      ),
      AchievementBadge(
        id = "quiz_master",
        title = "Assessment Passed",
        description = "Pass a module quiz or final course examination",
        category = "Mastery",
        unlocked = passedQuizzes >= 1,
        progressLabel = "${passedQuizzes.coerceAtMost(1)}/1 passed"
      ),
      AchievementBadge(
        id = "distinction_85",
        title = "Distinction Scholar",
        description = "Score 85% or higher on an assessment",
        category = "Excellence",
        unlocked = highScoreCount >= 1,
        progressLabel = if (highScoreCount >= 1) "Earned" else "Target 85%+"
      ),
      AchievementBadge(
        id = "streak_3",
        title = "3-Day Learning Streak",
        description = "Study consistently for 3 active days",
        category = "Habit",
        unlocked = streak >= 3L,
        progressLabel = "${streak.coerceAtMost(3L)}/3 days"
      ),
      AchievementBadge(
        id = "course_graduate",
        title = "Course Finisher",
        description = "Complete 100% of a course curriculum",
        category = "Completion",
        unlocked = completedCourses >= 1,
        progressLabel = "${completedCourses.coerceAtMost(1)}/1 course"
      ),
      AchievementBadge(
        id = "multi_academy",
        title = "AI Track Explorer",
        description = "Enroll in 3 or more AI Academy courses",
        category = "Breadth",
        unlocked = distinctCourses >= 3,
        progressLabel = "${distinctCourses.coerceAtMost(3)}/3 courses"
      ),
      AchievementBadge(
        id = "certified_pro",
        title = "Verified Graduate",
        description = "Claim an official Datalabs certificate",
        category = "Credential",
        unlocked = certs >= 1,
        progressLabel = "${certs.coerceAtMost(1)}/1 cert"
      )
    )
  }
}

sealed interface AiAssistantUiState {
  data object Idle : AiAssistantUiState
  data class Loading(val promptLabel: String) : AiAssistantUiState
  data class Streaming(val promptLabel: String, val text: String) : AiAssistantUiState
  data class Success(val promptLabel: String, val text: String) : AiAssistantUiState
  data class Error(val message: String) : AiAssistantUiState
}

