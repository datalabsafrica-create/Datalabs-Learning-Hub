package com.example.ui.viewmodel

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DatalabsApplication
import com.example.data.ai.AiHelpMode
import com.example.data.ai.AiLearningAssistantService
import com.example.data.ai.AiStudyRequest
import com.example.data.billing.PlayBillingEvent
import com.example.data.billing.PlayBillingManager
import com.example.data.billing.PlayBillingUiState
import com.example.data.firebase.AnalyticsLogger
import com.example.data.firebase.LearningNotificationHelper
import com.example.data.firebase.MaterialAndCertificateStorage
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.model.Academy
import com.example.data.model.AiAssistantUiState
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Certificate
import com.example.data.model.Course
import com.example.data.model.CourseCertificationPolicy
import com.example.data.model.CoursePurchase
import com.example.data.model.Enrollment
import com.example.data.model.Instructor
import com.example.data.model.LearningPath
import com.example.data.model.LearningPathProgress
import com.example.data.model.Lesson
import com.example.data.model.LessonContent
import com.example.data.model.LessonProgress
import com.example.data.model.NotificationItem
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.SavedCourse
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.CatalogRepository
import com.example.data.repository.StudentRepository
import com.example.data.seed.AcademyCatalogDefinitions
import com.example.data.seed.CatalogSeeder
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HubCatalogSnapshot(
  val academies: List<Academy> = emptyList(),
  val courses: List<Course> = emptyList(),
  val learningPaths: List<LearningPath> = emptyList(),
  val instructors: List<Instructor> = emptyList(),
  val announcements: List<Announcement> = emptyList(),
  val plans: List<SubscriptionPlan> = emptyList()
)

data class StudentDataSnapshot(
  val profile: UserProfile? = null,
  val enrollments: List<Enrollment> = emptyList(),
  val lessonProgress: List<LessonProgress> = emptyList(),
  val pathProgress: List<LearningPathProgress> = emptyList(),
  val savedCourses: List<SavedCourse> = emptyList(),
  val certificates: List<Certificate> = emptyList(),
  val subscription: Subscription? = null,
  val quizAttempts: List<QuizAttempt> = emptyList(),
  val notifications: List<NotificationItem> = emptyList(),
  val coursePurchases: List<CoursePurchase> = emptyList()
)

data class AdminOverviewData(
  val students: List<UserProfile> = emptyList(),
  val enrollments: List<Enrollment> = emptyList(),
  val subscriptions: List<Subscription> = emptyList(),
  val certificates: List<Certificate> = emptyList(),
  val submissions: List<AssignmentSubmission> = emptyList(),
  val coursePurchases: List<CoursePurchase> = emptyList()
)

class MainViewModel(
  val currentUserId: String,
  private val catalogRepository: CatalogRepository,
  private val studentRepository: StudentRepository,
  private val catalogSeeder: CatalogSeeder,
  private val analyticsLogger: AnalyticsLogger,
  private val storageAndPdf: MaterialAndCertificateStorage,
  private val notificationHelper: LearningNotificationHelper,
  private val aiAssistantService: AiLearningAssistantService,
  private val playBillingManager: PlayBillingManager
) : ViewModel() {

  private companion object {
    const val TAG = "MainViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }

  private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val uiMessage = _uiMessage.asSharedFlow()

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _isSeedingCatalog = MutableStateFlow(false)
  val isSeedingCatalog: StateFlow<Boolean> = _isSeedingCatalog.asStateFlow()

  private val _compareCourseIds = MutableStateFlow<List<String>>(emptyList())
  val compareCourseIds: StateFlow<List<String>> = _compareCourseIds.asStateFlow()

  private val _adminOverview = MutableStateFlow<UiState<AdminOverviewData>>(UiState.Loading)
  val adminOverview: StateFlow<UiState<AdminOverviewData>> = _adminOverview.asStateFlow()

  private val _verifiedCertificateResult = MutableStateFlow<Result<Certificate?>?>(null)
  val verifiedCertificateResult: StateFlow<Result<Certificate?>?> = _verifiedCertificateResult.asStateFlow()

  private val _aiAssistantState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
  val aiAssistantState: StateFlow<AiAssistantUiState> = _aiAssistantState.asStateFlow()

  val billingState: StateFlow<PlayBillingUiState> = playBillingManager.state

  private var aiStreamJob: Job? = null

  private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing $label", firestoreError ?: error)
        emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load $label")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  val catalogState: StateFlow<UiState<HubCatalogSnapshot>> = combine(
    catalogRepository.observeAcademies(),
    catalogRepository.observeCourses(),
    catalogRepository.observeLearningPaths(),
    catalogRepository.observeInstructors(),
    catalogRepository.observeAnnouncements(),
    catalogRepository.observeSubscriptionPlans()
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    val activeAcademyId = AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID
    val activeAcademies = (values[0] as List<Academy>).filter { it.id == activeAcademyId }
    val activeCourses = (values[1] as List<Course>).filter { it.academyId == activeAcademyId }
    val activeCourseIds = activeCourses.map { it.id }.toSet()
    val activePaths = (values[2] as List<LearningPath>)
      .filter { it.academyId == activeAcademyId }
      .map { path ->
        val validPairs = path.courseIds.zip(path.courseTitles).filter { (cid, _) -> cid in activeCourseIds }
        if (validPairs.size == path.courseIds.size) path
        else path.copy(
          courseIds = validPairs.map { it.first },
          courseTitles = validPairs.map { it.second }
        )
      }
    val activeInstructors = (values[3] as List<Instructor>).filter { inst ->
      activeCourses.any { it.instructorId == inst.id } || inst.id == "inst_daniel_kato"
    }
    val activeAnnouncements = (values[4] as List<Announcement>).filter { ann ->
      ann.academyId.isNullOrBlank() || ann.academyId == activeAcademyId
    }
    HubCatalogSnapshot(
      academies = activeAcademies,
      courses = activeCourses,
      learningPaths = activePaths,
      instructors = activeInstructors,
      announcements = activeAnnouncements,
      plans = values[5] as List<SubscriptionPlan>
    )
  }.toUiStateFlow("catalogue")

  val studentState: StateFlow<UiState<StudentDataSnapshot>> = combine(
    studentRepository.observeUserProfile(currentUserId),
    studentRepository.observeEnrollments(currentUserId),
    studentRepository.observeLessonProgress(currentUserId),
    studentRepository.observeLearningPathProgress(currentUserId),
    studentRepository.observeSavedCourses(currentUserId),
    studentRepository.observeCertificates(currentUserId),
    studentRepository.observeSubscription(currentUserId),
    studentRepository.observeQuizAttempts(currentUserId),
    studentRepository.observeNotifications(currentUserId),
    studentRepository.observeCoursePurchases(currentUserId)
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    StudentDataSnapshot(
      profile = values[0] as UserProfile?,
      enrollments = values[1] as List<Enrollment>,
      lessonProgress = values[2] as List<LessonProgress>,
      pathProgress = values[3] as List<LearningPathProgress>,
      savedCourses = values[4] as List<SavedCourse>,
      certificates = values[5] as List<Certificate>,
      subscription = values[6] as Subscription?,
      quizAttempts = values[7] as List<QuizAttempt>,
      notifications = values[8] as List<NotificationItem>,
      coursePurchases = values[9] as List<CoursePurchase>
    )
  }.toUiStateFlow("student profile")

  init {
    bootstrapSession()
    observePlayBillingEvents()
  }

  private fun bootstrapSession() {
    viewModelScope.launch(Dispatchers.IO) {
      studentRepository.ensureUserProfileExists()
      val admin = catalogRepository.checkIsAdmin(currentUserId)
      _isAdmin.value = admin
      if (admin) {
        _isSeedingCatalog.value = true
        catalogSeeder.seedInitialCatalog(forceResync = false)
          .onSuccess { count ->
            if (count > 0) {
              _uiMessage.tryEmit("Synced AI Academy & 5 complete courses with full lessons, quizzes, assignments & practice kits.")
            }
          }
          .onFailure { e ->
            Log.w(TAG, "Starter catalog check failed", e)
            _uiMessage.tryEmit("Catalog sync issue: ${unwrapFirestoreMessage(e, "please retry from Home or Admin.")}")
          }
        _isSeedingCatalog.value = false
      }
    }
  }

  private fun observePlayBillingEvents() {
    viewModelScope.launch {
      playBillingManager.events.collect { event ->
        when (event) {
          is PlayBillingEvent.SubscriptionPurchased -> {
            val isAnnual = event.productId == PlayBillingManager.PRODUCT_PRO_ANNUAL
            val cal = Calendar.getInstance().apply {
              add(Calendar.DAY_OF_YEAR, if (isAnnual) 365 else 30)
            }
            val expiresText = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
            studentRepository.activatePlaySubscription(
              planId = if (isAnnual) "plan_annual_pro" else "plan_monthly_pro",
              planName = if (isAnnual) "Datalabs Pro Annual" else "Datalabs Pro Monthly",
              billingCycle = if (isAnnual) "ANNUAL" else "MONTHLY",
              amountUsd = if (isAnnual) 99.0 else 12.0,
              productId = event.productId,
              purchaseToken = event.purchaseToken,
              autoRenewing = event.autoRenewing,
              expiresAtText = expiresText
            ).onSuccess {
              _uiMessage.tryEmit("Datalabs Pro active via Google Play (renews $expiresText).")
            }.onFailure { e ->
              _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to sync Play subscription."))
            }
          }
          is PlayBillingEvent.CoursePurchased -> {
            val catalogCourses = (catalogState.value as? UiState.Success)?.data?.courses.orEmpty()
            val targetCourse = catalogCourses.firstOrNull { it.id == event.courseIdHint }
            if (targetCourse != null) {
              studentRepository.activatePlayCoursePurchase(
                course = targetCourse,
                productId = event.productId,
                purchaseToken = event.purchaseToken
              ).onSuccess {
                _uiMessage.tryEmit("${targetCourse.title} unlocked permanently!")
              }.onFailure { e ->
                _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to record course unlock."))
              }
            } else {
              _uiMessage.tryEmit("Google Play course purchase restored. Open the course to access content.")
            }
          }
          is PlayBillingEvent.Error -> _uiMessage.tryEmit(event.message)
          is PlayBillingEvent.Info -> _uiMessage.tryEmit(event.message)
        }
      }
    }
  }

  fun hasCourseAccess(course: Course, student: StudentDataSnapshot?): Boolean {
    if (!course.premiumOnly) return true
    if (student?.subscription?.activeEntitlement == true) return true
    return student?.coursePurchases?.any { it.courseId == course.id && it.activeUnlock } == true
  }

  fun askAiStudyAssistant(
    course: Course,
    lesson: Lesson,
    content: LessonContent?,
    mode: AiHelpMode,
    userPrompt: String
  ) {
    aiStreamJob?.cancel()
    val promptLabel = if (userPrompt.isNotBlank()) userPrompt.trim() else mode.label
    aiStreamJob = viewModelScope.launch {
      _aiAssistantState.value = AiAssistantUiState.Loading(promptLabel)
      try {
        val request = AiStudyRequest(
          courseTitle = course.title,
          academyName = course.academyName,
          level = course.level,
          lessonTitle = lesson.title,
          lessonSummary = lesson.summary,
          lessonNotesExcerpt = content?.notesMarkdown.orEmpty(),
          exercisePrompt = content?.exercisePrompt,
          mode = mode,
          userPrompt = userPrompt
        )
        var latest = ""
        aiAssistantService.streamStudyAssistance(request).collect { text ->
          latest = text
          _aiAssistantState.value = AiAssistantUiState.Streaming(promptLabel, text)
        }
        _aiAssistantState.value = AiAssistantUiState.Success(promptLabel, latest)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiAssistantState.value = AiAssistantUiState.Error(
          e.localizedMessage ?: "AI Study Assistant could not respond right now. Check connection and try again."
        )
      }
    }
  }

  fun clearAiAssistant() {
    aiStreamJob?.cancel()
    _aiAssistantState.value = AiAssistantUiState.Idle
  }

  fun ensureBillingConnected() {
    playBillingManager.ensureConnected()
  }

  fun launchPlaySubscriptionPurchase(activity: Activity, plan: SubscriptionPlan) {
    val productId = if (plan.billingCycle.equals("ANNUAL", ignoreCase = true)) {
      PlayBillingManager.PRODUCT_PRO_ANNUAL
    } else {
      PlayBillingManager.PRODUCT_PRO_MONTHLY
    }
    playBillingManager.launchSubscriptionPurchase(activity, productId)
  }

  fun launchPlayCoursePurchase(activity: Activity, course: Course) {
    playBillingManager.launchCoursePurchase(activity, course.id)
  }

  fun restorePlayPurchases() {
    playBillingManager.restorePurchases()
  }

  fun requestCoursePurchaseCheckout(course: Course, currency: String, provider: String) {
    viewModelScope.launch {
      studentRepository.requestCoursePurchaseCheckout(course, currency, provider)
        .onSuccess {
          _uiMessage.tryEmit("Single-course purchase request saved via $provider.")
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Could not request course purchase"))
        }
    }
  }

  fun observeLessons(courseId: String): Flow<List<Lesson>> =
    catalogRepository.observeLessonsForCourse(courseId)

  fun observeLessonsState(courseId: String): Flow<UiState<List<Lesson>>> =
    catalogRepository.observeLessonsForCourse(courseId)
      .map<List<Lesson>, UiState<List<Lesson>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing lessons for $courseId", firestoreError ?: error)
        emit(UiState.Error(unwrapFirestoreMessage(error, "Unable to load module lessons")))
      }

  fun observeQuizzes(courseId: String): Flow<List<Quiz>> =
    catalogRepository.observeQuizzesForCourse(courseId)

  fun observeAssignments(courseId: String): Flow<List<Assignment>> =
    catalogRepository.observeAssignmentsForCourse(courseId)

  suspend fun loadLessonContent(lessonId: String): Result<LessonContent> =
    catalogRepository.getLessonContent(lessonId)

  suspend fun loadAssignmentSubmission(assignmentId: String): AssignmentSubmission? =
    studentRepository.getAssignmentSubmission(assignmentId)

  fun trackCourseViewed(course: Course) {
    analyticsLogger.logCourseViewed(course.id, course.title, course.academyName)
  }

  fun enrollInCourse(course: Course, firstLesson: Lesson?) {
    viewModelScope.launch {
      studentRepository.enrollInCourse(course, firstLesson)
        .onSuccess {
          analyticsLogger.logCourseEnrolled(course.id, course.accessTier)
          _uiMessage.tryEmit("Enrolled in ${course.title}")
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Premium subscription required to enroll in this course."))
        }
    }
  }

  fun toggleSavedCourse(course: Course, currentlySaved: Boolean) {
    viewModelScope.launch {
      studentRepository.toggleSavedCourse(course, currentlySaved)
        .onSuccess {
          _uiMessage.tryEmit(if (currentlySaved) "Removed from saved courses" else "Saved ${course.title}")
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Unable to update saved courses"))
        }
    }
  }

  fun toggleCompareCourse(courseId: String) {
    val current = _compareCourseIds.value.toMutableList()
    if (current.contains(courseId)) {
      current.remove(courseId)
    } else {
      if (current.size >= 3) {
        _uiMessage.tryEmit("You can compare up to 3 courses at a time.")
        return
      }
      current.add(courseId)
    }
    _compareCourseIds.value = current
  }

  fun clearCompareCourses() {
    _compareCourseIds.value = emptyList()
  }

  fun completeLesson(
    course: Course,
    lesson: Lesson,
    allLessons: List<Lesson>,
    onCourseCompleted: () -> Unit
  ) {
    val student = (studentState.value as? UiState.Success)?.data ?: return
    val catalog = (catalogState.value as? UiState.Success)?.data
    val existingForCourse = student.lessonProgress.filter { it.courseId == course.id }
    val completedCourseIds = student.enrollments.filter { it.status == "COMPLETED" }.map { it.courseId }.toSet()

    viewModelScope.launch {
      studentRepository.completeLessonAndUpdateProgress(
        course = course,
        lesson = lesson,
        allCourseLessons = allLessons,
        existingProgressForCourse = existingForCourse,
        learningPaths = catalog?.learningPaths ?: emptyList(),
        completedCourseIds = completedCourseIds
      ).onSuccess { completedNow ->
        analyticsLogger.logLessonCompleted(course.id, lesson.id)
        if (completedNow) {
          notificationHelper.showLearningNotification(
            title = "All Lessons Finished!",
            body = "You finished the lessons in ${course.title}. Complete any pending quizzes and practical deliverable to unlock your certificate.",
            isCourseUpdate = true
          )
          maybeIssueSpecializationCertificate()
          onCourseCompleted()
        } else {
          _uiMessage.tryEmit("Lesson marked complete.")
        }
      }.onFailure { e ->
        _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Could not save lesson progress"))
      }
    }
  }

  fun saveLessonNotesAndState(
    courseId: String,
    lessonId: String,
    bookmarked: Boolean,
    lastPositionSeconds: Long,
    playbackSpeed: Double,
    studentNotes: String,
    silent: Boolean = false
  ) {
    viewModelScope.launch {
      studentRepository.saveLessonState(
        courseId = courseId,
        lessonId = lessonId,
        bookmarked = bookmarked,
        lastPositionSeconds = lastPositionSeconds,
        playbackSpeed = playbackSpeed,
        studentNotes = studentNotes
      ).onSuccess {
        if (!silent) _uiMessage.tryEmit("Lesson notes & bookmark saved.")
      }.onFailure { e ->
        if (!silent) _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to save notes"))
      }
    }
  }

  fun downloadAndShareMaterial(fileName: String, content: String) {
    viewModelScope.launch {
      storageAndPdf.saveDownloadableMaterialLocally(fileName, content)
        .onSuccess { uri ->
          storageAndPdf.shareUri(uri, "text/*", "Save or Share Learning Material")
        }
        .onFailure {
          _uiMessage.tryEmit("Could not prepare downloadable resource.")
        }
    }
  }

  fun observeMyAssignmentSubmission(assignmentId: String): Flow<AssignmentSubmission?> =
    studentRepository.observeAssignmentSubmission(assignmentId)

  fun issueCertificateForCourse(course: Course, onIssued: (Certificate) -> Unit = {}) {
    val student = (studentState.value as? UiState.Success)?.data
    val studentName = student?.profile?.fullName ?: "Datalabs Learner"
    viewModelScope.launch {
      val courseLessons = runCatching { observeLessons(course.id).first() }.getOrDefault(emptyList())
      val courseQuizzes = runCatching { observeQuizzes(course.id).first() }.getOrDefault(emptyList())
      val courseAssignments = runCatching { observeAssignments(course.id).first() }.getOrDefault(emptyList())
      val submissionMap = courseAssignments.associate { assign ->
        assign.id to studentRepository.getAssignmentSubmission(assign.id)
      }
      val completedIds = student?.lessonProgress
        ?.filter { it.courseId == course.id && it.completed }
        ?.map { it.lessonId }
        ?.toSet()
        ?: emptySet()

      val readiness = CourseCertificationPolicy.evaluateReadiness(
        lessons = courseLessons,
        completedLessonIds = completedIds,
        quizzes = courseQuizzes,
        quizAttempts = student?.quizAttempts ?: emptyList(),
        assignments = courseAssignments,
        assignmentSubmissions = submissionMap
      )
      if (!readiness.isEligible) {
        _uiMessage.tryEmit(
          readiness.blockingReasons.firstOrNull()
            ?: "Complete all required lessons, quizzes, and practical deliverables before claiming your certificate."
        )
        return@launch
      }

      studentRepository.issueCompletionCertificate(studentName, course)
        .onSuccess { cert ->
          analyticsLogger.logCertificateIssued(course.id, cert.certificateCode)
          _uiMessage.tryEmit("Certificate ${cert.certificateCode} issued!")
          onIssued(cert)
          maybeIssueSpecializationCertificate()
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Complete all lessons before claiming your certificate."))
        }
    }
  }

  fun claimAcademySpecializationCertificate(onIssued: (Certificate) -> Unit = {}) {
    val student = (studentState.value as? UiState.Success)?.data ?: return
    val catalog = (catalogState.value as? UiState.Success)?.data ?: return
    val mastery = com.example.data.seed.AcademyProgressionHelper.computeMastery(
      courses = catalog.courses.filter { it.academyId == AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID && it.published },
      enrollments = student.enrollments,
      certificates = student.certificates
    )
    if (!mastery.allRequiredCoursesCompleted) {
      _uiMessage.tryEmit("Complete all 5 AI Academy courses to unlock your Specialization Certificate.")
      return
    }
    val existing = mastery.specializationCertificate
    if (existing != null) {
      onIssued(existing)
      return
    }
    val studentName = student.profile?.fullName ?: "Datalabs Learner"
    viewModelScope.launch {
      studentRepository.issueSpecializationCertificate(studentName)
        .onSuccess { cert ->
          analyticsLogger.logCertificateIssued(cert.courseId, cert.certificateCode)
          _uiMessage.tryEmit("AI Academy Specialization Certificate ${cert.certificateCode} issued!")
          onIssued(cert)
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Unable to issue Specialization Certificate yet."))
        }
    }
  }

  private fun maybeIssueSpecializationCertificate() {
    val student = (studentState.value as? UiState.Success)?.data ?: return
    val catalog = (catalogState.value as? UiState.Success)?.data ?: return
    val mastery = com.example.data.seed.AcademyProgressionHelper.computeMastery(
      courses = catalog.courses.filter { it.academyId == AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID && it.published },
      enrollments = student.enrollments,
      certificates = student.certificates
    )
    if (mastery.allRequiredCoursesCompleted && mastery.specializationCertificate == null) {
      val studentName = student.profile?.fullName ?: "Datalabs Learner"
      viewModelScope.launch {
        studentRepository.issueSpecializationCertificate(studentName)
          .onSuccess { cert ->
            analyticsLogger.logCertificateIssued(cert.courseId, cert.certificateCode)
          }
      }
    }
  }

  fun exportAndShareCertificatePdf(certificate: Certificate) {
    viewModelScope.launch {
      storageAndPdf.generateCertificatePdfUri(certificate)
        .onSuccess { uri ->
          storageAndPdf.shareUri(uri, "application/pdf", "Share Datalabs Certificate PDF")
        }
        .onFailure {
          _uiMessage.tryEmit("Failed to generate PDF certificate.")
        }
    }
  }

  fun verifyCertificate(codeOrId: String) {
    if (codeOrId.isBlank()) {
      _uiMessage.tryEmit("Enter a Certificate ID or verification code.")
      return
    }
    viewModelScope.launch {
      _verifiedCertificateResult.value = studentRepository.verifyCertificateByCode(codeOrId)
    }
  }

  fun submitQuiz(
    quiz: Quiz,
    scorePercent: Long,
    earnedPoints: Long,
    totalPoints: Long,
    passed: Boolean,
    attemptNumber: Long,
    answersSummary: String
  ) {
    viewModelScope.launch {
      studentRepository.submitQuizAttempt(
        quiz = quiz,
        scorePercent = scorePercent,
        earnedPoints = earnedPoints,
        totalPoints = totalPoints,
        passed = passed,
        attemptNumber = attemptNumber,
        answersSummary = answersSummary
      ).onSuccess {
        _uiMessage.tryEmit(if (passed) "Assessment passed ($scorePercent%)!" else "Attempt recorded ($scorePercent%).")
      }.onFailure { e ->
        _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to submit assessment"))
      }
    }
  }

  fun submitAssignment(
    assignmentId: String,
    courseId: String,
    text: String,
    attachmentUrl: String?
  ) {
    viewModelScope.launch {
      studentRepository.submitAssignment(assignmentId, courseId, text, attachmentUrl)
        .onSuccess { _uiMessage.tryEmit("Assignment submitted for review.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to submit assignment")) }
    }
  }

  fun updateProfile(
    fullName: String,
    learningGoal: String,
    weeklyTargetHours: Long,
    careerInterests: List<String>,
    learningInterests: List<String>,
    remindersEnabled: Boolean,
    courseUpdatesEnabled: Boolean,
    announcementsEnabled: Boolean,
    achievementsEnabled: Boolean
  ) {
    viewModelScope.launch {
      studentRepository.updateProfileAndPreferences(
        fullName = fullName,
        learningGoal = learningGoal,
        weeklyTargetHours = weeklyTargetHours,
        careerInterests = careerInterests,
        learningInterests = learningInterests,
        remindersEnabled = remindersEnabled,
        courseUpdatesEnabled = courseUpdatesEnabled,
        announcementsEnabled = announcementsEnabled,
        achievementsEnabled = achievementsEnabled
      ).onSuccess {
        _uiMessage.tryEmit("Profile and preferences updated.")
      }.onFailure { e ->
        _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Unable to update profile"))
      }
    }
  }

  fun requestSubscriptionCheckout(plan: SubscriptionPlan, currency: String, provider: String) {
    viewModelScope.launch {
      studentRepository.requestSubscriptionCheckout(plan, currency, provider)
        .onSuccess {
          _uiMessage.tryEmit("Subscription request recorded via $provider. Awaiting gateway verification or admin approval.")
        }
        .onFailure { e ->
          _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Unable to initiate subscription"))
        }
    }
  }

  fun markNotificationRead(id: String) {
    viewModelScope.launch {
      studentRepository.markNotificationRead(id)
    }
  }

  fun sendLocalStudyReminder() {
    viewModelScope.launch {
      notificationHelper.showLearningNotification(
        title = "Datalabs Study Reminder",
        body = "Keep building your future — spend 15 minutes on your active course today.",
        isCourseUpdate = false
      )
      studentRepository.createNotification(
        title = "Daily Study Reminder",
        body = "You scheduled a learning reminder. Continue where you left off in My Learning.",
        type = "REMINDER",
        targetRoute = "my_learning"
      )
      _uiMessage.tryEmit("Learning reminder sent.")
    }
  }

  // --- ADMIN MANAGEMENT ACTIONS ---

  fun loadAdminOverview() {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      _adminOverview.value = UiState.Loading
      val students = catalogRepository.adminFetchAllStudents().getOrElse {
        _adminOverview.value = UiState.Error(unwrapFirestoreMessage(it, "Failed to load students"))
        return@launch
      }
      val enrollments = catalogRepository.adminFetchAllEnrollments().getOrDefault(emptyList())
      val subs = catalogRepository.adminFetchAllSubscriptions().getOrDefault(emptyList())
      val certs = catalogRepository.adminFetchAllCertificates().getOrDefault(emptyList())
      val submissions = catalogRepository.adminFetchAllSubmissions().getOrDefault(emptyList())
      val purchases = catalogRepository.adminFetchAllCoursePurchases().getOrDefault(emptyList())
      _adminOverview.value = UiState.Success(
        AdminOverviewData(
          students = students,
          enrollments = enrollments,
          subscriptions = subs,
          certificates = certs,
          submissions = submissions,
          coursePurchases = purchases
        )
      )
    }
  }

  fun adminResyncStarterCatalog() {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      _isSeedingCatalog.value = true
      catalogSeeder.seedInitialCatalog(forceResync = true)
        .onSuccess { ops -> _uiMessage.tryEmit("Synced starter catalogue ($ops Firestore writes).") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Catalogue sync failed")) }
      _isSeedingCatalog.value = false
    }
  }

  fun adminCreateOrUpdateAcademy(
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
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminUpsertAcademy(
        academyId, name, shortName, description, iconKey, accentHex, displayOrder, courseCount, published, isNew
      ).onSuccess { _uiMessage.tryEmit("Academy saved.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to save academy")) }
    }
  }

  fun adminCreateCourse(
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
    coverUri: Uri?
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val uploadedCover = if (coverUri != null) {
        storageAndPdf.uploadCourseAsset("covers", "${courseId}_cover.jpg", coverUri).getOrNull()
      } else null
      catalogRepository.adminUpsertCourse(
        courseId = courseId,
        academyId = academyId,
        academyName = academyName,
        title = title,
        subtitle = subtitle,
        description = description,
        instructorId = instructorId,
        instructorName = instructorName,
        level = level,
        durationMinutes = durationMinutes,
        accessTier = accessTier,
        priceUsd = priceUsd,
        priceUgx = priceUgx,
        certificateAvailable = true,
        published = true,
        featured = false,
        coverUrl = uploadedCover,
        whatYouWillLearn = listOf("Practical application of $title"),
        prerequisites = listOf("None"),
        formats = listOf("VIDEO", "WRITTEN"),
        tags = listOf(academyName, level),
        isNew = true
      ).onSuccess { _uiMessage.tryEmit("Course '$title' created.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to create course")) }
    }
  }

  fun adminToggleCoursePublish(course: Course) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminToggleCourseFlags(course.id, published = !course.published)
        .onSuccess { _uiMessage.tryEmit(if (course.published) "Course unpublished" else "Course published") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Update failed")) }
    }
  }

  fun adminToggleCourseFeatured(course: Course) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminToggleCourseFlags(course.id, featured = !course.featured)
        .onSuccess { _uiMessage.tryEmit(if (course.featured) "Removed from featured" else "Course featured on Home") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Update failed")) }
    }
  }

  fun adminToggleCourseTier(course: Course) {
    if (!_isAdmin.value) return
    val nextTier = if (course.premiumOnly) "FREE" else "PREMIUM"
    viewModelScope.launch {
      catalogRepository.adminToggleCourseFlags(course.id, accessTier = nextTier)
        .onSuccess { _uiMessage.tryEmit("Course set to $nextTier") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Update failed")) }
    }
  }

  fun adminAddLesson(
    course: Course,
    title: String,
    summary: String,
    orderIndex: Long,
    durationMinutes: Long,
    format: String,
    freePreview: Boolean,
    notesMarkdown: String,
    videoUrl: String?,
    downloadTitle: String?,
    attachmentUri: Uri?
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val lessonId = "${course.id}_l_${System.currentTimeMillis()}"
      val uploadedUrl = if (attachmentUri != null) {
        storageAndPdf.uploadCourseAsset("materials", downloadTitle ?: "resource", attachmentUri).getOrNull()
      } else null
      catalogRepository.adminAddLessonWithContent(
        lessonId = lessonId,
        courseId = course.id,
        academyId = course.academyId,
        title = title,
        summary = summary,
        orderIndex = orderIndex,
        durationMinutes = durationMinutes,
        format = format,
        freePreview = freePreview,
        premiumOnly = !freePreview && course.premiumOnly,
        notesMarkdown = notesMarkdown,
        videoUrl = videoUrl,
        downloadTitle = downloadTitle,
        downloadUrl = uploadedUrl,
        exercisePrompt = null
      ).onSuccess { _uiMessage.tryEmit("Lesson added to ${course.title}") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to add lesson")) }
    }
  }

  fun adminGrantSubscription(targetUserId: String, activate: Boolean) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminSetUserSubscription(
        targetUserId = targetUserId,
        status = if (activate) "ACTIVE" else "CANCELLED",
        planName = "Datalabs Pro (Admin Managed)",
        billingCycle = "ANNUAL",
        currency = "USD",
        amount = 99.0
      ).onSuccess {
        _uiMessage.tryEmit(if (activate) "Premium subscription activated for user." else "Subscription cancelled.")
        loadAdminOverview()
      }.onFailure { e ->
        _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update subscription"))
      }
    }
  }

  fun adminIssueCertificateForUser(targetUserId: String, studentName: String, course: Course) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminIssueCertificate(targetUserId, studentName, course)
        .onSuccess { cert ->
          _uiMessage.tryEmit("Issued certificate ${cert.certificateCode}")
          loadAdminOverview()
        }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to issue certificate")) }
    }
  }

  fun adminSendAnnouncement(title: String, message: String, category: String) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminBroadcastAnnouncement(title, message, category)
        .onSuccess { _uiMessage.tryEmit("Announcement published to all learners.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to publish announcement")) }
    }
  }

  fun adminCreateInstructor(fullName: String, title: String, bio: String, specialty: String) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val id = "inst_${System.currentTimeMillis()}"
      catalogRepository.adminCreateInstructor(id, fullName, title, bio, specialty)
        .onSuccess { _uiMessage.tryEmit("Instructor added.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to add instructor")) }
    }
  }

  fun adminUpdatePlan(plan: SubscriptionPlan, priceUsd: Double, priceUgx: Long) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminUpdateSubscriptionPlan(
        planId = plan.id,
        name = plan.name,
        billingCycle = plan.billingCycle,
        priceUsd = priceUsd,
        priceUgx = priceUgx,
        description = plan.description
      ).onSuccess { _uiMessage.tryEmit("Updated pricing for ${plan.name}") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update plan")) }
    }
  }

  fun adminCreateQuiz(
    courseId: String,
    title: String,
    description: String,
    passingScorePercent: Long,
    maxAttempts: Long,
    allowRetakes: Boolean,
    assessmentType: String = "FINAL_EXAM",
    questions: List<com.example.data.model.QuizQuestion>
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val quizId = "quiz_${courseId}_${System.currentTimeMillis()}"
      val questionsJson = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(com.example.data.model.QuizQuestion.serializer()),
        questions
      )
      catalogRepository.adminCreateQuiz(
        quizId = quizId,
        courseId = courseId,
        assessmentType = assessmentType,
        moduleIndex = if (assessmentType == "MODULE_QUIZ") 2L else 4L,
        title = title,
        description = description,
        passingScorePercent = passingScorePercent,
        maxAttempts = maxAttempts,
        allowRetakes = allowRetakes,
        questionsJson = questionsJson
      ).onSuccess { _uiMessage.tryEmit("Quiz '$title' added.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to create quiz")) }
    }
  }

  fun adminUpdateCourse(
    course: Course,
    title: String,
    subtitle: String,
    description: String,
    level: String,
    accessTier: String,
    priceUsd: Double,
    priceUgx: Long,
    category: String
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminUpsertCourse(
        courseId = course.id,
        academyId = course.academyId,
        academyName = course.academyName,
        title = title,
        subtitle = subtitle,
        description = description,
        instructorId = course.instructorId,
        instructorName = course.instructorName,
        level = level,
        durationMinutes = course.durationMinutes,
        accessTier = accessTier,
        priceUsd = priceUsd,
        priceUgx = priceUgx,
        certificateAvailable = course.certificateAvailable,
        published = course.published,
        featured = course.featured,
        coverUrl = course.coverUrl,
        whatYouWillLearn = course.whatYouWillLearn,
        prerequisites = course.prerequisites,
        formats = course.formats,
        tags = course.tags,
        category = category,
        categories = (listOf(category) + course.categories).filter { it.isNotBlank() }.distinct(),
        isNew = false
      ).onSuccess { _uiMessage.tryEmit("Updated course '$title'.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update course")) }
    }
  }

  fun adminUpdateLesson(
    lesson: Lesson,
    moduleTitle: String,
    title: String,
    summary: String,
    orderIndex: Long,
    durationMinutes: Long,
    format: String,
    freePreview: Boolean,
    notesMarkdown: String,
    videoUrl: String?,
    downloadTitle: String?
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminUpdateLessonWithContent(
        lessonId = lesson.id,
        moduleTitle = moduleTitle,
        title = title,
        summary = summary,
        orderIndex = orderIndex,
        durationMinutes = durationMinutes,
        format = format,
        freePreview = freePreview,
        premiumOnly = !freePreview,
        notesMarkdown = notesMarkdown,
        videoUrl = videoUrl,
        downloadTitle = downloadTitle
      ).onSuccess { _uiMessage.tryEmit("Updated lesson '$title'.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update lesson")) }
    }
  }

  fun adminUpdateQuiz(
    quiz: Quiz,
    title: String,
    description: String,
    passingScorePercent: Long,
    maxAttempts: Long,
    allowRetakes: Boolean,
    assessmentType: String,
    questions: List<com.example.data.model.QuizQuestion>
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val questionsJson = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(com.example.data.model.QuizQuestion.serializer()),
        questions
      )
      catalogRepository.adminUpdateQuiz(
        quizId = quiz.id,
        assessmentType = assessmentType,
        moduleIndex = if (assessmentType == "MODULE_QUIZ") 2L else 4L,
        title = title,
        description = description,
        passingScorePercent = passingScorePercent,
        maxAttempts = maxAttempts,
        allowRetakes = allowRetakes,
        questionsJson = questionsJson
      ).onSuccess { _uiMessage.tryEmit("Updated assessment '$title'.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update quiz")) }
    }
  }

  fun adminGradeSubmission(submission: AssignmentSubmission, score: Long, feedback: String) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminGradeSubmission(
        assignmentId = submission.assignmentId,
        targetUserId = submission.userId,
        score = score,
        feedback = feedback
      ).onSuccess {
        _uiMessage.tryEmit("Submission graded ($score pts).")
        loadAdminOverview()
      }.onFailure { e ->
        _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to grade submission"))
      }
    }
  }

  fun adminGrantCourseUnlock(targetUserId: String, course: Course, activate: Boolean) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminSetCoursePurchase(targetUserId, course, activate)
        .onSuccess {
          _uiMessage.tryEmit(if (activate) "Unlocked ${course.title} for learner." else "Revoked single-course access.")
          loadAdminOverview()
        }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to update course access")) }
    }
  }

  fun adminCreateAssignment(
    courseId: String,
    title: String,
    instructions: String,
    deliverableFormat: String,
    maxPoints: Long
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val assignmentId = "asg_${courseId}_${System.currentTimeMillis()}"
      catalogRepository.adminCreateAssignment(
        assignmentId = assignmentId,
        courseId = courseId,
        title = title,
        instructions = instructions,
        deliverableFormat = deliverableFormat,
        maxPoints = maxPoints
      ).onSuccess { _uiMessage.tryEmit("Assignment '$title' added.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to create assignment")) }
    }
  }

  fun adminCreateLearningPath(
    title: String,
    description: String,
    academyId: String,
    targetRole: String,
    selectedCourses: List<Course>,
    estimatedHours: Long,
    premiumOnly: Boolean
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      val pathId = "lp_${System.currentTimeMillis()}"
      catalogRepository.adminCreateLearningPath(
        pathId = pathId,
        title = title,
        description = description,
        academyId = academyId,
        targetRole = targetRole,
        courseIds = selectedCourses.map { it.id },
        courseTitles = selectedCourses.map { it.title },
        estimatedHours = estimatedHours,
        premiumOnly = premiumOnly
      ).onSuccess { _uiMessage.tryEmit("Learning Path '$title' created.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to create learning path")) }
    }
  }

  fun adminSendUserNotification(
    targetUserId: String,
    title: String,
    body: String,
    type: String
  ) {
    if (!_isAdmin.value) return
    viewModelScope.launch {
      catalogRepository.adminSendUserNotification(targetUserId, title, body, type)
        .onSuccess { _uiMessage.tryEmit("Notification sent to learner.") }
        .onFailure { e -> _uiMessage.tryEmit(unwrapFirestoreMessage(e, "Failed to send notification")) }
    }
  }

  fun factoryFor(userId: String): Nothing = error("Use companion factory")

  object Factory {
    fun create(app: DatalabsApplication, userId: String): ViewModelProvider.Factory =
      viewModelFactory {
        initializer {
          val c = app.container
          MainViewModel(
            currentUserId = userId,
            catalogRepository = c.catalogRepository,
            studentRepository = c.studentRepository,
            catalogSeeder = c.catalogSeeder,
            analyticsLogger = c.analyticsLogger,
            storageAndPdf = c.storageAndPdf,
            notificationHelper = c.notificationHelper,
            aiAssistantService = c.aiAssistantService,
            playBillingManager = c.playBillingManager
          )
        }
      }
  }
}
