package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CareerPortfolioHelper
import com.example.data.model.UiState
import com.example.data.seed.AcademyProgressionHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.TierBadge
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
  pathId: String,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit,
  onOpenCourse: (String) -> Unit,
  onResumeLesson: (courseId: String, lessonId: String) -> Unit,
  onOpenCertificates: () -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView()
    return
  }
  val catalog = (catalogState as? UiState.Success)?.data ?: return
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val path = catalog.learningPaths.firstOrNull { it.id == pathId }
  if (path == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Learning Path") },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        )
      }
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        EmptyStateCard(
          icon = Icons.Filled.School,
          title = "Learning Path Not Available",
          description = "This pathway is not currently active in AI Academy.",
          actionLabel = "Go Back",
          onAction = onBack
        )
      }
    }
    return
  }

  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val pathCourses = remember(path.courseIds, catalog.courses) {
    path.courseIds.mapNotNull { cid -> catalog.courses.firstOrNull { it.id == cid } }
  }
  val mastery = remember(pathCourses, student.enrollments, student.certificates) {
    AcademyProgressionHelper.computeMastery(
      courses = pathCourses,
      enrollments = student.enrollments,
      certificates = student.certificates
    )
  }
  val completedInPath = mastery.completedCoursesCount
  val progressPct = mastery.overallProgressPercent
  val nextNode = mastery.nextRecommendedNode

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Learning Path") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("learning_path_screen"),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Surface(color = NavyDeep, modifier = Modifier.fillMaxWidth()) {
          Column {
            AsyncImage(
              model = R.drawable.img_learning_path_banner,
              contentDescription = path.title,
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
              contentScale = ContentScale.Crop
            )
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "CAREER PATHWAY • ${path.targetRole.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlueSoft
              )
              Text(path.title, style = MaterialTheme.typography.displayMedium, color = Color.White)
              Text(path.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
              LinearProgressIndicator(
                progress = { (progressPct.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = if (completedInPath == pathCourses.size && pathCourses.isNotEmpty()) SuccessEmerald else Color(0xFF60A5FA),
                trackColor = Color.White.copy(alpha = 0.2f)
              )
              Text(
                text = "$completedInPath of ${pathCourses.size} stages completed • $progressPct% overall • ~${path.estimatedHours}h",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFBFDBFE)
              )

              if (nextNode != null) {
                val resumeLessonId = nextNode.enrollment?.lastLessonId
                Button(
                  onClick = {
                    if (!resumeLessonId.isNullOrBlank()) {
                      onResumeLesson(nextNode.course.id, resumeLessonId)
                    } else {
                      onOpenCourse(nextNode.course.id)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("learning_path_next_button")
                ) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  val actionPrefix = if (!resumeLessonId.isNullOrBlank()) "Resume" else "Start"
                  Text("$actionPrefix Stage ${nextNode.stageNumber}: ${nextNode.course.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              } else if (pathCourses.isNotEmpty()) {
                OutlinedButton(
                  onClick = onOpenCertificates,
                  border = BorderStroke(1.dp, SuccessEmerald),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("learning_path_view_certs_button")
                ) {
                  Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = WarningGold, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Pathway Complete • View Credentials")
                }
              }
            }
          }
        }
      }

      itemsIndexed(mastery.nodes, key = { _, node -> node.course.id }) { index, node ->
        val course = node.course
        val done = node.isCompleted
        val deliverable = CareerPortfolioHelper.deliverableForCourse(course.id)
        val hasAccess = !course.premiumOnly || hasSubscription || unlockedCourseIds.contains(course.id)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenCourse(course.id) }
            .testTag("learning_path_stage_${node.stageNumber}"),
          border = BorderStroke(
            width = 1.dp,
            color = when {
              done -> SuccessEmerald.copy(alpha = 0.6f)
              node.isNextRecommended -> ElectricBlue.copy(alpha = 0.7f)
              else -> MaterialTheme.colorScheme.outline
            }
          ),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = if (done) SuccessEmerald else if (node.isNextRecommended) ElectricBlue else ElectricBlue.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (done) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(20.dp))
                  } else {
                    Text(
                      text = "${node.stageNumber}",
                      fontWeight = FontWeight.Bold,
                      color = if (node.isNextRecommended) Color.White else ElectricBlue
                    )
                  }
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = node.trackLabel.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (done) SuccessEmerald else ElectricBlue,
                  fontWeight = FontWeight.Bold
                )
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                  text = "${course.level} • ${course.lessonCount} lessons • ${course.durationMinutes}m",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              TierBadge(
                isPremium = course.premiumOnly,
                hasSubscription = hasAccess
              )
            }

            if (deliverable != null) {
              Text(
                text = "Deliverable: ${deliverable.deliverableTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            if (node.progressPercent in 1..99) {
              LinearProgressIndicator(
                progress = { node.progressPercent / 100f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = ElectricBlue
              )
              Text(
                text = "${node.progressPercent}% complete",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue,
                fontWeight = FontWeight.SemiBold
              )
            } else if (!done && !node.prerequisiteMet && node.prerequisiteTitle != null) {
              Text(
                text = "Recommended prerequisite: ${node.prerequisiteTitle}",
                style = MaterialTheme.typography.labelSmall,
                color = WarningGold
              )
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          border = BorderStroke(
            1.dp,
            if (mastery.specializationCertificate != null) SuccessEmerald.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (mastery.specializationCertificate != null) SuccessEmerald.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
          )
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Filled.WorkspacePremium,
              contentDescription = null,
              tint = if (mastery.specializationCertificate != null) SuccessEmerald else WarningGold,
              modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = if (mastery.specializationCertificate != null) "Specialization Earned" else "AI Academy Specialization Goal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = when {
                  mastery.specializationCertificate != null -> "Verified code: ${mastery.specializationCertificate?.certificateCode}"
                  mastery.allRequiredCoursesCompleted -> "All 5 stages complete! Claim your Specialization Certificate in AI Academy."
                  else -> "Complete all ${pathCourses.size} stages and pass their capstone deliverables to unlock the Specialization Credential."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
