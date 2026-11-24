package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.repository.findActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Course
import com.example.data.model.CourseCertificationPolicy
import com.example.data.model.Lesson
import com.example.data.model.Quiz
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.TierBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
  courseId: String,
  viewModel: MainViewModel,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit,
  onOpenLesson: (courseId: String, lessonId: String) -> Unit,
  onOpenQuiz: (quizId: String, courseId: String) -> Unit,
  onOpenSubscription: () -> Unit,
  onOpenCertificates: () -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView()
    return
  }
  val catalog = (catalogState as? UiState.Success)?.data ?: return
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val course = catalog.courses.firstOrNull { it.id == courseId }
  if (course == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Course Overview") },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
      ) {
        EmptyStateCard(
          icon = Icons.Filled.School,
          title = "Course Not Available",
          description = "Explore the active courses inside AI Academy.",
          actionLabel = "Go Back",
          onAction = onBack
        )
      }
    }
    return
  }
  val instructor = catalog.instructors.firstOrNull { it.id == course.instructorId }

  var lessonReloadKey by remember(courseId) { mutableIntStateOf(0) }
  val lessonsUiState by remember(courseId, lessonReloadKey) {
    viewModel.observeLessonsState(courseId)
  }.collectAsStateWithLifecycle(initialValue = UiState.Loading)
  val lessons = (lessonsUiState as? UiState.Success)?.data.orEmpty()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isSeedingCatalog by viewModel.isSeedingCatalog.collectAsStateWithLifecycle()

  val quizzes by viewModel.observeQuizzes(courseId).collectAsStateWithLifecycle(initialValue = emptyList())
  val assignments by viewModel.observeAssignments(courseId).collectAsStateWithLifecycle(initialValue = emptyList())
  val primaryAssignment = assignments.firstOrNull()
  val primarySubmission by remember(primaryAssignment?.id) {
    primaryAssignment?.let { viewModel.observeMyAssignmentSubmission(it.id) } ?: flowOf(null)
  }.collectAsStateWithLifecycle(initialValue = null)

  val activity = LocalContext.current.findActivity()
  val enrollment = student.enrollments.firstOrNull { it.courseId == courseId }
  val isEnrolled = enrollment != null
  val isSaved = student.savedCourses.any { it.courseId == courseId }
  val hasSubscription = student.subscription?.activeEntitlement == true
  val hasCourseAccess = viewModel.hasCourseAccess(course, student)
  val completedLessonIds = student.lessonProgress.filter { it.courseId == courseId && it.completed }.map { it.lessonId }.toSet()
  val existingCert = student.certificates.firstOrNull { it.courseId == courseId }
  val submissionMap = remember(primaryAssignment?.id, primarySubmission) {
    if (primaryAssignment != null) mapOf(primaryAssignment.id to primarySubmission) else emptyMap()
  }
  val readiness = remember(lessons, completedLessonIds, quizzes, student.quizAttempts, assignments, submissionMap) {
    CourseCertificationPolicy.evaluateReadiness(
      lessons = lessons,
      completedLessonIds = completedLessonIds,
      quizzes = quizzes,
      quizAttempts = student.quizAttempts,
      assignments = assignments,
      assignmentSubmissions = submissionMap
    )
  }

  var activeAssignmentDialog by remember { mutableStateOf<Assignment?>(null) }
  var existingSubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }
  var showSinglePurchaseDialog by remember { mutableStateOf(false) }

  LaunchedEffect(courseId) {
    viewModel.trackCourseViewed(course)
    viewModel.ensureBillingConnected()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Course Overview") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.toggleSavedCourse(course, isSaved) }) {
            Icon(
              imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
              contentDescription = if (isSaved) "Saved" else "Save course",
              tint = if (isSaved) ElectricBlue else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("course_detail_screen"),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hero Banner Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = NavyDeep
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              TierBadge(isPremium = course.premiumOnly, hasSubscription = hasCourseAccess)
              if (course.certificateAvailable) {
                Surface(
                  color = Color.White.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.WorkspacePremium,
                      contentDescription = null,
                      tint = Color(0xFFFCD34D),
                      modifier = Modifier.size(13.dp)
                    )
                    Text(
                      text = "Certificate Included",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White
                    )
                  }
                }
              }
            }

            Text(
              text = course.title,
              style = MaterialTheme.typography.displayMedium,
              color = Color.White
            )
            Text(
              text = course.subtitle,
              style = MaterialTheme.typography.bodyLarge,
              color = Color(0xFFCBD5E1)
            )
            val totalLessonCount = lessons.size.coerceAtLeast(course.lessonCount.toInt())
            val lessonLabel = if (totalLessonCount == 1) "lesson" else "lessons"
            val quizLabel = if (quizzes.size == 1) "quiz" else "quizzes"
            val assignmentLabel = if (assignments.size == 1) "assignment" else "assignments"
            Text(
              text = "${course.academyName} • ${course.level} • ${formatDuration(course.durationMinutes)} • $totalLessonCount $lessonLabel" +
                (if (quizzes.isNotEmpty()) " • ${quizzes.size} $quizLabel" else "") +
                (if (assignments.isNotEmpty()) " • ${assignments.size} $assignmentLabel" else ""),
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF93C5FD)
            )
            if (course.premiumOnly && !hasCourseAccess) {
              val ugxFormatted = NumberFormat.getIntegerInstance(Locale.US).format(course.priceUgx)
              Text(
                text = "Included with Datalabs Pro, or unlock single course for $${course.priceUsd.toInt()} USD / UGX $ugxFormatted",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFCD34D)
              )
            }

            if (isEnrolled) {
              Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                LinearProgressIndicator(
                  progress = { (enrollment.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  color = if (enrollment.status == "COMPLETED") SuccessEmerald else Color(0xFF60A5FA),
                  trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text(
                  text = "${enrollment.progressPercent}% complete (${enrollment.completedLessonsCount}/${enrollment.totalLessonsCount} lessons)",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFFBFDBFE)
                )
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              when {
                !hasCourseAccess -> {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                      Button(
                        onClick = onOpenSubscription,
                        modifier = Modifier
                          .weight(1f)
                          .testTag("upgrade_to_enroll_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningGold, contentColor = Color.White)
                      ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock with Pro")
                      }
                      OutlinedButton(
                        onClick = { showSinglePurchaseDialog = true },
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCD34D)),
                        modifier = Modifier.testTag("buy_single_course_button")
                      ) {
                        Text("Buy Course ($${course.priceUsd.toInt()})")
                      }
                    }
                    val previewLesson = lessons.firstOrNull { it.freePreview }
                    if (previewLesson != null) {
                      OutlinedButton(
                        onClick = { onOpenLesson(course.id, previewLesson.id) },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Text("Watch Free Preview Lesson")
                      }
                    }
                  }
                }
                !isEnrolled -> {
                  Button(
                    onClick = { viewModel.enrollInCourse(course, lessons.firstOrNull()) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("enroll_course_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                  ) {
                    Text("Enroll in Course")
                  }
                }
                else -> {
                  val nextLesson = lessons.firstOrNull { !completedLessonIds.contains(it.id) }
                    ?: lessons.firstOrNull()
                  if (nextLesson != null) {
                    Button(
                      onClick = { onOpenLesson(course.id, nextLesson.id) },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("start_or_continue_lesson_button"),
                      colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                    ) {
                      Text(if (enrollment.status == "COMPLETED") "Review Course" else "Continue Learning")
                    }
                  }
                  if (existingCert != null && course.certificateAvailable) {
                    Button(
                      onClick = onOpenCertificates,
                      colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald, contentColor = Color.White),
                      modifier = Modifier.testTag("view_course_certificate_button")
                    ) {
                      Text("View Certificate")
                    }
                  } else if (readiness.isEligible && course.certificateAvailable) {
                    Button(
                      onClick = { viewModel.issueCertificateForCourse(course) { onOpenCertificates() } },
                      colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald, contentColor = Color.White),
                      modifier = Modifier.testTag("claim_course_certificate_button")
                    ) {
                      Text("Claim Certificate")
                    }
                  } else if (readiness.lessonsComplete && course.certificateAvailable) {
                    OutlinedButton(
                      onClick = {
                        when {
                          readiness.moduleQuiz != null && !readiness.moduleQuizPassed ->
                            onOpenQuiz(readiness.moduleQuiz.id, courseId)
                          readiness.finalQuiz != null && !readiness.finalQuizPassed ->
                            onOpenQuiz(readiness.finalQuiz.id, courseId)
                          readiness.requiredAssignment != null && !readiness.assignmentSubmitted ->
                            activeAssignmentDialog = readiness.requiredAssignment
                        }
                      },
                      border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCD34D)),
                      modifier = Modifier.testTag("complete_assessments_button")
                    ) {
                      Text("Finish Assessments")
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Description & What Students Will Learn
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text("About this course", style = MaterialTheme.typography.titleMedium)
            Text(course.description, style = MaterialTheme.typography.bodyMedium)

            if (course.whatYouWillLearn.isNotEmpty()) {
              Text("What you will learn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              course.whatYouWillLearn.forEach { point ->
                Row(
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessEmerald,
                    modifier = Modifier
                      .padding(top = 2.dp)
                      .size(16.dp)
                  )
                  Text(point, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }

            if (course.prerequisites.isNotEmpty()) {
              Text("Prerequisites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(course.prerequisites.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (course.formats.isNotEmpty()) {
              Text("Included formats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(
                text = course.formats.joinToString(" • ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
                style = MaterialTheme.typography.bodySmall,
                color = ElectricBlue
              )
            }
          }
        }
      }

      // Instructor Bio
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NavyDeep),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = course.instructorName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(course.instructorName, style = MaterialTheme.typography.titleMedium)
              Text(
                text = instructor?.title ?: "Lead Instructor • ${course.academyName}",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricBlue
              )
              if (instructor != null) {
                Text(instructor.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      // Teaching Modules & Curriculum
      item {
        Column(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "Teaching Modules & Lessons (${lessons.size})",
            style = MaterialTheme.typography.titleLarge
          )
          Text(
            text = "Tap any module below to read lesson notes, explore slides, review the case study, and practice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      when {
        lessonsUiState is UiState.Loading || (isSeedingCatalog && lessons.isEmpty()) -> {
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("lessons_loading_card"),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ElectricBlue)
                Text(
                  text = if (isSeedingCatalog) {
                    "Syncing AI Academy teaching modules..."
                  } else {
                    "Syncing module lessons..."
                  },
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }
        lessonsUiState is UiState.Error -> {
          item {
            ErrorStateCard(
              message = (lessonsUiState as UiState.Error).message,
              onRetry = {
                if (isAdmin) {
                  viewModel.adminResyncStarterCatalog()
                }
                lessonReloadKey++
              }
            )
          }
        }
        lessons.isEmpty() -> {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              EmptyStateCard(
                icon = Icons.Filled.School,
                title = "No Teaching Modules Loaded Yet",
                description = if (isAdmin) {
                  "Tap below to populate all 4 teaching modules (notes, slides, case studies, and practice kits) for this course."
                } else {
                  "Course lessons are syncing or unavailable right now. Tap Refresh to check again."
                },
                actionLabel = if (isAdmin) "Sync Teaching Modules" else "Refresh Modules",
                onAction = {
                  if (isAdmin) {
                    viewModel.adminResyncStarterCatalog()
                  } else {
                    lessonReloadKey++
                  }
                }
              )
            }
          }
        }
        else -> {
          items(lessons, key = { it.id }) { lesson ->
            val unlocked = lesson.freePreview || !lesson.premiumOnly || hasCourseAccess
            val done = completedLessonIds.contains(lesson.id)
            LessonCurriculumRow(
              lesson = lesson,
              unlocked = unlocked,
              completed = done,
              onClick = {
                if (unlocked) {
                  onOpenLesson(course.id, lesson.id)
                } else {
                  onOpenSubscription()
                }
              }
            )
          }
        }
      }

      // Quizzes
      if (quizzes.isNotEmpty()) {
        item {
          Text(
            text = "Assessments & Quizzes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
        items(quizzes, key = { it.id }) { quiz ->
          val attempts = student.quizAttempts.filter { it.quizId == quiz.id }
          val passed = attempts.any { it.passed }
          QuizEntryCard(
            quiz = quiz,
            attemptsCount = attempts.size,
            passed = passed,
            onClick = { onOpenQuiz(quiz.id, courseId) }
          )
        }
      }

      // Practical Assignments
      if (assignments.isNotEmpty()) {
        item {
          Text(
            text = "Practical Assignments & Projects",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
        items(assignments, key = { it.id }) { assignment ->
          val sub by remember(assignment.id, primaryAssignment?.id, primarySubmission) {
            if (assignment.id == primaryAssignment?.id) flowOf(primarySubmission)
            else viewModel.observeMyAssignmentSubmission(assignment.id)
          }.collectAsStateWithLifecycle(initialValue = null)
          val isReviewed = sub?.status == "REVIEWED"
          val isSubmitted = sub != null

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clickable { activeAssignmentDialog = assignment }
              .testTag("assignment_card_${assignment.id}"),
            border = BorderStroke(
              1.dp,
              if (isSubmitted) SuccessEmerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = if (isSubmitted) SuccessEmerald else ElectricBlue
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                  color = when {
                    isReviewed -> SuccessEmerald.copy(alpha = 0.14f)
                    isSubmitted -> ElectricBlue.copy(alpha = 0.14f)
                    else -> WarningGold.copy(alpha = 0.14f)
                  },
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = when {
                      isReviewed -> "REVIEWED${sub?.score?.let { " • $it/${assignment.maxPoints} pts" } ?: ""}"
                      isSubmitted -> "SUBMITTED • READY"
                      else -> "REQUIRED DELIVERABLE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                      isReviewed -> SuccessEmerald
                      isSubmitted -> ElectricBlue
                      else -> WarningGold
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
                Text(assignment.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${assignment.deliverableFormat} • ${assignment.maxPoints} pts",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = if (isSubmitted) "View / Edit" else "Submit",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSubmitted) SuccessEmerald else ElectricBlue
              )
            }
          }
        }
      }
      // Certificate Readiness Checklist (when course awards certificate)
      if (course.certificateAvailable) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag("certificate_requirements_card"),
            border = BorderStroke(
              1.dp,
              if (readiness.isEligible) SuccessEmerald else MaterialTheme.colorScheme.outline
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Certificate Requirements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                  text = if (readiness.isEligible) "All Met" else "${readiness.blockingReasons.size} remaining",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (readiness.isEligible) SuccessEmerald else WarningGold,
                  fontWeight = FontWeight.Bold
                )
              }

              RequirementStatusRow(
                met = readiness.lessonsComplete,
                label = "Complete all ${readiness.totalLessons} curriculum lessons (${readiness.completedLessons}/${readiness.totalLessons})",
                onClick = if (!readiness.lessonsComplete && lessons.isNotEmpty() && hasCourseAccess) {
                  {
                    val nextLesson = lessons.firstOrNull { !completedLessonIds.contains(it.id) } ?: lessons.first()
                    onOpenLesson(course.id, nextLesson.id)
                  }
                } else null
              )

              readiness.moduleQuiz?.let { mq ->
                RequirementStatusRow(
                  met = readiness.moduleQuizPassed,
                  label = "Pass ${mq.title} (${mq.passingScorePercent}%+)",
                  onClick = { onOpenQuiz(mq.id, courseId) }
                )
              }

              readiness.finalQuiz?.let { fq ->
                RequirementStatusRow(
                  met = readiness.finalQuizPassed,
                  label = "Pass ${fq.title} (${fq.passingScorePercent}%+)",
                  onClick = { onOpenQuiz(fq.id, courseId) }
                )
              }

              readiness.requiredAssignment?.let { reqAssign ->
                RequirementStatusRow(
                  met = readiness.assignmentSubmitted,
                  label = "Submit Practical Deliverable: ${reqAssign.title}",
                  onClick = { activeAssignmentDialog = reqAssign }
                )
              }
            }
          }
        }
      }
    }
  }

  if (showSinglePurchaseDialog) {
    val ugxFormatted = NumberFormat.getIntegerInstance(Locale.US).format(course.priceUgx)
    AlertDialog(
      onDismissRequest = { showSinglePurchaseDialog = false },
      title = { Text("Unlock ${course.title}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Choose lifetime single-course unlock ($${course.priceUsd.toInt()} USD / UGX $ugxFormatted) or upgrade to Datalabs Pro for full AI Academy access.",
            style = MaterialTheme.typography.bodySmall
          )
          Button(
            onClick = {
              showSinglePurchaseDialog = false
              if (activity != null) {
                viewModel.launchPlayCoursePurchase(activity, course)
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Purchase via Google Play")
          }
          OutlinedButton(
            onClick = {
              showSinglePurchaseDialog = false
              viewModel.requestCoursePurchaseCheckout(course, "UGX", "MTN_MOMO")
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Request Mobile Money Unlock (UGX)")
          }
          OutlinedButton(
            onClick = {
              showSinglePurchaseDialog = false
              onOpenSubscription()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Compare Datalabs Pro Plans")
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSinglePurchaseDialog = false }) { Text("Close") }
      }
    )
  }

  activeAssignmentDialog?.let { assignment ->
    LaunchedEffect(assignment.id) {
      existingSubmission = viewModel.loadAssignmentSubmission(assignment.id)
    }
    var submissionText by remember(assignment.id, existingSubmission) {
      mutableStateOf(existingSubmission?.submissionText ?: "")
    }
    var attachmentUrl by remember(assignment.id, existingSubmission) {
      mutableStateOf(existingSubmission?.attachmentUrl ?: "")
    }
    AlertDialog(
      onDismissRequest = { activeAssignmentDialog = null },
      title = { Text(assignment.title) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(assignment.instructions, style = MaterialTheme.typography.bodySmall)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Evaluation Rubric (${assignment.maxPoints} pts)",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "• Problem & Scope Clarity (25%)\n• Accurate Practical Execution (40%)\n• Verification & Edge-Case Guardrails (20%)\n• Crisp Executive Summary (15%)",
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
          if (existingSubmission != null) {
            Surface(
              color = SuccessEmerald.copy(alpha = 0.12f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Status: ${existingSubmission!!.status}" +
                    (existingSubmission!!.score?.let { " • Score: $it/${assignment.maxPoints}" } ?: ""),
                  style = MaterialTheme.typography.labelMedium,
                  color = SuccessEmerald,
                  fontWeight = FontWeight.Bold
                )
                existingSubmission!!.feedback?.takeIf { it.isNotBlank() }?.let { fb ->
                  Text("Feedback: $fb", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
          OutlinedTextField(
            value = submissionText,
            onValueChange = { submissionText = it },
            label = { Text("Your solution / summary") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("assignment_submission_input")
          )
          OutlinedTextField(
            value = attachmentUrl,
            onValueChange = { attachmentUrl = it },
            label = { Text("Optional portfolio/drive link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (submissionText.isNotBlank()) {
              viewModel.submitAssignment(assignment.id, courseId, submissionText, attachmentUrl)
              activeAssignmentDialog = null
            }
          },
          enabled = submissionText.isNotBlank() && existingSubmission?.status != "REVIEWED",
          modifier = Modifier.testTag("submit_assignment_button")
        ) {
          Text(if (existingSubmission != null) "Update Submission" else "Submit")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeAssignmentDialog = null }) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun RequirementStatusRow(
  met: Boolean,
  label: String,
  onClick: (() -> Unit)?
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = 4.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      imageVector = Icons.Filled.CheckCircle,
      contentDescription = null,
      tint = if (met) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = if (met) MaterialTheme.colorScheme.onSurface else ElectricBlue,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun LessonCurriculumRow(
  lesson: Lesson,
  unlocked: Boolean,
  completed: Boolean,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("lesson_row_${lesson.id}"),
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = when {
          completed -> Icons.Filled.CheckCircle
          unlocked -> Icons.Filled.PlayCircle
          else -> Icons.Filled.Lock
        },
        contentDescription = null,
        tint = when {
          completed -> SuccessEmerald
          unlocked -> ElectricBlue
          else -> WarningGold
        },
        modifier = Modifier.size(26.dp)
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (lesson.moduleTitle.isNotBlank()) {
          Text(
            text = lesson.moduleTitle,
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            fontWeight = FontWeight.SemiBold
          )
        }
        Text(lesson.title, style = MaterialTheme.typography.titleSmall)
        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val formatLabel = when (lesson.format.uppercase()) {
          "WRITTEN" -> "Teaching Guide & Examples"
          "SLIDES" -> "Slides & Worked Examples"
          "EXERCISE" -> "Teaching + Guided Lab"
          "PROJECT" -> "Teaching + Capstone Guide"
          else -> lesson.format.lowercase().replaceFirstChar { it.uppercase() }
        }
        Text(
          text = "$formatLabel • Case Study • ${lesson.durationMinutes} min" + if (lesson.freePreview) " • Free Preview" else "",
          style = MaterialTheme.typography.labelSmall,
          color = if (lesson.freePreview) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = when {
          completed -> "Review"
          unlocked -> "Read Lesson"
          else -> "Unlock"
        },
        style = MaterialTheme.typography.labelLarge,
        color = when {
          completed -> SuccessEmerald
          unlocked -> ElectricBlue
          else -> WarningGold
        }
      )
    }
  }
}

@Composable
private fun QuizEntryCard(
  quiz: Quiz,
  attemptsCount: Int,
  passed: Boolean,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .clickable(onClick = onClick)
      .testTag("quiz_card_${quiz.id}"),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Filled.Quiz,
        contentDescription = null,
        tint = if (passed) SuccessEmerald else ElectricBlue
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (quiz.assessmentType == "MODULE_QUIZ") "MODULE QUIZ" else "FINAL EXAM",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricBlue,
          fontWeight = FontWeight.Bold
        )
        Text(quiz.title, style = MaterialTheme.typography.titleSmall)
        Text(
          text = "Passing score: ${quiz.passingScorePercent}% • Attempts: $attemptsCount/${quiz.maxAttempts}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = if (passed) "Passed" else "Start",
        style = MaterialTheme.typography.labelLarge,
        color = if (passed) SuccessEmerald else ElectricBlue
      )
    }
  }
}
