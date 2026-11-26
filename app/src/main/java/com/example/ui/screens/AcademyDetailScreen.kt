package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Course
import com.example.data.model.CourseRatingSummary
import com.example.data.model.UiState
import com.example.data.seed.AcademyMasterySummary
import com.example.data.seed.AcademyProgressionHelper
import com.example.data.seed.CourseProgressionNode
import com.example.ui.components.CourseCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LearningPathCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.academyIconFor
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.theme.parseAccentColor
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyDetailScreen(
  academyId: String,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit,
  onOpenCourse: (String) -> Unit,
  onResumeLesson: (courseId: String, lessonId: String) -> Unit,
  onOpenLearningPath: (String) -> Unit,
  onToggleSaveCourse: (Course, Boolean) -> Unit,
  onOpenCertificates: () -> Unit,
  onClaimSpecialization: () -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView()
    return
  }
  val catalog = (catalogState as? UiState.Success)?.data ?: return
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val academy = catalog.academies.firstOrNull { it.id == academyId }
  if (academy == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Academy") },
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
          title = "Academy Not Available",
          description = "AI Academy is the sole active academy in this learning phase.",
          actionLabel = "Go Back",
          onAction = onBack
        )
      }
    }
    return
  }

  val accent = parseAccentColor(academy.accentHex, ElectricBlue)
  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val savedIds = student.savedCourses.map { it.courseId }.toSet()

  val academyCourses = remember(catalog.courses, academyId) {
    catalog.courses.filter { it.academyId == academyId && it.published }
  }
  val academyPaths = remember(catalog.learningPaths, academyId) {
    catalog.learningPaths.filter { it.academyId == academyId && it.published }
  }

  val mastery = remember(academyCourses, student.enrollments, student.certificates) {
    AcademyProgressionHelper.computeMastery(
      courses = academyCourses,
      enrollments = student.enrollments,
      certificates = student.certificates
    )
  }

  var levelFilter by remember { mutableStateOf<String?>(null) }
  var tierFilter by remember { mutableStateOf<String?>(null) }

  val visibleNodes = remember(mastery.nodes, levelFilter, tierFilter) {
    mastery.nodes.filter { node ->
      (levelFilter == null || node.course.level == levelFilter) &&
        (tierFilter == null || node.course.accessTier == tierFilter)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(academy.shortName) },
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
        .testTag("academy_detail_screen"),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Flagship Hero Header + Mastery Bar + Next Recommended Action
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = NavyDeep
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .drawBehind {
                if (size.minDimension > 0f) {
                  drawCircle(
                    brush = Brush.radialGradient(
                      colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                      center = Offset(size.width * 0.9f, size.height * 0.15f),
                      radius = (size.maxDimension * 0.6f).coerceAtLeast(1f)
                    )
                  )
                }
              }
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(accent),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = academyIconFor(academy.iconKey),
                  contentDescription = academy.name,
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "FLAGSHIP PROGRAM",
                  style = MaterialTheme.typography.labelSmall,
                  color = ElectricBlueSoft,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = academy.name,
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White
                )
                Text(
                  text = "${academyCourses.size} Stages • ${academyCourses.count { !it.premiumOnly }} Free • ${academyCourses.count { it.premiumOnly }} Premium",
                  style = MaterialTheme.typography.labelMedium,
                  color = ElectricBlueSoft
                )
              }
            }

            Text(
              text = academy.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )

            // Mastery Progress & Next Action Panel
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavySurface),
              border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f)),
              modifier = Modifier.testTag("academy_mastery_card")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Academy Mastery",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "${mastery.completedCoursesCount} of ${mastery.totalCourses} Completed • ${mastery.overallProgressPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (mastery.allRequiredCoursesCompleted) SuccessEmerald else ElectricBlueSoft,
                    fontWeight = FontWeight.Bold
                  )
                }

                LinearProgressIndicator(
                  progress = { mastery.overallProgressPercent / 100f },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  color = if (mastery.allRequiredCoursesCompleted) SuccessEmerald else ElectricBlue,
                  trackColor = Color.White.copy(alpha = 0.14f)
                )

                val nextNode = mastery.nextRecommendedNode
                if (nextNode != null) {
                  val lastLesson = nextNode.enrollment?.lastLessonId
                  val isActiveResume = nextNode.enrollment != null && !lastLesson.isNullOrBlank()
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = if (isActiveResume) {
                        "RESUME ${nextNode.trackLabel.uppercase()}"
                      } else {
                        "RECOMMENDED NEXT • ${nextNode.trackLabel.uppercase()}"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = ElectricBlueSoft,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = nextNode.course.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Button(
                    onClick = {
                      if (isActiveResume && lastLesson != null) {
                        onResumeLesson(nextNode.course.id, lastLesson)
                      } else {
                        onOpenCourse(nextNode.course.id)
                      }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = ElectricBlue,
                      contentColor = Color.White
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("academy_next_action_button")
                  ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = when {
                        isActiveResume -> "Resume Stage ${nextNode.stageNumber} (${nextNode.progressPercent}%)"
                        !nextNode.course.premiumOnly -> "Start Stage ${nextNode.stageNumber} (Free)"
                        else -> "Continue to Stage ${nextNode.stageNumber}"
                      }
                    )
                  }
                } else if (mastery.allRequiredCoursesCompleted) {
                  Text(
                    text = "All 5 AI Academy stages completed! Your Specialization credential is unlocked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                  )
                  Button(
                    onClick = {
                      if (mastery.specializationCertificate != null) onOpenCertificates()
                      else onClaimSpecialization()
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = SuccessEmerald,
                      contentColor = Color.White
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("academy_hero_claim_spec_button")
                  ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      if (mastery.specializationCertificate != null) "View Specialization Certificate"
                      else "Claim Specialization Certificate"
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. AI Academy Specialization Certificate Card
      item {
        SpecializationMilestoneCard(
          mastery = mastery,
          onClaimSpecialization = onClaimSpecialization,
          onOpenCertificates = onOpenCertificates,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }

      // 3. Academy Learning Paths (if present)
      if (academyPaths.isNotEmpty()) {
        item {
          Text(
            text = "Curated Career Pathways",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(academyPaths, key = { it.id }) { path ->
              val progress = student.pathProgress.firstOrNull { it.pathId == path.id }?.progressPercent ?: 0L
              LearningPathCard(
                path = path,
                progressPercent = progress,
                onClick = { onOpenLearningPath(path.id) },
                modifier = Modifier.width(276.dp)
              )
            }
          }
        }
      }

      // 4. Roadmap Section Header + Filter Chips
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
              text = "5-Stage Skill Progression Roadmap",
              style = MaterialTheme.typography.titleLarge
            )
            Text(
              text = "Follow the structured order from AI foundations to shipping AI apps, or filter by tier and level.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = tierFilter == null && levelFilter == null,
              onClick = {
                tierFilter = null
                levelFilter = null
              },
              label = { Text("All Stages (${mastery.totalCourses})") }
            )
            FilterChip(
              selected = tierFilter == "FREE",
              onClick = { tierFilter = if (tierFilter == "FREE") null else "FREE" },
              label = { Text("Free") }
            )
            FilterChip(
              selected = tierFilter == "PREMIUM",
              onClick = { tierFilter = if (tierFilter == "PREMIUM") null else "PREMIUM" },
              label = { Text("Premium") }
            )
            listOf("BEGINNER" to "Beginner", "INTERMEDIATE" to "Intermediate", "ADVANCED" to "Advanced")
              .forEach { (key, label) ->
                FilterChip(
                  selected = levelFilter == key,
                  onClick = { levelFilter = if (levelFilter == key) null else key },
                  label = { Text(label) }
                )
              }
          }
        }
      }

      // 5. Guided Progression Nodes
      if (visibleNodes.isEmpty()) {
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            EmptyStateCard(
              icon = Icons.Filled.School,
              title = "No courses for selected filter",
              description = "Clear filters to view the full ${academy.name} 5-stage roadmap."
            )
          }
        }
      } else {
        itemsIndexed(visibleNodes, key = { _, node -> node.course.id }) { index, node ->
          ProgressionCourseStageRow(
            node = node,
            isLast = index == visibleNodes.lastIndex,
            accentColor = accent,
            isSaved = savedIds.contains(node.course.id),
            hasAccess = hasSubscription || unlockedCourseIds.contains(node.course.id),
            ratingSummary = catalog.ratingSummaries[node.course.id],
            onOpenCourse = { onOpenCourse(node.course.id) },
            onToggleSave = { onToggleSaveCourse(node.course, savedIds.contains(node.course.id)) },
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SpecializationMilestoneCard(
  mastery: AcademyMasterySummary,
  onClaimSpecialization: () -> Unit,
  onOpenCertificates: () -> Unit,
  modifier: Modifier = Modifier
) {
  val earnedCert = mastery.specializationCertificate
  val readyToClaim = mastery.allRequiredCoursesCompleted && earnedCert == null
  val borderColor = when {
    earnedCert != null -> SuccessEmerald
    readyToClaim -> WarningGold
    else -> MaterialTheme.colorScheme.outline
  }
  val containerColor = when {
    earnedCert != null -> SuccessEmerald.copy(alpha = 0.08f)
    readyToClaim -> WarningGold.copy(alpha = 0.09f)
    else -> MaterialTheme.colorScheme.surface
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("academy_specialization_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              when {
                earnedCert != null -> SuccessEmerald.copy(alpha = 0.16f)
                readyToClaim -> WarningGold.copy(alpha = 0.18f)
                else -> ElectricBlue.copy(alpha = 0.12f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (earnedCert != null) Icons.Filled.Verified else Icons.Filled.WorkspacePremium,
            contentDescription = "AI Academy Specialization",
            tint = when {
              earnedCert != null -> SuccessEmerald
              readyToClaim -> WarningGold
              else -> ElectricBlue
            }
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "AI Academy Specialization Certificate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = when {
              earnedCert != null -> "Verified Credential Earned • ${earnedCert.certificateCode}"
              readyToClaim -> "All 5 courses completed — your credential is ready to claim!"
              else -> "${mastery.completedCoursesCount} of ${mastery.totalCourses} courses finished toward specialization"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (earnedCert != null) {
        OutlinedButton(
          onClick = onOpenCertificates,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("view_specialization_cert_button")
        ) {
          Text("View & Export Specialization PDF")
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      } else if (readyToClaim) {
        Button(
          onClick = onClaimSpecialization,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("claim_specialization_cert_button")
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Claim Specialization Certificate")
        }
      } else {
        LinearProgressIndicator(
          progress = {
            if (mastery.totalCourses == 0) 0f
            else mastery.completedCoursesCount.toFloat() / mastery.totalCourses.toFloat()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = ElectricBlue
        )
      }
    }
  }
}

@Composable
private fun ProgressionCourseStageRow(
  node: CourseProgressionNode,
  isLast: Boolean,
  accentColor: Color,
  isSaved: Boolean,
  hasAccess: Boolean,
  ratingSummary: CourseRatingSummary?,
  onOpenCourse: () -> Unit,
  onToggleSave: () -> Unit,
  modifier: Modifier = Modifier
) {
  val stageColor = when {
    node.isCompleted -> SuccessEmerald
    node.isNextRecommended -> ElectricBlue
    node.enrollment != null -> ElectricBlue
    else -> MaterialTheme.colorScheme.outline
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("academy_stage_${node.stageNumber}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Stage Header + Prerequisite Readiness Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(
            if (node.isCompleted || node.isNextRecommended || node.enrollment != null) stageColor
            else MaterialTheme.colorScheme.surfaceVariant
          ),
        contentAlignment = Alignment.Center
      ) {
        if (node.isCompleted) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Stage ${node.stageNumber} completed",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        } else {
          Text(
            text = "${node.stageNumber}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (node.isNextRecommended || node.enrollment != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = node.trackLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (node.isCompleted) SuccessEmerald else accentColor,
            fontWeight = FontWeight.Bold
          )
          val badgeLabel = when {
            node.isCompleted -> "Completed"
            node.enrollment != null -> "In Progress • ${node.progressPercent}%"
            node.isNextRecommended -> "Recommended Next"
            node.prerequisiteMet -> "Ready"
            else -> "After Stage ${node.stageNumber - 1}"
          }
          val badgeBg = when {
            node.isCompleted -> SuccessEmerald.copy(alpha = 0.14f)
            node.isNextRecommended || node.enrollment != null -> ElectricBlue.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surfaceVariant
          }
          val badgeFg = when {
            node.isCompleted -> SuccessEmerald
            node.isNextRecommended || node.enrollment != null -> ElectricBlue
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }
          Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
            Text(
              text = badgeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = badgeFg,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = if (!node.prerequisiteMet && node.prerequisiteTitle != null && !node.isCompleted) {
            "Suggested prerequisite: ${node.prerequisiteTitle}"
          } else {
            node.outcomeTag
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    CourseCard(
      course = node.course,
      accentColor = accentColor,
      isSaved = isSaved,
      hasSubscription = hasAccess,
      progressPercent = node.enrollment?.progressPercent,
      ratingSummary = ratingSummary,
      onClick = onOpenCourse,
      onToggleSave = onToggleSave
    )

    if (!isLast) {
      Box(
        modifier = Modifier
          .padding(start = 14.dp)
          .width(2.dp)
          .height(10.dp)
          .background(stageColor.copy(alpha = 0.45f))
      )
    }
  }
}
