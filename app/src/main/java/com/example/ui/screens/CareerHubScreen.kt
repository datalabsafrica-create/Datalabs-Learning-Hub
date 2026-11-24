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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CareerPortfolioHelper
import com.example.data.model.Course
import com.example.data.model.PortfolioDeliverableItem
import com.example.data.model.UiState
import com.example.ui.components.CourseCard
import com.example.ui.components.LearningPathCard
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.theme.parseAccentColor
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot
import kotlinx.coroutines.delay

private data class CareerTrackPillar(
  val id: String,
  val title: String,
  val subtitle: String,
  val checklist: List<String>,
  val icon: ImageVector,
  val keyword: String,
  val linkedStageCourseId: String
)

private val careerPillars = listOf(
  CareerTrackPillar(
    id = "cv_resume",
    title = "CV & Resume Preparation",
    subtitle = "Impact bullet points, ATS formatting & quantifiable achievements",
    checklist = listOf(
      "Use Action + Metric + Business Result on every role bullet",
      "Tailor top 3 summary lines to each job description",
      "Export clean 1–2 page PDF without decorative clutter"
    ),
    icon = Icons.Filled.Description,
    keyword = "productivity",
    linkedStageCourseId = "ai_ai_for_productivity"
  ),
  CareerTrackPillar(
    id = "linkedin",
    title = "LinkedIn & Personal Branding",
    subtitle = "Recruiter searchability, headline formulas & featured projects",
    checklist = listOf(
      "Write a Role + Skill + Outcome headline",
      "Pin 2 portfolio case studies in Featured section",
      "Engage with industry posts twice weekly"
    ),
    icon = Icons.Filled.Badge,
    keyword = "prompt",
    linkedStageCourseId = "ai_prompt_engineering"
  ),
  CareerTrackPillar(
    id = "interview",
    title = "Interview Preparation (STAR)",
    subtitle = "Structured Situation-Task-Action-Result storytelling & technical walkthroughs",
    checklist = listOf(
      "Prepare 5 STAR stories covering teamwork, conflict & delivery",
      "Practice live AI architecture & evaluation walkthroughs aloud",
      "Research market salary bands before offer discussion"
    ),
    icon = Icons.Filled.RecordVoiceOver,
    keyword = "apps",
    linkedStageCourseId = "ai_building_apps_with_ai"
  ),
  CareerTrackPillar(
    id = "portfolio",
    title = "Portfolio & Proof of Work",
    subtitle = "Showcasing JSON schemas, eval scorecards, AI specs & case studies",
    checklist = listOf(
      "Include problem statement, dataset/prompt, and business outcome",
      "Link live deliverables from AI Academy course assignments",
      "Document clear READMEs & verification codes for hiring managers"
    ),
    icon = Icons.Filled.FolderShared,
    keyword = "prompt",
    linkedStageCourseId = "ai_prompt_engineering"
  ),
  CareerTrackPillar(
    id = "workplace",
    title = "Workplace & AI Governance Readiness",
    subtitle = "Responsible AI judgment, executive communication & workflow safety",
    checklist = listOf(
      "Lead written updates with conclusions first (BLUF)",
      "Verify AI-generated outputs before sharing with stakeholders",
      "Apply redaction rules to protect sensitive company data"
    ),
    icon = Icons.Filled.Groups,
    keyword = "beginners",
    linkedStageCourseId = "ai_artificial_intelligence_for_beginners"
  ),
  CareerTrackPillar(
    id = "freelancing",
    title = "Freelancing & Self-Employment",
    subtitle = "Winning global clients, scoping AI ROI proposals & milestone invoicing",
    checklist = listOf(
      "Lead proposals with the client's desired business ROI",
      "Collect 40–50% deposit before kickoff with clear revision limits",
      "Price by value delivered rather than race-to-the-bottom hourly rates"
    ),
    icon = Icons.Filled.Public,
    keyword = "business",
    linkedStageCourseId = "ai_ai_for_business_and_entrepreneurs"
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerHubScreen(
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit,
  onOpenCourse: (String) -> Unit,
  onOpenLearningPath: (String) -> Unit,
  onOpenCertificates: () -> Unit,
  onToggleSaveCourse: (Course, Boolean) -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView("Loading Career Readiness Hub...")
    return
  }

  val catalog = (catalogState as? UiState.Success)?.data ?: HubCatalogSnapshot()
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val savedIds = student.savedCourses.map { it.courseId }.toSet()
  val enrollmentByCourse = student.enrollments.associateBy { it.courseId }
  val accentMap = remember(catalog.academies) {
    catalog.academies.associate { it.id to parseAccentColor(it.accentHex, ElectricBlue) }
  }

  val readiness = remember(
    catalog.courses,
    student.enrollments,
    student.lessonProgress,
    student.quizAttempts,
    student.certificates
  ) {
    CareerPortfolioHelper.computeReadiness(
      courses = catalog.courses,
      enrollments = student.enrollments,
      lessonProgress = student.lessonProgress,
      quizAttempts = student.quizAttempts,
      certificates = student.certificates
    )
  }

  val portfolioPitch = remember(student.profile?.fullName, readiness) {
    CareerPortfolioHelper.buildPortfolioPitch(
      learnerName = student.profile?.fullName.orEmpty(),
      snapshot = readiness
    )
  }

  val clipboard = LocalClipboardManager.current
  var pitchCopied by remember { mutableStateOf(false) }
  LaunchedEffect(pitchCopied) {
    if (pitchCopied) {
      delay(1800L)
      pitchCopied = false
    }
  }

  var selectedPillarId by remember { mutableStateOf("ALL") }
  val checkedChecklistKeys = remember { mutableStateMapOf<String, Boolean>() }

  val careerCourses = remember(catalog.courses, selectedPillarId) {
    val published = catalog.courses.filter { it.published }
    if (selectedPillarId == "ALL") {
      published
    } else {
      val pillar = careerPillars.firstOrNull { it.id == selectedPillarId }
      if (pillar == null) published
      else published.sortedByDescending {
        it.id == pillar.linkedStageCourseId ||
          it.title.contains(pillar.keyword, ignoreCase = true) ||
          it.description.contains(pillar.keyword, ignoreCase = true)
      }
    }
  }

  val careerPaths = remember(catalog.learningPaths) {
    catalog.learningPaths.filter { it.published }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Career & Portfolio Hub") },
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
        .testTag("career_hub_screen"),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Live Career Readiness & Proof-of-Work Scorecard
      item {
        Surface(
          color = NavyDeep,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("career_readiness_scorecard")
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Filled.BusinessCenter, contentDescription = null, tint = ElectricBlueSoft)
                Text(
                  text = "LIVE PROOF-OF-WORK READINESS",
                  style = MaterialTheme.typography.labelSmall,
                  color = ElectricBlueSoft
                )
              }
              Surface(
                color = if (readiness.specializationEarned) SuccessEmerald.copy(alpha = 0.25f) else ElectricBlue.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
              ) {
                Text(
                  text = readiness.readinessTier,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Text(
                text = "Turn Learned Skills into Real Opportunities",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${readiness.readinessScorePercent}%",
                style = MaterialTheme.typography.headlineMedium,
                color = if (readiness.readinessScorePercent >= 80) SuccessEmerald else ElectricBlueSoft,
                fontWeight = FontWeight.Bold
              )
            }

            LinearProgressIndicator(
              progress = { (readiness.readinessScorePercent / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = if (readiness.readinessScorePercent >= 80) SuccessEmerald else ElectricBlueSoft,
              trackColor = Color.White.copy(alpha = 0.2f)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ScoreMiniStat("Courses", "${readiness.completedCoursesCount}/${readiness.totalCoursesCount}", Modifier.weight(1f))
              ScoreMiniStat("Labs Logged", "${readiness.labsLoggedCount}", Modifier.weight(1f))
              ScoreMiniStat("Certificates", "${readiness.verifiedCertificatesCount}", Modifier.weight(1f))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val nextId = readiness.nextDeliverableCourseId
              if (nextId != null) {
                Button(
                  onClick = { onOpenCourse(nextId) },
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                  modifier = Modifier.weight(1f).testTag("career_build_next_deliverable_button")
                ) {
                  Text("Build Next Deliverable", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
              OutlinedButton(
                onClick = {
                  clipboard.setText(AnnotatedString(portfolioPitch))
                  pitchCopied = true
                },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1f).testTag("copy_portfolio_pitch_button")
              ) {
                Icon(
                  imageVector = if (pitchCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (pitchCopied) "Pitch Copied" else "Copy Pitch")
              }
            }
          }
        }
      }

      // Live Proof-of-Work Portfolio Deliverables (5 Stages)
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "AI Academy Proof-of-Work Portfolio",
            style = MaterialTheme.typography.titleLarge
          )
          TextButton(onClick = onOpenCertificates, modifier = Modifier.testTag("career_view_certificates_button")) {
            Text("Credentials")
          }
        }
      }

      items(readiness.deliverables, key = { "deliverable_${it.spec.courseId}" }) { item ->
        PortfolioDeliverableCard(
          item = item,
          onOpenCourse = { onOpenCourse(item.spec.courseId) },
          onOpenCertificates = onOpenCertificates
        )
      }

      // Structured Career Pathways
      if (careerPaths.isNotEmpty()) {
        item {
          Text(
            text = "Career Pathways",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(careerPaths, key = { it.id }) { path ->
              val pct = student.pathProgress.firstOrNull { it.pathId == path.id }?.progressPercent ?: 0L
              LearningPathCard(
                path = path,
                progressPercent = pct,
                onClick = { onOpenLearningPath(path.id) },
                modifier = Modifier.width(276.dp)
              )
            }
          }
        }
      }

      // Interactive Readiness Pillars & Checklists
      item {
        Text(
          text = "Readiness Pillars & Action Checklists",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedPillarId == "ALL",
              onClick = { selectedPillarId = "ALL" },
              label = { Text("All Pillars") }
            )
          }
          items(careerPillars, key = { it.id }) { pillar ->
            FilterChip(
              selected = selectedPillarId == pillar.id,
              onClick = { selectedPillarId = if (selectedPillarId == pillar.id) "ALL" else pillar.id },
              label = { Text(pillar.title.substringBefore(" (")) }
            )
          }
        }
      }

      val visiblePillars = if (selectedPillarId == "ALL") careerPillars
      else careerPillars.filter { it.id == selectedPillarId }

      items(visiblePillars, key = { "pillar_${it.id}" }) { pillar ->
        val checkedCount = pillar.checklist.indices.count { idx ->
          checkedChecklistKeys["${pillar.id}_$idx"] == true
        }
        val linkedDeliverable = readiness.deliverables.firstOrNull { it.spec.courseId == pillar.linkedStageCourseId }
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(pillar.icon, contentDescription = null, tint = ElectricBlue)
                Column {
                  Text(pillar.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                  Text(pillar.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Surface(
                color = (if (checkedCount == pillar.checklist.size) SuccessEmerald else ElectricBlue).copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "$checkedCount/${pillar.checklist.size}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (checkedCount == pillar.checklist.size) SuccessEmerald else ElectricBlue,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            pillar.checklist.forEachIndexed { idx, tip ->
              val key = "${pillar.id}_$idx"
              val checked = checkedChecklistKeys[key] == true
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { checkedChecklistKeys[key] = !checked }
                  .padding(vertical = 4.dp, horizontal = 2.dp)
                  .testTag("career_check_${pillar.id}_$idx"),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                  contentDescription = if (checked) "Completed checklist step" else "Mark checklist step complete",
                  tint = if (checked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = tip,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (linkedDeliverable != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Stage ${linkedDeliverable.spec.stageNumber}: ${linkedDeliverable.statusLabel}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (linkedDeliverable.certificate != null) SuccessEmerald else ElectricBlue,
                  fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { onOpenCourse(linkedDeliverable.spec.courseId) }) {
                  Text("Practice Stage ${linkedDeliverable.spec.stageNumber}")
                  Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "AI Academy Career Courses (${careerCourses.size})",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }

      items(careerCourses, key = { "career_${it.id}" }) { course ->
        val unlocked = hasSubscription || unlockedCourseIds.contains(course.id)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          CourseCard(
            course = course,
            accentColor = accentMap[course.academyId] ?: ElectricBlue,
            isSaved = savedIds.contains(course.id),
            hasSubscription = unlocked,
            progressPercent = enrollmentByCourse[course.id]?.progressPercent,
            onClick = { onOpenCourse(course.id) },
            onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) }
          )
        }
      }
    }
  }
}

@Composable
private fun ScoreMiniStat(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.White.copy(alpha = 0.08f),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
    }
  }
}

@Composable
private fun PortfolioDeliverableCard(
  item: PortfolioDeliverableItem,
  onOpenCourse: () -> Unit,
  onOpenCertificates: () -> Unit
) {
  val isVerified = item.certificate != null
  val accent = when {
    isVerified -> SuccessEmerald
    item.courseCompleted -> ElectricBlue
    item.labsLoggedCount > 0 || item.progressPercent > 0 -> WarningGold
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onOpenCourse)
      .testTag("portfolio_deliverable_${item.spec.stageNumber}"),
    border = BorderStroke(
      width = 1.dp,
      color = if (isVerified) SuccessEmerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
    ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "STAGE ${item.spec.stageNumber} DELIVERABLE",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricBlue,
          fontWeight = FontWeight.Bold
        )
        Surface(
          color = accent.copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (isVerified) {
              Icon(Icons.Filled.Verified, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(13.dp))
            }
            Text(
              text = item.statusLabel,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Text(
        text = item.spec.deliverableTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = item.spec.careerProofSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${item.labsLoggedCount} labs saved • ${item.passedQuizCount} quizzes passed",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isVerified) {
          TextButton(onClick = onOpenCertificates) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("View Cert")
          }
        } else {
          TextButton(onClick = onOpenCourse) {
            Text(if (item.progressPercent > 0) "Continue" else "Build Proof")
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
          }
        }
      }
    }
  }
}
