package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Academy
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Certificate
import com.example.data.model.Course
import com.example.data.model.CoursePurchase
import com.example.data.model.CourseRatingHelper
import com.example.data.model.CourseReview
import com.example.data.model.Enrollment
import com.example.data.model.Instructor
import com.example.data.model.LearningPath
import com.example.data.model.Lesson
import com.example.data.model.LessonContent
import com.example.data.model.Quiz
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.seed.CategoryDefinitions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatalogRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth?.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      if (auth?.currentUser != null) {
        handleFirestoreError(firestoreError ?: error, op, path)
      }
      throw error
    }

  fun observeAcademies(): Flow<List<Academy>> = flow {
    requireUserId()
    emitAll(
      db.collection("academies")
        .snapshots()
        .map { snap ->
          snap.toObjects(Academy::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayOrder }
        }
        .logAndRethrow(OperationType.LIST, "academies")
    )
  }

  fun observeCourses(): Flow<List<Course>> = flow {
    requireUserId()
    emitAll(
      db.collection("courses")
        .snapshots()
        .map { snap ->
          snap.toObjects(Course::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareByDescending<Course> { it.featured }.thenBy { it.title })
        }
        .logAndRethrow(OperationType.LIST, "courses")
    )
  }

  fun observeLessonsForCourse(courseId: String): Flow<List<Lesson>> = flow {
    requireUserId()
    emitAll(
      db.collection("lessons")
        .whereEqualTo("courseId", courseId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Lesson::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.orderIndex }
        }
        .logAndRethrow(OperationType.LIST, "lessons")
    )
  }

  fun observeLearningPaths(): Flow<List<LearningPath>> = flow {
    requireUserId()
    emitAll(
      db.collection("learningPaths")
        .snapshots()
        .map { snap ->
          snap.toObjects(LearningPath::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.title }
        }
        .logAndRethrow(OperationType.LIST, "learningPaths")
    )
  }

  fun observeInstructors(): Flow<List<Instructor>> = flow {
    requireUserId()
    emitAll(
      db.collection("instructors")
        .snapshots()
        .map { snap ->
          snap.toObjects(Instructor::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.fullName }
        }
        .logAndRethrow(OperationType.LIST, "instructors")
    )
  }

  fun observeQuizzesForCourse(courseId: String): Flow<List<Quiz>> = flow {
    requireUserId()
    emitAll(
      db.collection("quizzes")
        .whereEqualTo("courseId", courseId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Quiz::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<Quiz> { it.moduleIndex }.thenBy { it.id })
        }
        .logAndRethrow(OperationType.LIST, "quizzes")
    )
  }

  fun observeAssignmentsForCourse(courseId: String): Flow<List<Assignment>> = flow {
    requireUserId()
    emitAll(
      db.collection("assignments")
        .whereEqualTo("courseId", courseId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Assignment::class.java, ServerTimestampBehavior.ESTIMATE)
        }
        .logAndRethrow(OperationType.LIST, "assignments")
    )
  }

  fun observeSubscriptionPlans(): Flow<List<SubscriptionPlan>> = flow {
    requireUserId()
    emitAll(
      db.collection("subscriptionPlans")
        .snapshots()
        .map { snap ->
          snap.toObjects(SubscriptionPlan::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.priceUsd }
        }
        .logAndRethrow(OperationType.LIST, "subscriptionPlans")
    )
  }

  fun observeAnnouncements(): Flow<List<Announcement>> = flow {
    requireUserId()
    emitAll(
      db.collection("announcements")
        .snapshots()
        .map { snap ->
          snap.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "announcements")
    )
  }

  fun observeCourseReviews(): Flow<List<CourseReview>> = flow {
    requireUserId()
    emitAll(
      db.collection("courseReviews")
        .snapshots()
        .map { snap ->
          snap.toObjects(CourseReview::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "courseReviews")
    )
  }

  fun observeReviewsForCourse(courseId: String): Flow<List<CourseReview>> = flow {
    requireUserId()
    emitAll(
      db.collection("courseReviews")
        .whereEqualTo("courseId", courseId)
        .snapshots()
        .map { snap ->
          snap.toObjects(CourseReview::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "courseReviews")
    )
  }

  suspend fun submitCourseReview(
    courseId: String,
    rating: Int,
    reviewText: String,
    fallbackUserName: String? = null
  ): Result<Unit> {
    if (courseId.isBlank()) {
      return Result.failure(IllegalArgumentException("Invalid course selection."))
    }
    if (rating !in CourseRatingHelper.MIN_RATING..CourseRatingHelper.MAX_RATING) {
      return Result.failure(IllegalArgumentException("Please select a rating between 1 and 5 stars."))
    }
    val cleanReviewText = reviewText.trim()
    if (cleanReviewText.length > CourseRatingHelper.MAX_REVIEW_LENGTH) {
      return Result.failure(
        IllegalArgumentException("Review must be ${CourseRatingHelper.MAX_REVIEW_LENGTH} characters or less.")
      )
    }

    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val reviewId = "${uid}_${courseId}"
    val path = "courseReviews/$reviewId"
    val rawName = auth?.currentUser?.displayName?.trim()?.takeIf { it.length >= 2 }
      ?: fallbackUserName?.trim()?.takeIf { it.length >= 2 }
      ?: auth?.currentUser?.email?.substringBefore("@")?.trim()?.takeIf { it.length >= 2 }
      ?: "Datalabs Learner"
    val safeAuthorName = rawName.take(120)

    var isUpdate = false
    return try {
      val ref = db.collection("courseReviews").document(reviewId)
      val existing = ref.get().await()
      if (existing.exists()) {
        isUpdate = true
        ref.update(
          mapOf(
            "rating" to rating.toLong(),
            "reviewText" to cleanReviewText,
            "userName" to safeAuthorName,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "userName" to safeAuthorName,
            "courseId" to courseId,
            "rating" to rating.toLong(),
            "reviewText" to cleanReviewText,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteCourseReview(courseId: String): Result<Unit> {
    if (courseId.isBlank()) {
      return Result.failure(IllegalArgumentException("Invalid course selection."))
    }
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val reviewId = "${uid}_${courseId}"
    val path = "courseReviews/$reviewId"
    return try {
      db.collection("courseReviews").document(reviewId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun getLessonContent(lessonId: String): Result<LessonContent> {
    val path = "lessonContents/$lessonId"
    return try {
      requireUserId()
      val snap = db.collection("lessonContents").document(lessonId).get().await()
      val content = snap.toObject(LessonContent::class.java, ServerTimestampBehavior.ESTIMATE)
        ?: return Result.failure(IllegalStateException("Lesson content not found"))
      Result.success(content)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun checkIsAdmin(userId: String): Boolean {
    val current = auth?.currentUser
    if (current?.email.equals(AuthManager.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && current?.isEmailVerified == true) {
      return true
    }
    return try {
      val doc = db.collection("admins").document(userId).get().await()
      doc.exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  // --- ADMIN MANAGEMENT OPERATIONS ---

  suspend fun adminUpsertAcademy(
    academyId: String,
    name: String,
    shortName: String,
    description: String,
    iconKey: String,
    accentHex: String,
    displayOrder: Long,
    courseCount: Long,
    published: Boolean,
    isNew: Boolean
  ): Result<Unit> {
    val path = "academies/$academyId"
    return try {
      requireUserId()
      val ref = db.collection("academies").document(academyId)
      if (isNew) {
        ref.set(
          mapOf(
            "name" to name.trim(),
            "shortName" to shortName.trim(),
            "description" to description.trim(),
            "iconKey" to iconKey,
            "accentHex" to accentHex,
            "displayOrder" to displayOrder,
            "courseCount" to courseCount,
            "published" to published,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.update(
          mapOf(
            "name" to name.trim(),
            "shortName" to shortName.trim(),
            "description" to description.trim(),
            "iconKey" to iconKey,
            "accentHex" to accentHex,
            "displayOrder" to displayOrder,
            "courseCount" to courseCount,
            "published" to published,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun adminUpsertCourse(
    courseId: String,
    academyId: String,
    academyName: String,
    title: String,
    subtitle: String,
    description: String,
    instructorId: String,
    instructorName: String,
    level: String,
    durationMinutes: Long,
    accessTier: String,
    priceUsd: Double,
    priceUgx: Long,
    certificateAvailable: Boolean,
    published: Boolean,
    featured: Boolean,
    coverUrl: String?,
    whatYouWillLearn: List<String>,
    prerequisites: List<String>,
    formats: List<String>,
    tags: List<String>,
    category: String = "",
    categories: List<String> = emptyList(),
    isNew: Boolean
  ): Result<Unit> {
    val path = "courses/$courseId"
    return try {
      requireUserId()
      val resolvedPrimary = category.ifBlank {
        CategoryDefinitions.resolvePrimaryCategory(academyId, title)
      }
      val resolvedList = categories.ifEmpty {
        CategoryDefinitions.resolveCategories(academyId, title)
      }
      val ref = db.collection("courses").document(courseId)
      if (isNew) {
        val payload = mapOf(
          "academyId" to academyId,
          "academyName" to academyName,
          "title" to title.trim(),
          "subtitle" to subtitle.trim(),
          "description" to description.trim(),
          "instructorId" to instructorId,
          "instructorName" to instructorName,
          "level" to level,
          "durationMinutes" to durationMinutes,
          "lessonCount" to 0L,
          "moduleCount" to 4L,
          "accessTier" to accessTier,
          "priceUsd" to priceUsd,
          "priceUgx" to priceUgx,
          "certificateAvailable" to certificateAvailable,
          "published" to published,
          "featured" to featured,
          "enrollmentCount" to 0L,
          "popularityScore" to 0L,
          "whatYouWillLearn" to whatYouWillLearn,
          "prerequisites" to prerequisites,
          "formats" to formats,
          "tags" to tags,
          "category" to resolvedPrimary,
          "categories" to resolvedList,
          "coverUrl" to coverUrl?.takeIf { it.isNotBlank() },
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        ref.set(payload).await()
      } else {
        val payload = mapOf(
          "academyId" to academyId,
          "academyName" to academyName,
          "title" to title.trim(),
          "subtitle" to subtitle.trim(),
          "description" to description.trim(),
          "instructorId" to instructorId,
          "instructorName" to instructorName,
          "level" to level,
          "durationMinutes" to durationMinutes,
          "accessTier" to accessTier,
          "priceUsd" to priceUsd,
          "priceUgx" to priceUgx,
          "certificateAvailable" to certificateAvailable,
          "published" to published,
          "featured" to featured,
          "whatYouWillLearn" to whatYouWillLearn,
          "prerequisites" to prerequisites,
          "formats" to formats,
          "tags" to tags,
          "category" to resolvedPrimary,
          "categories" to resolvedList,
          "coverUrl" to coverUrl?.takeIf { it.isNotBlank() },
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        ref.update(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun adminToggleCourseFlags(
    courseId: String,
    published: Boolean? = null,
    featured: Boolean? = null,
    accessTier: String? = null
  ): Result<Unit> {
    val path = "courses/$courseId"
    return try {
      requireUserId()
      val updates = mutableMapOf<String, Any>(
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (published != null) updates["published"] = published
      if (featured != null) updates["featured"] = featured
      if (accessTier != null) updates["accessTier"] = accessTier
      db.collection("courses").document(courseId).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminAddLessonWithContent(
    lessonId: String,
    courseId: String,
    academyId: String,
    moduleTitle: String = "",
    title: String,
    summary: String,
    orderIndex: Long,
    durationMinutes: Long,
    format: String,
    freePreview: Boolean,
    premiumOnly: Boolean,
    notesMarkdown: String,
    videoUrl: String?,
    downloadTitle: String?,
    downloadUrl: String?,
    exercisePrompt: String?
  ): Result<Unit> {
    val path = "lessons/$lessonId"
    return try {
      requireUserId()
      val batch = db.batch()
      val lessonPayload = mutableMapOf<String, Any>(
        "courseId" to courseId,
        "academyId" to academyId,
        "title" to title.trim(),
        "summary" to summary.trim(),
        "orderIndex" to orderIndex,
        "durationMinutes" to durationMinutes,
        "format" to format,
        "freePreview" to freePreview,
        "premiumOnly" to premiumOnly,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (moduleTitle.isNotBlank()) lessonPayload["moduleTitle"] = moduleTitle.trim()
      batch.set(db.collection("lessons").document(lessonId), lessonPayload)

      val contentPayload = mapOf(
        "courseId" to courseId,
        "freePreview" to freePreview,
        "premiumOnly" to premiumOnly,
        "notesMarkdown" to notesMarkdown.trim(),
        "videoUrl" to videoUrl?.takeIf { it.isNotBlank() },
        "downloadTitle" to downloadTitle?.takeIf { it.isNotBlank() },
        "downloadUrl" to downloadUrl?.takeIf { it.isNotBlank() },
        "exercisePrompt" to exercisePrompt?.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }
      batch.set(db.collection("lessonContents").document(lessonId), contentPayload)
      batch.update(
        db.collection("courses").document(courseId),
        mapOf(
          "lessonCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminUpdateLessonWithContent(
    lessonId: String,
    moduleTitle: String,
    title: String,
    summary: String,
    orderIndex: Long,
    durationMinutes: Long,
    format: String,
    freePreview: Boolean,
    premiumOnly: Boolean,
    notesMarkdown: String,
    videoUrl: String?,
    downloadTitle: String?
  ): Result<Unit> {
    val path = "lessons/$lessonId"
    return try {
      requireUserId()
      val batch = db.batch()
      val lessonUpdates = mutableMapOf<String, Any>(
        "title" to title.trim(),
        "summary" to summary.trim(),
        "orderIndex" to orderIndex,
        "durationMinutes" to durationMinutes,
        "format" to format,
        "freePreview" to freePreview,
        "premiumOnly" to premiumOnly,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (moduleTitle.isNotBlank()) lessonUpdates["moduleTitle"] = moduleTitle.trim()
      batch.update(db.collection("lessons").document(lessonId), lessonUpdates)

      val contentUpdates = mutableMapOf<String, Any>(
        "freePreview" to freePreview,
        "premiumOnly" to premiumOnly,
        "notesMarkdown" to notesMarkdown.trim(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (!videoUrl.isNullOrBlank()) contentUpdates["videoUrl"] = videoUrl.trim()
      if (!downloadTitle.isNullOrBlank()) contentUpdates["downloadTitle"] = downloadTitle.trim()
      batch.update(db.collection("lessonContents").document(lessonId), contentUpdates)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminCreateQuiz(
    quizId: String,
    courseId: String,
    assessmentType: String = "FINAL_EXAM",
    moduleIndex: Long = 4L,
    title: String,
    description: String,
    passingScorePercent: Long,
    maxAttempts: Long,
    allowRetakes: Boolean,
    questionsJson: String
  ): Result<Unit> {
    val path = "quizzes/$quizId"
    return try {
      requireUserId()
      db.collection("quizzes").document(quizId).set(
        mapOf(
          "courseId" to courseId,
          "assessmentType" to assessmentType,
          "moduleIndex" to moduleIndex,
          "title" to title.trim(),
          "description" to description.trim(),
          "passingScorePercent" to passingScorePercent,
          "maxAttempts" to maxAttempts,
          "allowRetakes" to allowRetakes,
          "questionsJson" to questionsJson,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminUpdateQuiz(
    quizId: String,
    assessmentType: String,
    moduleIndex: Long,
    title: String,
    description: String,
    passingScorePercent: Long,
    maxAttempts: Long,
    allowRetakes: Boolean,
    questionsJson: String
  ): Result<Unit> {
    val path = "quizzes/$quizId"
    return try {
      requireUserId()
      db.collection("quizzes").document(quizId).update(
        mapOf(
          "assessmentType" to assessmentType,
          "moduleIndex" to moduleIndex,
          "title" to title.trim(),
          "description" to description.trim(),
          "passingScorePercent" to passingScorePercent,
          "maxAttempts" to maxAttempts,
          "allowRetakes" to allowRetakes,
          "questionsJson" to questionsJson,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminCreateAssignment(
    assignmentId: String,
    courseId: String,
    title: String,
    instructions: String,
    deliverableFormat: String,
    maxPoints: Long
  ): Result<Unit> {
    val path = "assignments/$assignmentId"
    return try {
      requireUserId()
      db.collection("assignments").document(assignmentId).set(
        mapOf(
          "courseId" to courseId,
          "title" to title.trim(),
          "instructions" to instructions.trim(),
          "deliverableFormat" to deliverableFormat.trim(),
          "maxPoints" to maxPoints,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminCreateLearningPath(
    pathId: String,
    title: String,
    description: String,
    academyId: String,
    targetRole: String,
    courseIds: List<String>,
    courseTitles: List<String>,
    estimatedHours: Long,
    premiumOnly: Boolean
  ): Result<Unit> {
    val path = "learningPaths/$pathId"
    return try {
      requireUserId()
      db.collection("learningPaths").document(pathId).set(
        mapOf(
          "title" to title.trim(),
          "description" to description.trim(),
          "academyId" to academyId,
          "targetRole" to targetRole.trim(),
          "courseIds" to courseIds,
          "courseTitles" to courseTitles,
          "estimatedHours" to estimatedHours,
          "premiumOnly" to premiumOnly,
          "published" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminCreateInstructor(
    instructorId: String,
    fullName: String,
    title: String,
    bio: String,
    specialty: String
  ): Result<Unit> {
    val path = "instructors/$instructorId"
    return try {
      requireUserId()
      db.collection("instructors").document(instructorId).set(
        mapOf(
          "fullName" to fullName.trim(),
          "title" to title.trim(),
          "bio" to bio.trim(),
          "specialty" to specialty.trim(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminBroadcastAnnouncement(
    title: String,
    message: String,
    category: String,
    academyId: String? = null,
    courseId: String? = null
  ): Result<Unit> {
    val id = "ann_${System.currentTimeMillis()}"
    val path = "announcements/$id"
    return try {
      requireUserId()
      val payload = mapOf(
        "title" to title.trim(),
        "message" to message.trim(),
        "category" to category.trim(),
        "academyId" to academyId?.takeIf { it.isNotBlank() },
        "courseId" to courseId?.takeIf { it.isNotBlank() },
        "published" to true,
        "createdAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }
      db.collection("announcements").document(id).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllStudents(): Result<List<UserProfile>> {
    return try {
      requireUserId()
      val snap = db.collection("users").get().await()
      Result.success(snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users")
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllEnrollments(): Result<List<Enrollment>> {
    return try {
      requireUserId()
      val snap = db.collection("enrollments").get().await()
      Result.success(snap.toObjects(Enrollment::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "enrollments")
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllSubscriptions(): Result<List<Subscription>> {
    return try {
      requireUserId()
      val snap = db.collection("subscriptions").get().await()
      Result.success(snap.toObjects(Subscription::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "subscriptions")
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllCertificates(): Result<List<Certificate>> {
    return try {
      requireUserId()
      val snap = db.collection("certificates").get().await()
      Result.success(snap.toObjects(Certificate::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "certificates")
      Result.failure(e)
    }
  }

  suspend fun adminSetUserSubscription(
    targetUserId: String,
    status: String,
    planName: String,
    billingCycle: String,
    currency: String,
    amount: Double
  ): Result<Unit> {
    val path = "subscriptions/$targetUserId"
    return try {
      requireUserId()
      val existing = db.collection("subscriptions").document(targetUserId).get().await()
      if (existing.exists()) {
        db.collection("subscriptions").document(targetUserId).update(
          mapOf(
            "status" to status,
            "planName" to planName,
            "billingCycle" to billingCycle,
            "currency" to currency,
            "amount" to amount,
            "paymentProvider" to "ADMIN_GRANT",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        db.collection("subscriptions").document(targetUserId).set(
          mapOf(
            "userId" to targetUserId,
            "planId" to "admin_plan",
            "planName" to planName,
            "billingCycle" to billingCycle,
            "currency" to currency,
            "amount" to amount,
            "status" to status,
            "paymentProvider" to "ADMIN_GRANT",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun adminIssueCertificate(
    targetUserId: String,
    studentName: String,
    course: Course
  ): Result<Certificate> {
    val certId = "${targetUserId}_${course.id}"
    val path = "certificates/$certId"
    return try {
      requireUserId()
      val code = "DLH-2026-${(certId.hashCode().toUInt().toString(36)).uppercase().take(6).padStart(6, '0')}"
      val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
      val verificationUrl = "https://datalabs.academy/verify/$code"
      db.collection("certificates").document(certId).set(
        mapOf(
          "certificateCode" to code,
          "userId" to targetUserId,
          "studentName" to studentName,
          "courseId" to course.id,
          "courseTitle" to course.title,
          "academyName" to course.academyName,
          "issuedDateText" to dateText,
          "verificationUrl" to verificationUrl,
          "status" to "VALID",
          "issuedBy" to "ADMIN",
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(
        Certificate(
          id = certId,
          certificateCode = code,
          userId = targetUserId,
          studentName = studentName,
          courseId = course.id,
          courseTitle = course.title,
          academyName = course.academyName,
          issuedDateText = dateText,
          verificationUrl = verificationUrl,
          status = "VALID",
          issuedBy = "ADMIN"
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminUpdateSubscriptionPlan(
    planId: String,
    name: String,
    billingCycle: String,
    priceUsd: Double,
    priceUgx: Long,
    description: String
  ): Result<Unit> {
    val path = "subscriptionPlans/$planId"
    return try {
      requireUserId()
      db.collection("subscriptionPlans").document(planId).update(
        mapOf(
          "name" to name.trim(),
          "billingCycle" to billingCycle,
          "priceUsd" to priceUsd,
          "priceUgx" to priceUgx,
          "description" to description.trim(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllSubmissions(): Result<List<AssignmentSubmission>> {
    return try {
      requireUserId()
      val snap = db.collectionGroup("submissions").get().await()
      Result.success(snap.toObjects(AssignmentSubmission::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "submissions")
      Result.failure(e)
    }
  }

  suspend fun adminGradeSubmission(
    assignmentId: String,
    targetUserId: String,
    score: Long,
    feedback: String
  ): Result<Unit> {
    val path = "assignments/$assignmentId/submissions/$targetUserId"
    return try {
      requireUserId()
      db.collection("assignments").document(assignmentId)
        .collection("submissions").document(targetUserId)
        .update(
          mapOf(
            "status" to "REVIEWED",
            "score" to score.coerceIn(0L, 1000L),
            "feedback" to feedback.trim().take(2000),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adminFetchAllCoursePurchases(): Result<List<CoursePurchase>> {
    return try {
      requireUserId()
      val snap = db.collection("coursePurchases").get().await()
      Result.success(snap.toObjects(CoursePurchase::class.java, ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "coursePurchases")
      Result.failure(e)
    }
  }

  suspend fun adminSetCoursePurchase(
    targetUserId: String,
    course: Course,
    activate: Boolean
  ): Result<Unit> {
    val id = "${targetUserId}_${course.id}"
    val path = "coursePurchases/$id"
    return try {
      requireUserId()
      val ref = db.collection("coursePurchases").document(id)
      val snap = ref.get().await()
      val status = if (activate) "ACTIVE" else "REVOKED"
      if (snap.exists()) {
        ref.update(
          mapOf(
            "status" to status,
            "paymentProvider" to "ADMIN_GRANT",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to targetUserId,
            "courseId" to course.id,
            "courseTitle" to course.title,
            "amount" to course.priceUsd,
            "currency" to "USD",
            "paymentProvider" to "ADMIN_GRANT",
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun adminSendUserNotification(
    targetUserId: String,
    title: String,
    body: String,
    type: String
  ): Result<Unit> {
    val id = "notif_${System.currentTimeMillis()}"
    val path = "notifications/$id"
    return try {
      requireUserId()
      db.collection("notifications").document(id).set(
        mapOf(
          "userId" to targetUserId,
          "title" to title.trim(),
          "body" to body.trim(),
          "type" to type,
          "read" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }
}
