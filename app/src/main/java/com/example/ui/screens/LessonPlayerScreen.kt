package com.example.ui.screens

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.AiHelpMode
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.model.CourseCertificationPolicy
import com.example.data.model.LessonContent
import com.example.data.model.UiState
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.CopyableCodeBlockCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.InteractiveKeyTakeawaysCard
import com.example.ui.components.LessonExerciseLabCard
import com.example.ui.components.LessonNotesHelper
import com.example.ui.components.LoadingStateView
import com.example.ui.components.PracticeKitPreviewCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
  courseId: String,
  lessonId: String,
  viewModel: MainViewModel,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit,
  onNavigateLesson: (String) -> Unit,
  onOpenSubscription: () -> Unit,
  onOpenCertificates: () -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView()
    return
  }
  val catalog = (catalogState as? UiState.Success)?.data ?: return
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val course = catalog.courses.firstOrNull { it.id == courseId } ?: return
  var reloadTrigger by remember(lessonId) { mutableIntStateOf(0) }
  val lessonsUiState by remember(courseId, reloadTrigger) {
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
  val currentLesson = lessons.firstOrNull { it.id == lessonId }
  val lessonIndex = lessons.indexOfFirst { it.id == lessonId }
  val existingProgress = student.lessonProgress.firstOrNull { it.lessonId == lessonId }

  val aiState by viewModel.aiAssistantState.collectAsStateWithLifecycle()
  var contentState by remember(lessonId) { mutableStateOf<Result<LessonContent>?>(null) }
  var bookmarked by remember(lessonId, existingProgress) { mutableStateOf(existingProgress?.bookmarked ?: false) }
  var studentNotes by remember(lessonId, existingProgress) { mutableStateOf(existingProgress?.studentNotes ?: "") }
  var playbackSpeed by remember(lessonId, existingProgress) { mutableDoubleStateOf(existingProgress?.playbackSpeed ?: 1.0) }
  var resumeSeconds by remember(lessonId, existingProgress) { mutableLongStateOf(existingProgress?.lastPositionSeconds ?: 0L) }
  var showCompletionDialog by remember { mutableStateOf(false) }
  var showAiSheet by remember { mutableStateOf(false) }

  LaunchedEffect(lessonId, reloadTrigger) {
    contentState = null
    viewModel.clearAiAssistant()
    contentState = viewModel.loadLessonContent(lessonId)
  }

  // Seamless debounced auto-save for notes, playback position & preferences
  LaunchedEffect(lessonId, studentNotes, bookmarked, playbackSpeed, resumeSeconds) {
    val initialNotes = existingProgress?.studentNotes ?: ""
    val initialPos = existingProgress?.lastPositionSeconds ?: 0L
    if (studentNotes == initialNotes && resumeSeconds == initialPos && existingProgress == null && studentNotes.isBlank()) {
      return@LaunchedEffect
    }
    delay(1500L)
    viewModel.saveLessonNotesAndState(
      courseId = courseId,
      lessonId = lessonId,
      bookmarked = bookmarked,
      lastPositionSeconds = resumeSeconds,
      playbackSpeed = playbackSpeed,
      studentNotes = studentNotes,
      silent = true
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(course.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
              text = if (lessonIndex >= 0) "Lesson ${lessonIndex + 1} of ${lessons.size}" else "Lesson",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              bookmarked = !bookmarked
              viewModel.saveLessonNotesAndState(
                courseId = courseId,
                lessonId = lessonId,
                bookmarked = bookmarked,
                lastPositionSeconds = resumeSeconds,
                playbackSpeed = playbackSpeed,
                studentNotes = studentNotes
              )
            },
            modifier = Modifier.testTag("bookmark_lesson_button")
          ) {
            Icon(
              imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
              contentDescription = "Bookmark lesson",
              tint = if (bookmarked) ElectricBlue else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      )
    },
    floatingActionButton = {
      if (currentLesson != null && contentState?.isSuccess == true) {
        ExtendedFloatingActionButton(
          onClick = { showAiSheet = true },
          containerColor = NavyDeep,
          contentColor = Color.White,
          icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Study Tutor") },
          text = { Text("AI Study Tutor") },
          modifier = Modifier.testTag("ai_study_assistant_button")
        )
      }
    }
  ) { innerPadding ->
    if (lessonsUiState is UiState.Error) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        ErrorStateCard(
          message = (lessonsUiState as UiState.Error).message,
          onRetry = {
            if (isAdmin) viewModel.adminResyncStarterCatalog()
            reloadTrigger++
          }
        )
      }
      return@Scaffold
    }

    if (currentLesson == null) {
      if (lessonsUiState is UiState.Loading || isSeedingCatalog) {
        Box(modifier = Modifier.padding(innerPadding)) {
          LoadingStateView("Loading lesson materials...")
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
        ) {
          EmptyStateCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Lesson Not Found",
            description = "This teaching module could not be loaded yet. Try syncing or refreshing the course modules.",
            actionLabel = if (isAdmin) "Sync Teaching Modules" else "Go Back",
            onAction = {
              if (isAdmin) {
                viewModel.adminResyncStarterCatalog()
                reloadTrigger++
              } else {
                onBack()
              }
            }
          )
        }
      }
      return@Scaffold
    }

    if (contentState == null) {
      Box(modifier = Modifier.padding(innerPadding)) {
        LoadingStateView("Loading lesson teaching notes...")
      }
      return@Scaffold
    }

    val result = contentState!!
    if (result.isFailure) {
      val err = result.exceptionOrNull()
      val firestoreErr = generateSequence(err) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      val hasCourseAccess = viewModel.hasCourseAccess(course, student)
      val isLockedError = firestoreErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
        !currentLesson.freePreview && !hasCourseAccess

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        if (isLockedError) {
          Icon(Icons.Filled.Lock, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("Premium Lesson Locked", style = MaterialTheme.typography.headlineMedium)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Backend entitlement verification requires an active Datalabs Pro subscription (or single-course unlock) to unlock full lessons and downloadable resources.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onOpenSubscription,
            modifier = Modifier.testTag("view_plans_from_locked_lesson_button")
          ) {
            Text("View Subscription Plans")
          }
        } else {
          ErrorStateCard(
            message = err?.let {
              unwrapFirestoreMessage(
                it,
                "Unable to load teaching content for this lesson. Please check your connection or resync."
              )
            } ?: "Unable to load teaching content for this lesson. Please check your connection or resync.",
            onRetry = { reloadTrigger++ }
          )
          if (isAdmin) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = {
                viewModel.adminResyncStarterCatalog()
                reloadTrigger++
              },
              modifier = Modifier.testTag("sync_catalog_from_lesson_button")
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Starter Catalog")
            }
          }
        }
      }
      return@Scaffold
    }

    val content = result.getOrThrow()
    val isCompleted = existingProgress?.completed == true

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
        .testTag("lesson_player_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Progress Header
      val completedCount = student.lessonProgress.count { it.courseId == courseId && it.completed }
      val pct = if (lessons.isEmpty()) 0f else (completedCount.toFloat() / lessons.size.toFloat()).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { pct },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = SuccessEmerald
      )

      if (currentLesson.moduleTitle.isNotBlank()) {
        Surface(
          color = ElectricBlue.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = currentLesson.moduleTitle,
            style = MaterialTheme.typography.labelMedium,
            color = ElectricBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
      Text(currentLesson.title, style = MaterialTheme.typography.headlineMedium)
      Text(
        text = "${currentLesson.format} • ${currentLesson.durationMinutes} min • Auto-saving progress",
        style = MaterialTheme.typography.labelMedium,
        color = ElectricBlue
      )

      // Video / Media Player when URL exists, or On-Device Voice Narration Controller
      val mediaUrl = content.videoUrl ?: content.audioUrl
      if (!mediaUrl.isNullOrBlank()) {
        NativeVideoAudioPlayer(
          mediaUrl = mediaUrl,
          initialPositionSec = resumeSeconds,
          playbackSpeed = playbackSpeed,
          onSpeedChange = { speed ->
            playbackSpeed = speed
            viewModel.saveLessonNotesAndState(
              courseId, lessonId, bookmarked, resumeSeconds, speed, studentNotes, silent = true
            )
          },
          onPositionUpdate = { sec ->
            resumeSeconds = sec
          }
        )
      } else {
        AudioNarrationStudyCard(
          lessonId = lessonId,
          lessonTitle = currentLesson.title,
          summary = currentLesson.summary,
          notesMarkdown = content.notesMarkdown,
          playbackSpeed = playbackSpeed,
          onSpeedChange = { spd ->
            playbackSpeed = spd
            viewModel.saveLessonNotesAndState(
              courseId, lessonId, bookmarked, resumeSeconds, spd, studentNotes, silent = true
            )
          }
        )
      }

      // Core Teaching & Formatted Lesson Notes Reader
      Card(
        modifier = Modifier.testTag("lesson_teaching_notes_card"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "CORE TEACHING & CONCEPTS",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            fontWeight = FontWeight.Bold
          )
          Text("Lesson Notes", style = MaterialTheme.typography.titleMedium)
          FormattedNotesBlock(markdown = content.notesMarkdown)
        }
      }

      if (!content.slidesText.isNullOrBlank()) {
        InteractiveSlideDeckCard(slidesText = content.slidesText, lessonId = lessonId)
      }

      if (!content.caseStudyText.isNullOrBlank()) {
        Card(
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.BusinessCenter, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
              Text("Real-World Case Study", style = MaterialTheme.typography.titleSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
            }
            Text(content.caseStudyText, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      InteractiveKeyTakeawaysCard(
        takeaways = content.keyTakeaways,
        lessonId = lessonId
      )

      if (!content.exercisePrompt.isNullOrBlank()) {
        LessonExerciseLabCard(
          lessonId = lessonId,
          exercisePrompt = content.exercisePrompt,
          savedNotes = studentNotes,
          onSaveLabResponse = { draft ->
            val updatedNotes = LessonNotesHelper.upsertLabResponse(studentNotes, draft)
            studentNotes = updatedNotes
            viewModel.saveLessonNotesAndState(
              courseId = courseId,
              lessonId = lessonId,
              bookmarked = bookmarked,
              lastPositionSeconds = resumeSeconds,
              playbackSpeed = playbackSpeed,
              studentNotes = updatedNotes
            )
          },
          onAskTutorForFeedback = { draftOrPrompt ->
            showAiSheet = true
            viewModel.askAiStudyAssistant(
              course = course,
              lesson = currentLesson,
              content = content,
              mode = AiHelpMode.EXERCISE_HINT,
              userPrompt = "Coach me on my Practice Lab attempt without giving away the answer:\n$draftOrPrompt"
            )
          }
        )
      }

      // Downloadable Resource Preview + Copy + Export
      if (!content.downloadTitle.isNullOrBlank()) {
        val kitContent = content.downloadContent
          ?: "Resource: ${content.downloadTitle}\nURL: ${content.downloadUrl ?: "Available inside Datalabs Learning Hub"}"
        PracticeKitPreviewCard(
          title = content.downloadTitle,
          content = kitContent,
          onDownloadShare = {
            viewModel.downloadAndShareMaterial(content.downloadTitle, kitContent)
          }
        )
      }

      // Personal Student Notes
      Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Your Personal Notes", style = MaterialTheme.typography.titleSmall)
          OutlinedTextField(
            value = studentNotes,
            onValueChange = { studentNotes = it },
            placeholder = { Text("Capture key takeaways, SQL queries, or business ideas...") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("student_notes_input")
          )
          OutlinedButton(
            onClick = {
              viewModel.saveLessonNotesAndState(
                courseId, lessonId, bookmarked, resumeSeconds, playbackSpeed, studentNotes
              )
            }
          ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Notes")
          }
        }
      }

      // Complete & Prev/Next Controls
      Button(
        onClick = {
          viewModel.completeLesson(
            course = course,
            lesson = currentLesson,
            allLessons = lessons,
            onCourseCompleted = { showCompletionDialog = true }
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("mark_lesson_complete_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isCompleted) SuccessEmerald else ElectricBlue,
          contentColor = Color.White
        )
      ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isCompleted) "Lesson Completed" else "Mark Lesson Complete")
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        val prev = if (lessonIndex > 0) lessons[lessonIndex - 1] else null
        val next = if (lessonIndex >= 0 && lessonIndex < lessons.lastIndex) lessons[lessonIndex + 1] else null
        OutlinedButton(
          onClick = { prev?.let { onNavigateLesson(it.id) } },
          enabled = prev != null
        ) {
          Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
          Text("Previous")
        }
        Button(
          onClick = { next?.let { onNavigateLesson(it.id) } },
          enabled = next != null
        ) {
          Text("Next Lesson")
          Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
        }
      }
      Spacer(modifier = Modifier.height(64.dp))
    }
  }

  if (showAiSheet && currentLesson != null) {
    AiAssistantSheet(
      course = course,
      lesson = currentLesson,
      lessonContent = contentState?.getOrNull(),
      aiState = aiState,
      onAskAi = { mode, prompt ->
        viewModel.askAiStudyAssistant(course, currentLesson, contentState?.getOrNull(), mode, prompt)
      },
      onClearAi = { viewModel.clearAiAssistant() },
      onDismiss = { showAiSheet = false }
    )
  }

  if (showCompletionDialog) {
    val completedIds = student.lessonProgress.filter { it.courseId == courseId && it.completed }.map { it.lessonId }.toSet()
    val submissionMap = if (primaryAssignment != null) mapOf(primaryAssignment.id to primarySubmission) else emptyMap()
    val readiness = CourseCertificationPolicy.evaluateReadiness(
      lessons = lessons,
      completedLessonIds = completedIds,
      quizzes = quizzes,
      quizAttempts = student.quizAttempts,
      assignments = assignments,
      assignmentSubmissions = submissionMap
    )
    AlertDialog(
      onDismissRequest = { showCompletionDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.WorkspacePremium,
          contentDescription = null,
          tint = SuccessEmerald,
          modifier = Modifier.size(40.dp)
        )
      },
      title = {
        Text(
          if (readiness.isEligible) "Course Ready for Certification!" else "All Lessons Completed!"
        )
      },
      text = {
        if (readiness.isEligible) {
          Text("Congratulations on finishing all lessons, quizzes, and practical deliverables in ${course.title}.")
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Great job finishing the lessons in ${course.title}! Complete these remaining items in Course Overview to claim your certificate:")
            readiness.blockingReasons.forEach { reason ->
              Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      },
      confirmButton = {
        if (readiness.isEligible) {
          Button(
            onClick = {
              showCompletionDialog = false
              viewModel.issueCertificateForCourse(course) {
                onOpenCertificates()
              }
            }
          ) {
            Text("Get Certificate")
          }
        } else {
          Button(
            onClick = {
              showCompletionDialog = false
              onBack()
            }
          ) {
            Text("Go to Assessments")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showCompletionDialog = false }) {
          Text("Stay Here")
        }
      }
    )
  }
}

@Composable
private fun AudioNarrationStudyCard(
  lessonId: String,
  lessonTitle: String,
  summary: String,
  notesMarkdown: String,
  playbackSpeed: Double,
  onSpeedChange: (Double) -> Unit
) {
  val context = LocalContext.current
  var ttsReady by remember(lessonId) { mutableStateOf(false) }
  var ttsError by remember(lessonId) { mutableStateOf(false) }
  var isSpeaking by remember(lessonId) { mutableStateOf(false) }

  val spokenScript = remember(lessonTitle, summary, notesMarkdown) {
    val cleanNotes = notesMarkdown
      .replace(Regex("```[\\s\\S]*?```"), ". See the code template in lesson notes. ")
      .replace(Regex("\\|[-:]+"), " ")
      .replace("|", ", ")
      .replace("#", "")
      .replace("**", "")
      .replace("`", "")
      .replace(Regex("\\n+"), ". ")
      .replace(Regex("\\s+"), " ")
      .trim()
    "$lessonTitle. $summary. $cleanNotes"
  }

  val ttsEngine = remember(lessonId) {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        engine?.language = Locale.US
        ttsReady = true
      } else {
        ttsError = true
      }
    }
    engine
  }

  DisposableEffect(lessonId) {
    ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        isSpeaking = true
      }
      override fun onDone(utteranceId: String?) {
        if (utteranceId == "${lessonId}_end") {
          isSpeaking = false
        }
      }
      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        isSpeaking = false
      }
    })
    onDispose {
      runCatching {
        ttsEngine.stop()
        ttsEngine.shutdown()
      }
    }
  }

  fun startSpeaking(speed: Double) {
    if (!ttsReady) return
    ttsEngine.stop()
    ttsEngine.setSpeechRate(speed.toFloat().coerceIn(0.5f, 2.0f))
    val maxChunk = TextToSpeech.getMaxSpeechInputLength().coerceIn(800, 3200)
    val chunks = spokenScript.chunked(maxChunk)
    chunks.forEachIndexed { idx, chunk ->
      val mode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      val uttId = if (idx == chunks.lastIndex) "${lessonId}_end" else "${lessonId}_$idx"
      ttsEngine.speak(chunk, mode, null, uttId)
    }
    isSpeaking = true
  }

  Surface(
    color = NavyDeep,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("audio_narration_study_card")
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = Color(0xFF93C5FD),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "AUDIO LESSON NARRATION",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF93C5FD),
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = if (isSpeaking) "Playing • ${playbackSpeed}x" else "Speed: ${playbackSpeed}x",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White
        )
      }
      Text(
        text = summary,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = {
            if (isSpeaking) {
              ttsEngine.stop()
              isSpeaking = false
            } else {
              startSpeaking(playbackSpeed)
            }
          },
          enabled = ttsReady && !ttsError,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isSpeaking) SuccessEmerald else ElectricBlue,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("tts_listen_button")
        ) {
          Icon(
            imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isSpeaking) "Stop audio narration" else "Listen to teaching",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            when {
              ttsError -> "Audio Unavailable"
              !ttsReady -> "Preparing Audio..."
              isSpeaking -> "Stop Narration"
              else -> "Listen to Teaching"
            }
          )
        }
      }
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(0.8, 1.0, 1.25, 1.5, 2.0).forEach { spd ->
          FilterChip(
            selected = playbackSpeed == spd,
            onClick = {
              onSpeedChange(spd)
              if (isSpeaking) {
                startSpeaking(spd)
              }
            },
            label = { Text("${spd}x") },
            colors = FilterChipDefaults.filterChipColors(
              labelColor = Color(0xFFCBD5E1),
              selectedLabelColor = NavyDeep,
              selectedContainerColor = Color(0xFF93C5FD)
            )
          )
        }
      }
    }
  }
}

@Composable
private fun NativeVideoAudioPlayer(
  mediaUrl: String,
  initialPositionSec: Long,
  playbackSpeed: Double,
  onSpeedChange: (Double) -> Unit,
  onPositionUpdate: (Long) -> Unit
) {
  var isPlaying by remember { mutableStateOf(false) }
  var isPrepared by remember { mutableStateOf(false) }
  var durationMs by remember { mutableLongStateOf(0L) }
  var positionMs by remember { mutableLongStateOf(initialPositionSec * 1000L) }
  var mediaError by remember { mutableStateOf<String?>(null) }

  val mediaPlayer = remember(mediaUrl) { MediaPlayer() }

  DisposableEffect(mediaUrl) {
    onDispose {
      try {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.release()
      } catch (_: Exception) {
      }
    }
  }

  LaunchedEffect(isPlaying, isPrepared) {
    while (isPlaying && isPrepared) {
      try {
        val pos = mediaPlayer.currentPosition.toLong()
        positionMs = pos
        onPositionUpdate(pos / 1000L)
      } catch (_: Exception) {
      }
      delay(1000L)
    }
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = NavyDeep),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        AndroidView(
          factory = { ctx ->
            SurfaceView(ctx).apply {
              holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                  try {
                    mediaPlayer.reset()
                    mediaPlayer.setDisplay(holder)
                    mediaPlayer.setDataSource(mediaUrl)
                    mediaPlayer.setOnPreparedListener { mp ->
                      isPrepared = true
                      durationMs = mp.duration.toLong().coerceAtLeast(1L)
                      if (initialPositionSec > 0L) {
                        mp.seekTo((initialPositionSec * 1000L).toInt())
                      }
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mp.playbackParams = PlaybackParams().setSpeed(playbackSpeed.toFloat())
                      }
                    }
                    mediaPlayer.setOnErrorListener { _, _, _ ->
                      mediaError = "Unable to stream media URL."
                      true
                    }
                    mediaPlayer.prepareAsync()
                  } catch (e: Exception) {
                    mediaError = e.localizedMessage ?: "Media error"
                  }
                }
                override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {}
              })
            }
          },
          modifier = Modifier.fillMaxSize()
        )
        if (!isPrepared && mediaError == null) {
          CircularProgressIndicator(color = Color.White)
        }
        if (mediaError != null) {
          Text(mediaError!!, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
      }

      if (durationMs > 0L) {
        Slider(
          value = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
          onValueChange = { frac ->
            val target = (frac * durationMs).toLong()
            positionMs = target
            if (isPrepared) mediaPlayer.seekTo(target.toInt())
          }
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            if (!isPrepared) return@IconButton
            if (isPlaying) {
              mediaPlayer.pause()
              isPlaying = false
            } else {
              mediaPlayer.start()
              isPlaying = true
            }
          }
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(1.0, 1.25, 1.5, 2.0).forEach { spd ->
            FilterChip(
              selected = playbackSpeed == spd,
              onClick = {
                onSpeedChange(spd)
                if (isPrepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  try {
                    mediaPlayer.playbackParams = PlaybackParams().setSpeed(spd.toFloat())
                  } catch (_: Exception) {
                  }
                }
              },
              label = { Text("${spd}x") }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InteractiveSlideDeckCard(
  slidesText: String,
  lessonId: String
) {
  val slides = remember(slidesText) {
    slidesText.lines().map { it.trim() }.filter { it.isNotBlank() }
  }
  if (slides.isEmpty()) return

  var currentSlideIndex by remember(lessonId) { mutableIntStateOf(0) }
  val safeIndex = currentSlideIndex.coerceIn(0, slides.lastIndex)

  Card(
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Interactive Slide Deck", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Surface(
          color = ElectricBlue.copy(alpha = 0.14f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "Slide ${safeIndex + 1} of ${slides.size}",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Surface(
        color = NavyDeep,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = slides[safeIndex],
          style = MaterialTheme.typography.bodyLarge,
          color = Color.White,
          modifier = Modifier.padding(18.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        OutlinedButton(
          onClick = { if (safeIndex > 0) currentSlideIndex = safeIndex - 1 },
          enabled = safeIndex > 0
        ) {
          Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
          Text("Prev Slide")
        }
        Button(
          onClick = { if (safeIndex < slides.lastIndex) currentSlideIndex = safeIndex + 1 },
          enabled = safeIndex < slides.lastIndex
        ) {
          Text("Next Slide")
          Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
        }
      }
    }
  }
}

@Composable
private fun FormattedNotesBlock(markdown: String) {
  val lines = remember(markdown) { markdown.lines() }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    var i = 0
    while (i < lines.size) {
      val trimmed = lines[i].trim()
      when {
        trimmed.startsWith("```") -> {
          val languageTag = trimmed.removePrefix("```").trim()
          val codeLines = mutableListOf<String>()
          i++
          while (i < lines.size && !lines[i].trim().startsWith("```")) {
            codeLines.add(lines[i])
            i++
          }
          CopyableCodeBlockCard(
            languageTag = languageTag,
            code = codeLines.joinToString("\n")
          )
        }
        trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2 -> {
          val tableRows = mutableListOf<List<String>>()
          while (i < lines.size) {
            val rowLine = lines[i].trim()
            if (!rowLine.startsWith("|")) break
            val isDivider = rowLine.replace("|", "").replace("-", "").replace(":", "").isBlank()
            if (!isDivider) {
              val cells = rowLine.trim('|').split('|').map { it.trim() }
              if (cells.isNotEmpty()) tableRows.add(cells)
            }
            i++
          }
          i--
          if (tableRows.isNotEmpty()) {
            MarkdownTableCard(rows = tableRows)
          }
        }
        trimmed.startsWith("# ") -> {
          Text(
            text = trimmed.removePrefix("# ").trim(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        trimmed.startsWith("## ") -> {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = trimmed.removePrefix("## ").trim(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ElectricBlue
          )
        }
        trimmed.startsWith("### ") -> {
          Text(
            text = trimmed.removePrefix("### ").trim(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        trimmed.isNotEmpty() -> {
          val displayLine = if (trimmed.startsWith("- ")) "• ${trimmed.removePrefix("- ")}" else trimmed
          Text(
            text = parseInlineFormatting(displayLine),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      i++
    }
  }
}

@Composable
private fun MarkdownTableCard(rows: List<List<String>>) {
  val colCount = rows.maxOfOrNull { it.size } ?: return
  Surface(
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
      rows.forEachIndexed { rowIndex, cells ->
        val isHeader = rowIndex == 0
        Row(
          modifier = Modifier
            .background(if (isHeader) NavyDeep else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          for (colIdx in 0 until colCount) {
            val value = cells.getOrElse(colIdx) { "" }
            Text(
              text = value,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
              color = if (isHeader) Color.White else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.width(if (colCount <= 2) 155.dp else 130.dp)
            )
          }
        }
        if (rowIndex < rows.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
      }
    }
  }
}

private fun parseInlineFormatting(text: String) = buildAnnotatedString {
  var idx = 0
  while (idx < text.length) {
    when {
      text.startsWith("**", idx) -> {
        val end = text.indexOf("**", idx + 2)
        if (end != -1) {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(idx + 2, end))
          }
          idx = end + 2
        } else {
          append(text[idx])
          idx++
        }
      }
      text[idx] == '`' -> {
        val end = text.indexOf('`', idx + 1)
        if (end != -1) {
          withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = ElectricBlue)) {
            append(text.substring(idx + 1, end))
          }
          idx = end + 1
        } else {
          append(text[idx])
          idx++
        }
      }
      else -> {
        append(text[idx])
        idx++
      }
    }
  }
}

