package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Course
import com.example.data.model.Lesson
import com.example.data.model.Quiz
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile
import com.example.data.seed.CategoryDefinitions
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.AdminOverviewData
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.Json

@Composable
fun AdminEditCourseDialog(
  course: Course,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    subtitle: String,
    description: String,
    level: String,
    accessTier: String,
    priceUsd: Double,
    priceUgx: Long,
    category: String
  ) -> Unit
) {
  var title by remember(course.id) { mutableStateOf(course.title) }
  var subtitle by remember(course.id) { mutableStateOf(course.subtitle) }
  var description by remember(course.id) { mutableStateOf(course.description) }
  var level by remember(course.id) { mutableStateOf(course.level) }
  var tier by remember(course.id) { mutableStateOf(course.accessTier) }
  var priceUsd by remember(course.id) { mutableStateOf(course.priceUsd.toString()) }
  var priceUgx by remember(course.id) { mutableStateOf(course.priceUgx.toString()) }
  var category by remember(course.id) {
    mutableStateOf(
      course.category.ifBlank { CategoryDefinitions.resolvePrimaryCategory(course.academyId, course.title) }
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Course") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("BEGINNER", "INTERMEDIATE", "ADVANCED").forEach { lvl ->
            FilterChip(selected = level == lvl, onClick = { level = lvl }, label = { Text(lvl) })
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("FREE", "PREMIUM").forEach { t ->
            FilterChip(selected = tier == t, onClick = { tier = t }, label = { Text(t) })
          }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CategoryDefinitions.categories.forEach { cat ->
            FilterChip(selected = category == cat.name, onClick = { category = cat.name }, label = { Text(cat.name) })
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = priceUsd, onValueChange = { priceUsd = it }, label = { Text("USD") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = priceUgx, onValueChange = { priceUgx = it }, label = { Text("UGX") }, singleLine = true, modifier = Modifier.weight(1f))
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            title.trim(),
            subtitle.trim(),
            description.trim(),
            level,
            tier,
            if (tier == "FREE") 0.0 else (priceUsd.toDoubleOrNull() ?: 19.0),
            if (tier == "FREE") 0L else (priceUgx.toLongOrNull() ?: 70000L),
            category
          )
        },
        enabled = title.trim().length >= 2 && subtitle.trim().length >= 2 && description.trim().length >= 4
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLessonAndQuizEditorsCard(
  viewModel: MainViewModel,
  courses: List<Course>
) {
  var selectedCourse by remember(courses) { mutableStateOf(courses.firstOrNull()) }
  var courseExpanded by remember { mutableStateOf(false) }

  val targetCourseId = selectedCourse?.id.orEmpty()
  val lessons by remember(targetCourseId) {
    viewModel.observeLessons(targetCourseId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val quizzes by remember(targetCourseId) {
    viewModel.observeQuizzes(targetCourseId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  var editingLesson by remember { mutableStateOf<Lesson?>(null) }
  var editingQuiz by remember { mutableStateOf<Quiz?>(null) }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Edit, contentDescription = null, tint = ElectricBlue)
        Text("Edit Existing Lessons & Quizzes", style = MaterialTheme.typography.titleMedium)
      }

      ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }) {
        OutlinedTextField(
          value = selectedCourse?.title ?: "Select Course",
          onValueChange = {},
          readOnly = true,
          label = { Text("Inspect Course Content") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
          courses.take(50).forEach { crs ->
            DropdownMenuItem(
              text = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              onClick = { selectedCourse = crs; courseExpanded = false }
            )
          }
        }
      }

      if (lessons.isNotEmpty()) {
        Text("Lessons (${lessons.size})", style = MaterialTheme.typography.labelLarge)
        lessons.forEach { lesson ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${lesson.orderIndex}. ${lesson.title}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  text = "${lesson.moduleTitle.ifBlank { "Module" }} • ${lesson.durationMinutes} min • ${lesson.format}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              OutlinedButton(
                onClick = { editingLesson = lesson },
                modifier = Modifier.testTag("admin_edit_lesson_${lesson.id}")
              ) {
                Text("Edit")
              }
            }
          }
        }
      }

      if (quizzes.isNotEmpty()) {
        Text("Assessments (${quizzes.size})", style = MaterialTheme.typography.labelLarge)
        quizzes.forEach { quiz ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  text = "${quiz.assessmentType} • Pass: ${quiz.passingScorePercent}% • Retakes: ${quiz.allowRetakes}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              OutlinedButton(
                onClick = { editingQuiz = quiz },
                modifier = Modifier.testTag("admin_edit_quiz_${quiz.id}")
              ) {
                Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
              }
            }
          }
        }
      }
    }
  }

  editingLesson?.let { lesson ->
    var loadedNotes by remember(lesson.id) { mutableStateOf("") }
    var loadedVideoUrl by remember(lesson.id) { mutableStateOf("") }
    var loadedDownloadTitle by remember(lesson.id) { mutableStateOf("") }
    var contentLoaded by remember(lesson.id) { mutableStateOf(false) }

    LaunchedEffect(lesson.id) {
      viewModel.loadLessonContent(lesson.id).onSuccess { c ->
        loadedNotes = c.notesMarkdown
        loadedVideoUrl = c.videoUrl.orEmpty()
        loadedDownloadTitle = c.downloadTitle.orEmpty()
      }
      contentLoaded = true
    }

    var moduleTitle by remember(lesson.id) { mutableStateOf(lesson.moduleTitle) }
    var title by remember(lesson.id) { mutableStateOf(lesson.title) }
    var summary by remember(lesson.id) { mutableStateOf(lesson.summary) }
    var orderText by remember(lesson.id) { mutableStateOf(lesson.orderIndex.toString()) }
    var durationText by remember(lesson.id) { mutableStateOf(lesson.durationMinutes.toString()) }
    var format by remember(lesson.id) { mutableStateOf(lesson.format) }
    var freePreview by remember(lesson.id) { mutableStateOf(lesson.freePreview) }

    AlertDialog(
      onDismissRequest = { editingLesson = null },
      title = { Text("Edit Lesson") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(value = moduleTitle, onValueChange = { moduleTitle = it }, label = { Text("Module Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Lesson Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = orderText, onValueChange = { orderText = it }, label = { Text("Order") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = durationText, onValueChange = { durationText = it }, label = { Text("Minutes") }, singleLine = true, modifier = Modifier.weight(1f))
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("WRITTEN", "VIDEO", "SLIDES", "EXERCISE", "PROJECT").forEach { fmt ->
              FilterChip(selected = format == fmt, onClick = { format = fmt }, label = { Text(fmt) })
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Free Preview", style = MaterialTheme.typography.bodySmall)
            Switch(checked = freePreview, onCheckedChange = { freePreview = it })
          }
          OutlinedTextField(
            value = loadedNotes,
            onValueChange = { loadedNotes = it },
            label = { Text(if (contentLoaded) "Lesson Study Notes (Markdown)" else "Loading notes...") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(value = loadedVideoUrl, onValueChange = { loadedVideoUrl = it }, label = { Text("Video/Audio URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = loadedDownloadTitle, onValueChange = { loadedDownloadTitle = it }, label = { Text("Downloadable Kit Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminUpdateLesson(
              lesson = lesson,
              moduleTitle = moduleTitle.trim(),
              title = title.trim(),
              summary = summary.trim(),
              orderIndex = orderText.toLongOrNull()?.coerceAtLeast(1L) ?: lesson.orderIndex,
              durationMinutes = durationText.toLongOrNull()?.coerceAtLeast(1L) ?: lesson.durationMinutes,
              format = format,
              freePreview = freePreview,
              notesMarkdown = loadedNotes.ifBlank { lesson.summary },
              videoUrl = loadedVideoUrl.takeIf { it.isNotBlank() },
              downloadTitle = loadedDownloadTitle.takeIf { it.isNotBlank() }
            )
            editingLesson = null
          },
          enabled = title.trim().length >= 2 && summary.trim().length >= 2
        ) {
          Text("Save Lesson")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingLesson = null }) { Text("Cancel") }
      }
    )
  }

  editingQuiz?.let { quiz ->
    val existingQuestions = remember(quiz.id, quiz.questionsJson) {
      runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<List<QuizQuestion>>(quiz.questionsJson)
      }.getOrDefault(emptyList())
    }
    var qTitle by remember(quiz.id) { mutableStateOf(quiz.title) }
    var qDesc by remember(quiz.id) { mutableStateOf(quiz.description) }
    var qPass by remember(quiz.id) { mutableStateOf(quiz.passingScorePercent.toString()) }
    var qAttempts by remember(quiz.id) { mutableStateOf(quiz.maxAttempts.toString()) }
    var qRetakes by remember(quiz.id) { mutableStateOf(quiz.allowRetakes) }
    var qType by remember(quiz.id) { mutableStateOf(quiz.assessmentType) }

    AlertDialog(
      onDismissRequest = { editingQuiz = null },
      title = { Text("Edit Assessment Settings") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(value = qTitle, onValueChange = { qTitle = it }, label = { Text("Assessment Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = qDesc, onValueChange = { qDesc = it }, label = { Text("Instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = qType == "MODULE_QUIZ", onClick = { qType = "MODULE_QUIZ" }, label = { Text("Module Quiz") })
            FilterChip(selected = qType == "FINAL_EXAM", onClick = { qType = "FINAL_EXAM" }, label = { Text("Final Exam") })
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = qPass, onValueChange = { qPass = it }, label = { Text("Pass %") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = qAttempts, onValueChange = { qAttempts = it }, label = { Text("Max Attempts") }, singleLine = true, modifier = Modifier.weight(1f))
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Allow Retakes", style = MaterialTheme.typography.bodySmall)
            Switch(checked = qRetakes, onCheckedChange = { qRetakes = it })
          }
          Text(
            text = "${existingQuestions.size} questions preserved cleanly.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminUpdateQuiz(
              quiz = quiz,
              title = qTitle.trim(),
              description = qDesc.trim(),
              passingScorePercent = qPass.toLongOrNull()?.coerceIn(1L, 100L) ?: 70L,
              maxAttempts = qAttempts.toLongOrNull()?.coerceIn(1L, 50L) ?: 5L,
              allowRetakes = qRetakes,
              assessmentType = qType,
              questions = existingQuestions
            )
            editingQuiz = null
          },
          enabled = qTitle.trim().length >= 2 && qDesc.trim().length >= 2
        ) {
          Text("Save Assessment")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingQuiz = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun AdminSubmissionReviewCard(
  viewModel: MainViewModel,
  overview: AdminOverviewData,
  courses: List<Course>
) {
  val studentsById = remember(overview.students) { overview.students.associateBy { it.userId } }
  val coursesById = remember(courses) { courses.associateBy { it.id } }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = ElectricBlue)
        Text("Assignment Review & Grading (${overview.submissions.size})", style = MaterialTheme.typography.titleMedium)
      }

      if (overview.submissions.isEmpty()) {
        Text(
          text = "No learner assignment submissions pending review yet.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        overview.submissions.take(25).forEach { sub ->
          SubmissionReviewRow(
            submission = sub,
            studentName = studentsById[sub.userId]?.fullName ?: sub.userId,
            courseTitle = coursesById[sub.courseId]?.title ?: sub.courseId,
            onSaveGrade = { score, feedback ->
              viewModel.adminGradeSubmission(sub, score, feedback)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun SubmissionReviewRow(
  submission: AssignmentSubmission,
  studentName: String,
  courseTitle: String,
  onSaveGrade: (Long, String) -> Unit
) {
  var scoreText by remember(submission.id, submission.score) { mutableStateOf((submission.score ?: 85L).toString()) }
  var feedbackText by remember(submission.id, submission.feedback) { mutableStateOf(submission.feedback.orEmpty()) }
  val isGraded = submission.status.equals("GRADED", ignoreCase = true)

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(studentName, style = MaterialTheme.typography.titleSmall)
          Text(courseTitle, style = MaterialTheme.typography.bodySmall, color = ElectricBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(
          color = if (isGraded) SuccessEmerald.copy(alpha = 0.14f) else WarningGold.copy(alpha = 0.16f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = submission.status,
            style = MaterialTheme.typography.labelSmall,
            color = if (isGraded) SuccessEmerald else WarningGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Text(submission.submissionText, style = MaterialTheme.typography.bodySmall)
      if (!submission.attachmentUrl.isNullOrBlank()) {
        Text(
          text = "Link: ${submission.attachmentUrl}",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricBlue
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = scoreText,
          onValueChange = { scoreText = it },
          label = { Text("Score") },
          singleLine = true,
          modifier = Modifier.width(88.dp)
        )
        OutlinedTextField(
          value = feedbackText,
          onValueChange = { feedbackText = it },
          label = { Text("Instructor Feedback") },
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
      }

      Button(
        onClick = {
          val score = scoreText.toLongOrNull()?.coerceIn(0L, 1000L) ?: 85L
          onSaveGrade(score, feedbackText.trim().ifBlank { "Reviewed and approved by Datalabs Faculty." })
        },
        modifier = Modifier.fillMaxWidth().testTag("admin_grade_${submission.id}")
      ) {
        Icon(Icons.Filled.Grade, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (isGraded) "Update Grade" else "Submit Grade & Feedback")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSingleCourseUnlockSection(
  viewModel: MainViewModel,
  students: List<UserProfile>,
  courses: List<Course>,
  overview: AdminOverviewData
) {
  var selectedStudent by remember(students) { mutableStateOf(students.firstOrNull()) }
  var studentExpanded by remember { mutableStateOf(false) }

  val premiumCourses = remember(courses) { courses.filter { it.premiumOnly } }
  var selectedCourse by remember(premiumCourses) { mutableStateOf(premiumCourses.firstOrNull() ?: courses.firstOrNull()) }
  var courseExpanded by remember { mutableStateOf(false) }

  if (students.isEmpty() || selectedCourse == null) return

  val isUnlocked = overview.coursePurchases.any {
    it.userId == selectedStudent?.userId && it.courseId == selectedCourse?.id && it.activeUnlock
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.LockOpen, contentDescription = null, tint = ElectricBlue)
        Text("Single-Course Purchase Unlocks (${overview.coursePurchases.size})", style = MaterialTheme.typography.titleMedium)
      }

      ExposedDropdownMenuBox(expanded = studentExpanded, onExpandedChange = { studentExpanded = !studentExpanded }) {
        OutlinedTextField(
          value = selectedStudent?.let { "${it.fullName} (${it.email})" } ?: "Select Learner",
          onValueChange = {},
          readOnly = true,
          label = { Text("Learner") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentExpanded) },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = studentExpanded, onDismissRequest = { studentExpanded = false }) {
          students.forEach { st ->
            DropdownMenuItem(text = { Text("${st.fullName} (${st.email})") }, onClick = { selectedStudent = st; studentExpanded = false })
          }
        }
      }

      ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }) {
        OutlinedTextField(
          value = selectedCourse?.title ?: "Select Course",
          onValueChange = {},
          readOnly = true,
          label = { Text("Premium Course") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
          (premiumCourses.ifEmpty { courses }).take(50).forEach { crs ->
            DropdownMenuItem(
              text = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              onClick = { selectedCourse = crs; courseExpanded = false }
            )
          }
        }
      }

      Button(
        onClick = {
          val st = selectedStudent ?: return@Button
          val crs = selectedCourse ?: return@Button
          viewModel.adminGrantCourseUnlock(st.userId, crs, !isUnlocked)
        },
        modifier = Modifier.fillMaxWidth().testTag("admin_toggle_single_course_unlock")
      ) {
        Text(if (isUnlocked) "Revoke Single-Course Unlock" else "Activate Single-Course Unlock")
      }
    }
  }
}
