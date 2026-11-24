package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Certificate
import com.example.data.model.Course
import com.example.data.model.CoursePurchase
import com.example.data.model.Enrollment
import com.example.data.model.LearningPath
import com.example.data.model.LearningPathProgress
import com.example.data.model.Lesson
import com.example.data.model.LessonProgress
import com.example.data.model.NotificationItem
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.SavedCourse
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
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

class StudentRepository(
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

  private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String, expectedUid: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      if (auth?.currentUser?.uid == expectedUid) {
        handleFirestoreError(firestoreError ?: error, op, path)
      }
      throw error
    }

  suspend fun ensureUserProfileExists(): Result<UserProfile> {
    val user = auth?.currentUser
      ?: return Result.failure(IllegalStateException("Not signed in"))
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        val profile = snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(id = uid, userId = uid)
        Result.success(profile)
      } else {
        val isBootstrapAdmin = user.email.equals(AuthManager.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
        val role = if (isBootstrapAdmin) "ADMIN" else "STUDENT"
        val fullName = user.displayName?.takeIf { it.isNotBlank() } ?: (user.email?.substringBefore("@") ?: "Datalabs Learner")
        val email = user.email ?: "learner@datalabs.academy"
        val payload = mapOf(
          "userId" to uid,
          "fullName" to fullName.take(120),
          "email" to email.take(200),
          "photoUrl" to user.photoUrl?.toString()?.take(1000),
          "role" to role,
          "careerInterests" to listOf("Data Analytics", "AI & Productivity"),
          "learningInterests" to listOf("Excel", "SQL", "Business"),
          "learningGoal" to "Build practical skills for career growth and entrepreneurship.",
          "weeklyTargetHours" to 5L,
          "streakDays" to 0L,
          "totalLearningMinutes" to 0L,
          "remindersEnabled" to true,
          "courseUpdatesEnabled" to true,
          "announcementsEnabled" to true,
          "achievementsEnabled" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        docRef.set(payload).await()
        Result.success(
          UserProfile(
            id = uid,
            userId = uid,
            fullName = fullName,
            email = email,
            photoUrl = user.photoUrl?.toString(),
            role = role
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.GET, "users/$userId", uid)
    )
  }

  fun observeEnrollments(userId: String): Flow<List<Enrollment>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("enrollments")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Enrollment::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "enrollments", uid)
    )
  }

  fun observeLessonProgress(userId: String): Flow<List<LessonProgress>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("lessonProgress")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap -> snap.toObjects(LessonProgress::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.LIST, "lessonProgress", uid)
    )
  }

  fun observeLearningPathProgress(userId: String): Flow<List<LearningPathProgress>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("learningPathProgress")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap -> snap.toObjects(LearningPathProgress::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.LIST, "learningPathProgress", uid)
    )
  }

  fun observeSavedCourses(userId: String): Flow<List<SavedCourse>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("savedCourses")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap -> snap.toObjects(SavedCourse::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.LIST, "savedCourses", uid)
    )
  }

  fun observeCertificates(userId: String): Flow<List<Certificate>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("certificates")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Certificate::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "certificates", uid)
    )
  }

  fun observeSubscription(userId: String): Flow<Subscription?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("subscriptions").document(userId)
        .snapshots()
        .map { it.toObject(Subscription::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.GET, "subscriptions/$userId", uid)
    )
  }

  fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("quizAttempts")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(QuizAttempt::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "quizAttempts", uid)
    )
  }

  fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("notifications")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(NotificationItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logAndRethrow(OperationType.LIST, "notifications", uid)
    )
  }

  fun observeCoursePurchases(userId: String): Flow<List<CoursePurchase>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "User ID mismatch" }
    emitAll(
      db.collection("coursePurchases")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(CoursePurchase::class.java, ServerTimestampBehavior.ESTIMATE)
        }
        .logAndRethrow(OperationType.LIST, "coursePurchases", uid)
    )
  }

  suspend fun enrollInCourse(course: Course, firstLesson: Lesson?): Result<Unit> {
    val uid = requireUserId()
    val enrollmentId = "${uid}_${course.id}"
    val path = "enrollments/$enrollmentId"
    return try {
      val existing = db.collection("enrollments").document(enrollmentId).get().await()
      if (existing.exists()) {
        return Result.success(Unit)
      }
      val batch = db.batch()
      val payload = mapOf(
        "userId" to uid,
        "courseId" to course.id,
        "academyId" to course.academyId,
        "courseTitle" to course.title,
        "status" to "ACTIVE",
        "progressPercent" to 0L,
        "completedLessonsCount" to 0L,
        "totalLessonsCount" to course.lessonCount.coerceAtLeast(1L),
        "lastLessonId" to firstLesson?.id,
        "lastLessonTitle" to firstLesson?.title,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }
      batch.set(db.collection("enrollments").document(enrollmentId), payload)
      batch.update(
        db.collection("courses").document(course.id),
        mapOf(
          "enrollmentCount" to FieldValue.increment(1L),
          "popularityScore" to FieldValue.increment(1L),
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

  suspend fun completeLessonAndUpdateProgress(
    course: Course,
    lesson: Lesson,
    allCourseLessons: List<Lesson>,
    existingProgressForCourse: List<LessonProgress>,
    learningPaths: List<LearningPath>,
    completedCourseIds: Set<String>
  ): Result<Boolean> {
    val uid = requireUserId()
    val progressId = "${uid}_${lesson.id}"
    val enrollmentId = "${uid}_${course.id}"
    return try {
      val lessonRef = db.collection("lessonProgress").document(progressId)
      val existingLessonDoc = lessonRef.get().await()
      val wasAlreadyCompleted = existingLessonDoc.getBoolean("completed") == true

      val totalLessons = allCourseLessons.size.coerceAtLeast(1)
      val completedSet = existingProgressForCourse.filter { it.completed }.map { it.lessonId }.toMutableSet()
      completedSet.add(lesson.id)
      val completedCount = completedSet.size.coerceAtMost(totalLessons)
      val progressPercent = ((completedCount * 100.0) / totalLessons).toLong().coerceIn(0L, 100L)
      val courseCompletedNow = completedCount >= totalLessons

      val batch = db.batch()
      if (existingLessonDoc.exists()) {
        batch.update(
          lessonRef,
          mapOf(
            "completed" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      } else {
        batch.set(
          lessonRef,
          mapOf(
            "userId" to uid,
            "courseId" to course.id,
            "lessonId" to lesson.id,
            "completed" to true,
            "bookmarked" to false,
            "lastPositionSeconds" to 0L,
            "playbackSpeed" to 1.0,
            "studentNotes" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      val enrollRef = db.collection("enrollments").document(enrollmentId)
      val enrollSnap = enrollRef.get().await()
      if (enrollSnap.exists()) {
        val updates = mutableMapOf<String, Any>(
          "status" to if (courseCompletedNow) "COMPLETED" else "ACTIVE",
          "progressPercent" to progressPercent,
          "completedLessonsCount" to completedCount.toLong(),
          "totalLessonsCount" to totalLessons.toLong(),
          "lastLessonId" to lesson.id,
          "lastLessonTitle" to lesson.title,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        if (courseCompletedNow && enrollSnap.getTimestamp("completedAt") == null) {
          updates["completedAt"] = FieldValue.serverTimestamp()
        }
        batch.update(enrollRef, updates)
      }

      if (!wasAlreadyCompleted) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val userRef = db.collection("users").document(uid)
        val userSnap = userRef.get().await()
        val lastDate = userSnap.getString("lastActivityDate")
        val streakUpdate = if (lastDate == today) 0L else 1L
        batch.update(
          userRef,
          mapOf(
            "totalLearningMinutes" to FieldValue.increment(lesson.durationMinutes),
            "streakDays" to FieldValue.increment(streakUpdate),
            "lastActivityDate" to today,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      // Sync learning path progress if this course completed
      if (courseCompletedNow) {
        val updatedCompleted = completedCourseIds + course.id
        for (lp in learningPaths) {
          if (lp.courseIds.contains(course.id)) {
            val lpDone = lp.courseIds.filter { updatedCompleted.contains(it) }
            val lpPercent = if (lp.courseIds.isEmpty()) 0L else ((lpDone.size * 100.0) / lp.courseIds.size).toLong().coerceIn(0L, 100L)
            val lpProgressId = "${uid}_${lp.id}"
            val lpRef = db.collection("learningPathProgress").document(lpProgressId)
            val lpSnap = lpRef.get().await()
            if (lpSnap.exists()) {
              batch.update(
                lpRef,
                mapOf(
                  "completedCourseIds" to lpDone,
                  "progressPercent" to lpPercent,
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            } else {
              batch.set(
                lpRef,
                mapOf(
                  "userId" to uid,
                  "pathId" to lp.id,
                  "pathTitle" to lp.title,
                  "completedCourseIds" to lpDone,
                  "progressPercent" to lpPercent,
                  "createdAt" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }
        }
      }

      batch.commit().await()
      Result.success(courseCompletedNow)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "lessonProgress/$progressId")
      Result.failure(e)
    }
  }

  suspend fun saveLessonState(
    courseId: String,
    lessonId: String,
    bookmarked: Boolean,
    lastPositionSeconds: Long,
    playbackSpeed: Double,
    studentNotes: String
  ): Result<Unit> {
    val uid = requireUserId()
    val progressId = "${uid}_${lessonId}"
    val path = "lessonProgress/$progressId"
    return try {
      val ref = db.collection("lessonProgress").document(progressId)
      val snap = ref.get().await()
      if (snap.exists()) {
        ref.update(
          mapOf(
            "bookmarked" to bookmarked,
            "lastPositionSeconds" to lastPositionSeconds.coerceAtLeast(0L),
            "playbackSpeed" to playbackSpeed.coerceIn(0.5, 3.0),
            "studentNotes" to studentNotes.take(10000),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "courseId" to courseId,
            "lessonId" to lessonId,
            "completed" to false,
            "bookmarked" to bookmarked,
            "lastPositionSeconds" to lastPositionSeconds.coerceAtLeast(0L),
            "playbackSpeed" to playbackSpeed.coerceIn(0.5, 3.0),
            "studentNotes" to studentNotes.take(10000),
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

  suspend fun issueCompletionCertificate(
    studentName: String,
    course: Course
  ): Result<Certificate> {
    val uid = requireUserId()
    val certId = "${uid}_${course.id}"
    val path = "certificates/$certId"
    return try {
      val existing = db.collection("certificates").document(certId).get().await()
      if (existing.exists()) {
        val cert = existing.toObject(Certificate::class.java, ServerTimestampBehavior.ESTIMATE)
        if (cert != null) return Result.success(cert)
      }
      val code = "DLH-2026-${(certId.hashCode().toUInt().toString(36)).uppercase().take(6).padStart(6, '0')}"
      val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
      val verificationUrl = "https://datalabs.academy/verify/$code"
      db.collection("certificates").document(certId).set(
        mapOf(
          "certificateCode" to code,
          "userId" to uid,
          "studentName" to studentName.ifBlank { "Datalabs Learner" }.take(120),
          "courseId" to course.id,
          "courseTitle" to course.title,
          "academyName" to course.academyName,
          "issuedDateText" to dateText,
          "verificationUrl" to verificationUrl,
          "status" to "VALID",
          "issuedBy" to "SYSTEM_COMPLETION",
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()

      // Record achievement notification
      createNotification(
        title = "Certificate Earned!",
        body = "Congratulations! Your verified certificate for ${course.title} ($code) is ready.",
        type = "CERTIFICATE",
        targetRoute = "certificates"
      )

      Result.success(
        Certificate(
          id = certId,
          certificateCode = code,
          userId = uid,
          studentName = studentName.ifBlank { "Datalabs Learner" },
          courseId = course.id,
          courseTitle = course.title,
          academyName = course.academyName,
          issuedDateText = dateText,
          verificationUrl = verificationUrl,
          status = "VALID",
          issuedBy = "SYSTEM_COMPLETION"
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun issueSpecializationCertificate(
    studentName: String,
    academyName: String = "AI Academy"
  ): Result<Certificate> {
    val uid = requireUserId()
    val specCourseId = com.example.data.seed.AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID
    val specTitle = com.example.data.seed.AcademyCatalogDefinitions.AI_SPECIALIZATION_TITLE
    val certId = "${uid}_$specCourseId"
    val path = "certificates/$certId"
    return try {
      val existing = db.collection("certificates").document(certId).get().await()
      if (existing.exists()) {
        val cert = existing.toObject(Certificate::class.java, ServerTimestampBehavior.ESTIMATE)
        if (cert != null) return Result.success(cert)
      }
      val code = "DLH-SPEC-${(certId.hashCode().toUInt().toString(36)).uppercase().take(6).padStart(6, '0')}"
      val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
      val verificationUrl = "https://datalabs.academy/verify/$code"
      db.collection("certificates").document(certId).set(
        mapOf(
          "certificateCode" to code,
          "userId" to uid,
          "studentName" to studentName.ifBlank { "Datalabs Learner" }.take(120),
          "courseId" to specCourseId,
          "courseTitle" to specTitle,
          "academyName" to academyName,
          "issuedDateText" to dateText,
          "verificationUrl" to verificationUrl,
          "status" to "VALID",
          "issuedBy" to "SYSTEM_COMPLETION",
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()

      createNotification(
        title = "AI Academy Specialization Earned!",
        body = "You completed all 5 AI Academy courses! Your Specialization Certificate ($code) is ready.",
        type = "CERTIFICATE",
        targetRoute = "certificates"
      )

      Result.success(
        Certificate(
          id = certId,
          certificateCode = code,
          userId = uid,
          studentName = studentName.ifBlank { "Datalabs Learner" },
          courseId = specCourseId,
          courseTitle = specTitle,
          academyName = academyName,
          issuedDateText = dateText,
          verificationUrl = verificationUrl,
          status = "VALID",
          issuedBy = "SYSTEM_COMPLETION"
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun verifyCertificateByCode(codeOrId: String): Result<Certificate?> {
    val clean = codeOrId.trim()
    return try {
      requireUserId()
      if (clean.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
        val byId = db.collection("certificates").document(clean).get().await()
        if (byId.exists()) {
          return Result.success(byId.toObject(Certificate::class.java, ServerTimestampBehavior.ESTIMATE))
        }
      }
      val uid = requireUserId()
      val mine = db.collection("certificates")
        .whereEqualTo("userId", uid)
        .get()
        .await()
        .toObjects(Certificate::class.java, ServerTimestampBehavior.ESTIMATE)
        .firstOrNull { it.certificateCode.equals(clean, ignoreCase = true) }
      Result.success(mine)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "certificates/$clean")
      Result.failure(e)
    }
  }

  suspend fun toggleSavedCourse(course: Course, currentlySaved: Boolean): Result<Unit> {
    val uid = requireUserId()
    val savedId = "${uid}_${course.id}"
    val path = "savedCourses/$savedId"
    return try {
      val ref = db.collection("savedCourses").document(savedId)
      if (currentlySaved) {
        ref.delete().await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "courseId" to course.id,
            "courseTitle" to course.title,
            "academyId" to course.academyId,
            "createdAt" to FieldValue.serverTimestamp()
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

  suspend fun submitQuizAttempt(
    quiz: Quiz,
    scorePercent: Long,
    earnedPoints: Long,
    totalPoints: Long,
    passed: Boolean,
    attemptNumber: Long,
    answersSummary: String
  ): Result<Unit> {
    val uid = requireUserId()
    val attemptId = "${uid}_${quiz.id}_$attemptNumber"
    val path = "quizAttempts/$attemptId"
    return try {
      db.collection("quizAttempts").document(attemptId).set(
        mapOf(
          "userId" to uid,
          "quizId" to quiz.id,
          "courseId" to quiz.courseId,
          "scorePercent" to scorePercent.coerceIn(0L, 100L),
          "earnedPoints" to earnedPoints.coerceAtLeast(0L),
          "totalPoints" to totalPoints.coerceAtLeast(1L),
          "passed" to passed,
          "attemptNumber" to attemptNumber.coerceAtLeast(1L),
          "answersSummary" to answersSummary.take(10000),
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

  suspend fun submitAssignment(
    assignmentId: String,
    courseId: String,
    submissionText: String,
    attachmentUrl: String?
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "assignments/$assignmentId/submissions/$uid"
    return try {
      val ref = db.collection("assignments").document(assignmentId)
        .collection("submissions").document(uid)
      val snap = ref.get().await()
      if (snap.exists()) {
        val updates = mapOf(
          "submissionText" to submissionText.trim(),
          "attachmentUrl" to attachmentUrl?.takeIf { it.isNotBlank() },
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        ref.update(updates).await()
      } else {
        val payload = mapOf(
          "userId" to uid,
          "assignmentId" to assignmentId,
          "courseId" to courseId,
          "submissionText" to submissionText.trim(),
          "attachmentUrl" to attachmentUrl?.takeIf { it.isNotBlank() },
          "status" to "SUBMITTED",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        ref.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun getAssignmentSubmission(assignmentId: String): AssignmentSubmission? {
    val uid = auth?.currentUser?.uid ?: return null
    return try {
      db.collection("assignments").document(assignmentId)
        .collection("submissions").document(uid)
        .get().await()
        .toObject(AssignmentSubmission::class.java, ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      null
    }
  }

  fun observeAssignmentSubmission(assignmentId: String): Flow<AssignmentSubmission?> = flow {
    val uid = requireUserId()
    val path = "assignments/$assignmentId/submissions/$uid"
    emitAll(
      db.collection("assignments").document(assignmentId)
        .collection("submissions").document(uid)
        .snapshots()
        .map { snap -> snap.toObject(AssignmentSubmission::class.java, ServerTimestampBehavior.ESTIMATE) }
        .logAndRethrow(OperationType.GET, path, uid)
    )
  }

  suspend fun updateProfileAndPreferences(
    fullName: String,
    learningGoal: String,
    weeklyTargetHours: Long,
    careerInterests: List<String>,
    learningInterests: List<String>,
    remindersEnabled: Boolean,
    courseUpdatesEnabled: Boolean,
    announcementsEnabled: Boolean,
    achievementsEnabled: Boolean
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid"
    return try {
      db.collection("users").document(uid).update(
        mapOf(
          "fullName" to fullName.trim().take(120),
          "learningGoal" to learningGoal.trim().take(300),
          "weeklyTargetHours" to weeklyTargetHours.coerceIn(1L, 80L),
          "careerInterests" to careerInterests.take(12),
          "learningInterests" to learningInterests.take(20),
          "remindersEnabled" to remindersEnabled,
          "courseUpdatesEnabled" to courseUpdatesEnabled,
          "announcementsEnabled" to announcementsEnabled,
          "achievementsEnabled" to achievementsEnabled,
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

  suspend fun requestSubscriptionCheckout(
    plan: SubscriptionPlan,
    currency: String,
    paymentProvider: String
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "subscriptions/$uid"
    return try {
      val amount = if (currency == "UGX") plan.priceUgx.toDouble() else plan.priceUsd
      val ref = db.collection("subscriptions").document(uid)
      val snap = ref.get().await()
      if (snap.exists()) {
        ref.update(
          mapOf(
            "planId" to plan.id,
            "planName" to plan.name,
            "billingCycle" to plan.billingCycle,
            "currency" to currency,
            "amount" to amount,
            "status" to "PENDING_PAYMENT",
            "paymentProvider" to paymentProvider,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "planId" to plan.id,
            "planName" to plan.name,
            "billingCycle" to plan.billingCycle,
            "currency" to currency,
            "amount" to amount,
            "status" to "PENDING_PAYMENT",
            "paymentProvider" to paymentProvider,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      createNotification(
        title = "Subscription Request Submitted",
        body = "Your ${plan.name} (${currency}) checkout request via $paymentProvider is pending payment gateway verification or admin activation.",
        type = "SUBSCRIPTION",
        targetRoute = "subscription"
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun activatePlaySubscription(
    planId: String,
    planName: String,
    billingCycle: String,
    amountUsd: Double,
    productId: String,
    purchaseToken: String,
    autoRenewing: Boolean,
    expiresAtText: String
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "subscriptions/$uid"
    return try {
      val ref = db.collection("subscriptions").document(uid)
      val snap = ref.get().await()
      if (snap.exists()) {
        ref.update(
          mapOf(
            "planId" to planId,
            "planName" to planName,
            "billingCycle" to billingCycle,
            "currency" to "USD",
            "amount" to amountUsd,
            "status" to "ACTIVE",
            "paymentProvider" to "GOOGLE_PLAY",
            "productId" to productId,
            "purchaseToken" to purchaseToken.take(500),
            "autoRenewing" to autoRenewing,
            "expiresAtText" to expiresAtText,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "planId" to planId,
            "planName" to planName,
            "billingCycle" to billingCycle,
            "currency" to "USD",
            "amount" to amountUsd,
            "status" to "ACTIVE",
            "paymentProvider" to "GOOGLE_PLAY",
            "productId" to productId,
            "purchaseToken" to purchaseToken.take(500),
            "autoRenewing" to autoRenewing,
            "expiresAtText" to expiresAtText,
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

  suspend fun requestCoursePurchaseCheckout(
    course: Course,
    currency: String,
    paymentProvider: String
  ): Result<Unit> {
    val uid = requireUserId()
    val id = "${uid}_${course.id}"
    val path = "coursePurchases/$id"
    return try {
      val amount = if (currency == "UGX") course.priceUgx.toDouble() else course.priceUsd
      val ref = db.collection("coursePurchases").document(id)
      val snap = ref.get().await()
      if (snap.exists()) {
        ref.update(
          mapOf(
            "amount" to amount,
            "currency" to currency,
            "paymentProvider" to paymentProvider,
            "status" to "PENDING_PAYMENT",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "courseId" to course.id,
            "courseTitle" to course.title.take(160),
            "amount" to amount,
            "currency" to currency,
            "paymentProvider" to paymentProvider,
            "status" to "PENDING_PAYMENT",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      createNotification(
        title = "Course Unlock Request Submitted",
        body = "Your single-course unlock for ${course.title} ($currency) via $paymentProvider is pending confirmation.",
        type = "SUBSCRIPTION",
        targetRoute = "subscription"
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun activatePlayCoursePurchase(
    course: Course,
    productId: String,
    purchaseToken: String
  ): Result<Unit> {
    val uid = requireUserId()
    val id = "${uid}_${course.id}"
    val path = "coursePurchases/$id"
    return try {
      val ref = db.collection("coursePurchases").document(id)
      val snap = ref.get().await()
      if (snap.exists()) {
        ref.update(
          mapOf(
            "amount" to course.priceUsd,
            "currency" to "USD",
            "paymentProvider" to "GOOGLE_PLAY",
            "productId" to productId,
            "purchaseToken" to purchaseToken.take(500),
            "status" to "ACTIVE",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "courseId" to course.id,
            "courseTitle" to course.title.take(160),
            "amount" to course.priceUsd,
            "currency" to "USD",
            "paymentProvider" to "GOOGLE_PLAY",
            "productId" to productId,
            "purchaseToken" to purchaseToken.take(500),
            "status" to "ACTIVE",
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

  suspend fun markNotificationRead(notificationId: String): Result<Unit> {
    val path = "notifications/$notificationId"
    return try {
      requireUserId()
      db.collection("notifications").document(notificationId)
        .update(mapOf("read" to true))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createNotification(
    title: String,
    body: String,
    type: String,
    targetRoute: String? = null
  ): Result<Unit> {
    val uid = requireUserId()
    val id = "notif_${uid}_${System.currentTimeMillis()}"
    val path = "notifications/$id"
    return try {
      val payload = mapOf(
        "userId" to uid,
        "title" to title.take(160),
        "body" to body.take(600),
        "type" to type,
        "targetRoute" to targetRoute?.take(200),
        "read" to false,
        "createdAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }
      db.collection("notifications").document(id).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }
}
