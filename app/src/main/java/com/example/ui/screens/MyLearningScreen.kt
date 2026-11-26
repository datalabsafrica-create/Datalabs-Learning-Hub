package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementsCalculator
import com.example.data.model.CareerPortfolioHelper
import com.example.data.model.Course
import com.example.data.model.UiState
import com.example.data.seed.AcademyProgressionHelper
import com.example.ui.components.CourseCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LearningPathCard
import com.example.ui.components.LessonNotesHelper
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot
import java.util.Locale

@Composable
fun MyLearningScreen(
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onOpenExplore: () -> Unit,
  onOpenCourse: (String) -> Unit,
  onResumeLesson: (courseId: String, lessonId: String) -> Unit,
  onOpenLearningPath: (String) -> Unit,
  onToggleSaveCourse: (Course, Boolean) -> Unit
) {
  if (studentState is UiState.Loading || catalogState is UiState.Loading) {
    LoadingStateView("Loading your student dashboard...")
    return
  }

  val catalog = (catalogState as? UiState.Success)?.data ?: HubCatalogSnapshot()
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val coursesById = remember(catalog.courses) { catalog.courses.associateBy { it.id } }
  val savedIds = student.savedCourses.map { it.courseId }.toSet()

  val visibleEnrollments = remember(student.enrollments, coursesById) {
    student.enrollments.filter { coursesById.containsKey(it.courseId) }
  }
  val totalEnrolled = visibleEnrollments.size
  val completedCount = visibleEnrollments.count { it.status == "COMPLETED" }
  val inProgressCount = visibleEnrollments.count { it.status != "COMPLETED" }
  val certCount = student.certificates.size
  val streakDays = student.profile?.streakDays ?: 0L
  val learningHours = String.format(Locale.US, "%.1f", (student.profile?.totalLearningMinutes ?: 0L) / 60.0)
  val overallProgressPercent = if (visibleEnrollments.isEmpty()) 0L
  else (visibleEnrollments.sumOf { it.progressPercent } / visibleEnrollments.size).coerceIn(0L, 100L)

  val continueEnrollment = remember(visibleEnrollments) {
    visibleEnrollments
      .filter { it.status != "COMPLETED" }
      .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      .firstOrNull()
  }

  val recentLessons = remember(student.lessonProgress, coursesById) {
    student.lessonProgress
      .filter { coursesById.containsKey(it.courseId) }
      .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      .take(4)
  }

  val badges = remember(student) {
    AchievementsCalculator.computeBadges(
      profile = student.profile,
      enrollments = student.enrollments,
      lessonProgress = student.lessonProgress,
      quizAttempts = student.quizAttempts,
      certificates = student.certificates
    )
  }

  val savedCourseList = remember(student.savedCourses, coursesById) {
    student.savedCourses.mapNotNull { coursesById[it.courseId] }
  }
  val masterySummary = remember(catalog.courses, student.enrollments, student.certificates) {
    AcademyProgressionHelper.computeMastery(
      courses = catalog.courses,
      enrollments = student.enrollments,
      certificates = student.certificates
    )
  }
  val labsLoggedCount = remember(student.lessonProgress) {
    student.lessonProgress.count { LessonNotesHelper.hasSavedLabResponse(it.studentNotes) }
  }

  val recommended = remember(catalog.courses, student.enrollments, student.profile) {
    CareerPortfolioHelper.recommendCoursesInStageOrder(
      courses = catalog.courses,
      enrollments = student.enrollments,
      profile = student.profile,
      limit = 5
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("my_learning_screen"),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text("Student Dashboard", style = MaterialTheme.typography.headlineLarge)
        Text(
          text = "Real-time progress, learning goals, and saved courses.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Continue Learning Hero Resume Card
    if (continueEnrollment != null) {
      item {
        Surface(
          color = NavyDeep,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "PICK UP WHERE YOU STOPPED",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD)
            )
            Text(
              text = continueEnrollment.courseTitle,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White
            )
            Text(
              text = "Last lesson: ${continueEnrollment.lastLessonTitle ?: "Lesson 1"} • ${continueEnrollment.progressPercent}% complete",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1)
            )
            LinearProgressIndicator(
              progress = { (continueEnrollment.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = Color(0xFF60A5FA),
              trackColor = Color.White.copy(alpha = 0.2f)
            )
            Button(
              onClick = {
                val lid = continueEnrollment.lastLessonId
                if (!lid.isNullOrBlank()) onResumeLesson(continueEnrollment.courseId, lid)
                else onOpenCourse(continueEnrollment.courseId)
              },
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
              modifier = Modifier.fillMaxWidth().testTag("dashboard_resume_button")
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Resume Lesson")
            }
          }
        }
      }
    }

    // Real Metrics Grid (2 rows x 3 cols) + Overall Learning Progress
    item {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatMetricCard("Enrolled", "$totalEnrolled", ElectricBlue, Modifier.weight(1f))
          StatMetricCard("In Progress", "$inProgressCount", WarningGold, Modifier.weight(1f))
          StatMetricCard("Completed", "$completedCount", SuccessEmerald, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatMetricCard("Certificates", "$certCount", ElectricBlue, Modifier.weight(1f))
          StatMetricCard("Streak", "${streakDays}d", WarningGold, Modifier.weight(1f))
          StatMetricCard("Hours", learningHours, SuccessEmerald, Modifier.weight(1f))
        }
        Card(
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("AI Academy Mastery (${masterySummary.completedCoursesCount}/${masterySummary.totalCourses} stages)", style = MaterialTheme.typography.titleSmall)
              Text("${masterySummary.overallProgressPercent}%", style = MaterialTheme.typography.titleSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
              progress = { (masterySummary.overallProgressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
              color = SuccessEmerald
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Enrolled avg: $overallProgressPercent% • $labsLoggedCount practice labs logged",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              val nextNode = masterySummary.nextRecommendedNode
              if (nextNode != null) {
                TextButton(
                  onClick = { onOpenCourse(nextNode.course.id) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                  modifier = Modifier.testTag("dashboard_next_stage_button")
                ) {
                  Text("Stage ${nextNode.stageNumber}", fontWeight = FontWeight.Bold)
                }
              }
            }
            student.profile?.let { profile ->
              Text(
                text = "Goal: ${profile.learningGoal.ifBlank { "Set your personal learning goal in Profile." }} (${profile.weeklyTargetHours}h/week)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // Achievements & Milestones
    item {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = WarningGold)
        Text("Achievements (${badges.count { it.unlocked }}/${badges.size})", style = MaterialTheme.typography.titleLarge)
      }
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(badges, key = { it.id }) { badge ->
          Surface(
            modifier = Modifier.width(176.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (badge.unlocked) ElectricBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (badge.unlocked) ElectricBlue else MaterialTheme.colorScheme.outline)
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                  imageVector = if (badge.unlocked) Icons.Filled.Verified else Icons.Filled.EmojiEvents,
                  contentDescription = null,
                  tint = if (badge.unlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Text(badge.category, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
              }
              Text(badge.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
              Text(
                text = if (badge.unlocked) "Unlocked" else badge.progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.unlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Recent Lessons
    if (recentLessons.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Filled.History, contentDescription = null, tint = ElectricBlue)
          Text("Recent Lessons", style = MaterialTheme.typography.titleLarge)
        }
      }
      items(recentLessons, key = { "recent_${it.lessonId}" }) { progress ->
        val crs = coursesById[progress.courseId]
        val hasLabLogged = LessonNotesHelper.hasSavedLabResponse(progress.studentNotes)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onResumeLesson(progress.courseId, progress.lessonId) },
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(crs?.title ?: "Course Lesson", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = if (progress.completed) "Completed" else "In progress • ${progress.playbackSpeed}x",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (progress.completed) SuccessEmerald else ElectricBlue
                )
                if (hasLabLogged) {
                  Surface(
                    color = SuccessEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                      Icon(Icons.Filled.Science, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(12.dp))
                      Text("Lab Logged", style = MaterialTheme.typography.labelSmall, color = SuccessEmerald, fontWeight = FontWeight.SemiBold)
                    }
                  }
                }
                if (progress.bookmarked) {
                  Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Bookmarked lesson",
                    tint = WarningGold,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
            Text("Resume", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
          }
        }
      }
    }

    // Enrolled Courses
    item {
      Text(
        text = "My Courses ($totalEnrolled)",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }

    if (visibleEnrollments.isEmpty()) {
      item {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          EmptyStateCard(
            icon = Icons.Filled.School,
            title = "You have not enrolled in any AI Academy courses yet",
            description = "Explore AI Academy and start your first free course today.",
            actionLabel = "Explore Courses",
            onAction = onOpenExplore
          )
        }
      }
    } else {
      items(visibleEnrollments, key = { it.id }) { enrollment ->
        val course = coursesById[enrollment.courseId]
        if (course != null) {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            CourseCard(
              course = course,
              isSaved = savedIds.contains(course.id),
              hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
              progressPercent = enrollment.progressPercent,
              ratingSummary = catalog.ratingSummaries[course.id],
              onClick = { onOpenCourse(course.id) },
              onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) }
            )
          }
        }
      }
    }

    // Learning Paths Progress
    if (catalog.learningPaths.isNotEmpty()) {
      item {
        Text(
          text = "Learning Pathways",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(catalog.learningPaths, key = { it.id }) { path ->
            val pct = student.pathProgress.firstOrNull { it.pathId == path.id }?.progressPercent ?: 0L
            LearningPathCard(
              path = path,
              progressPercent = pct,
              onClick = { onOpenLearningPath(path.id) },
              modifier = Modifier.width(270.dp)
            )
          }
        }
      }
    }

    // Saved Courses
    item {
      Text(
        text = "Saved Courses (${savedCourseList.size})",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }
    if (savedCourseList.isEmpty()) {
      item {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          EmptyStateCard(
            icon = Icons.Filled.Bookmark,
            title = "No saved courses yet",
            description = "Tap the bookmark icon on any course to save it for later."
          )
        }
      }
    } else {
      items(savedCourseList, key = { "saved_${it.id}" }) { course ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          CourseCard(
            course = course,
            isSaved = true,
            hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
            ratingSummary = catalog.ratingSummaries[course.id],
            onClick = { onOpenCourse(course.id) },
            onToggleSave = { onToggleSaveCourse(course, true) }
          )
        }
      }
    }

    // Recommended Next
    if (recommended.isNotEmpty()) {
      item {
        Text(
          text = "Recommended Next",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }
      items(recommended, key = { "rec_${it.id}" }) { course ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          CourseCard(
            course = course,
            isSaved = savedIds.contains(course.id),
            hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
            ratingSummary = catalog.ratingSummaries[course.id],
            onClick = { onOpenCourse(course.id) },
            onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) }
          )
        }
      }
    }
  }
}

@Composable
private fun StatMetricCard(
  label: String,
  value: String,
  accent: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = accent,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
