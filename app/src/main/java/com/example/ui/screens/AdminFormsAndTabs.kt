package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Academy
import com.example.data.model.Course
import com.example.data.model.Instructor
import com.example.data.model.QuizQuestion
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.AdminOverviewData
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AdminMetricCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun AdminAnalyticsSection(
  catalog: HubCatalogSnapshot,
  overview: AdminOverviewData,
  isSeeding: Boolean,
  onResyncStarterCatalog: () -> Unit
) {
  val activeSubs = overview.subscriptions.count { it.status == "ACTIVE" }
  val freeCourses = catalog.courses.count { !it.premiumOnly }
  val premiumCourses = catalog.courses.count { it.premiumOnly }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(
      colors = CardDefaults.cardColors(containerColor = NavyDeep),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Datalabs Platform Command", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
          "Real-time telemetry across academies, courses, learners, subscriptions, and verified certificates.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFCBD5E1)
        )
        Button(
          onClick = onResyncStarterCatalog,
          enabled = !isSeeding,
          modifier = Modifier.testTag("admin_resync_catalog_button")
        ) {
          Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isSeeding) "Syncing AI Academy..." else "Sync AI Academy Catalogue (5 Courses)")
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      AdminMetricCard("Academies", "${catalog.academies.size}", ElectricBlue, Modifier.weight(1f))
      AdminMetricCard("Courses", "${catalog.courses.size}", ElectricBlue, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      AdminMetricCard("Students", "${overview.students.size}", SuccessEmerald, Modifier.weight(1f))
      AdminMetricCard("Enrollments", "${overview.enrollments.size}", SuccessEmerald, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      AdminMetricCard("Active Pro", "$activeSubs", WarningGold, Modifier.weight(1f))
      AdminMetricCard("Certificates", "${overview.certificates.size}", ElectricBlue, Modifier.weight(1f))
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Catalogue Breakdown", style = MaterialTheme.typography.titleMedium)
        Text(
          "Free Courses: $freeCourses • Premium Courses: $premiumCourses • Learning Paths: ${catalog.learningPaths.size} • Instructors: ${catalog.instructors.size}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Active Academy Catalogue", style = MaterialTheme.typography.titleSmall)
        catalog.academies.sortedByDescending { ac ->
          catalog.courses.count { it.academyId == ac.id }
        }.take(6).forEach { academy ->
          val count = catalog.courses.count { it.academyId == academy.id }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(academy.shortName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("$count courses", style = MaterialTheme.typography.bodySmall, color = ElectricBlue)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCatalogFormsSection(
  viewModel: MainViewModel,
  academies: List<Academy>,
  instructors: List<Instructor>,
  courses: List<Course>
) {
  // Academy Form State
  var academyName by remember { mutableStateOf("") }
  var academyShort by remember { mutableStateOf("") }
  var academyDesc by remember { mutableStateOf("") }
  var academyIcon by remember { mutableStateOf("school") }
  var academyHex by remember { mutableStateOf("#2563EB") }

  // Course Form State
  var selectedAcademy by remember(academies) { mutableStateOf(academies.firstOrNull()) }
  var academyMenuExpanded by remember { mutableStateOf(false) }
  var selectedInstructor by remember(instructors) { mutableStateOf(instructors.firstOrNull()) }
  var instructorMenuExpanded by remember { mutableStateOf(false) }
  var courseTitle by remember { mutableStateOf("") }
  var courseSubtitle by remember { mutableStateOf("") }
  var courseDesc by remember { mutableStateOf("") }
  var courseLevel by remember { mutableStateOf("BEGINNER") }
  var courseTier by remember { mutableStateOf("FREE") }
  var courseDuration by remember { mutableStateOf("120") }
  var coursePriceUsd by remember { mutableStateOf("19") }
  var coursePriceUgx by remember { mutableStateOf("70000") }
  var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }

  val coverPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri -> pickedCoverUri = uri }

  // Lesson Form State
  var lessonCourse by remember(courses) { mutableStateOf(courses.firstOrNull()) }
  var lessonCourseExpanded by remember { mutableStateOf(false) }
  var lessonTitle by remember { mutableStateOf("") }
  var lessonSummary by remember { mutableStateOf("") }
  var lessonOrder by remember { mutableStateOf("1") }
  var lessonDuration by remember { mutableStateOf("15") }
  var lessonFormat by remember { mutableStateOf("WRITTEN") }
  var lessonFreePreview by remember { mutableStateOf(true) }
  var lessonNotes by remember { mutableStateOf("") }
  var lessonVideoUrl by remember { mutableStateOf("") }
  var downloadTitle by remember { mutableStateOf("") }
  var attachmentUri by remember { mutableStateOf<Uri?>(null) }

  val attachmentPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri -> attachmentUri = uri }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Create / Update Academy
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.School, contentDescription = null, tint = ElectricBlue)
          Text("Create New Academy", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(
          value = academyName,
          onValueChange = { academyName = it },
          label = { Text("Academy Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("admin_academy_name_input")
        )
        OutlinedTextField(
          value = academyShort,
          onValueChange = { academyShort = it },
          label = { Text("Short Name (e.g. Cloud & DevOps)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = academyDesc,
          onValueChange = { academyDesc = it },
          label = { Text("Description") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = academyIcon,
            onValueChange = { academyIcon = it },
            label = { Text("Icon Key") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = academyHex,
            onValueChange = { academyHex = it },
            label = { Text("Accent Hex") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Button(
          onClick = {
            if (academyName.trim().length >= 2 && academyShort.trim().length >= 2 && academyDesc.trim().length >= 4) {
              val id = "acad_${System.currentTimeMillis()}"
              viewModel.adminCreateOrUpdateAcademy(
                academyId = id,
                name = academyName,
                shortName = academyShort,
                description = academyDesc,
                iconKey = academyIcon.ifBlank { "school" },
                accentHex = academyHex.ifBlank { "#2563EB" },
                displayOrder = (academies.size + 1).toLong(),
                courseCount = 0L,
                published = true,
                isNew = true
              )
              academyName = ""
              academyShort = ""
              academyDesc = ""
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("admin_create_academy_button")
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Publish Academy")
        }
      }
    }

    // 2. Create Course
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Create New Course", style = MaterialTheme.typography.titleMedium)

        ExposedDropdownMenuBox(
          expanded = academyMenuExpanded,
          onExpandedChange = { academyMenuExpanded = !academyMenuExpanded }
        ) {
          OutlinedTextField(
            value = selectedAcademy?.name ?: "Select Academy",
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Academy") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = academyMenuExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = academyMenuExpanded,
            onDismissRequest = { academyMenuExpanded = false }
          ) {
            academies.forEach { ac ->
              DropdownMenuItem(
                text = { Text(ac.name) },
                onClick = {
                  selectedAcademy = ac
                  academyMenuExpanded = false
                }
              )
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = instructorMenuExpanded,
          onExpandedChange = { instructorMenuExpanded = !instructorMenuExpanded }
        ) {
          OutlinedTextField(
            value = selectedInstructor?.fullName ?: "Select Instructor",
            onValueChange = {},
            readOnly = true,
            label = { Text("Lead Instructor") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instructorMenuExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = instructorMenuExpanded,
            onDismissRequest = { instructorMenuExpanded = false }
          ) {
            instructors.forEach { inst ->
              DropdownMenuItem(
                text = { Text("${inst.fullName} — ${inst.specialty}") },
                onClick = {
                  selectedInstructor = inst
                  instructorMenuExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = courseTitle,
          onValueChange = { courseTitle = it },
          label = { Text("Course Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("admin_course_title_input")
        )
        OutlinedTextField(
          value = courseSubtitle,
          onValueChange = { courseSubtitle = it },
          label = { Text("Subtitle") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = courseDesc,
          onValueChange = { courseDesc = it },
          label = { Text("Course Description") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("BEGINNER", "INTERMEDIATE", "ADVANCED").forEach { lvl ->
            FilterChip(selected = courseLevel == lvl, onClick = { courseLevel = lvl }, label = { Text(lvl) })
          }
          listOf("FREE", "PREMIUM").forEach { tier ->
            FilterChip(selected = courseTier == tier, onClick = { courseTier = tier }, label = { Text(tier) })
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = courseDuration,
            onValueChange = { courseDuration = it },
            label = { Text("Minutes") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = coursePriceUsd,
            onValueChange = { coursePriceUsd = it },
            label = { Text("USD") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = coursePriceUgx,
            onValueChange = { coursePriceUgx = it },
            label = { Text("UGX") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedButton(
          onClick = {
            coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (pickedCoverUri != null) "Cover Image Selected" else "Pick Cover Image (Optional)")
        }

        Button(
          onClick = {
            val ac = selectedAcademy ?: return@Button
            val inst = selectedInstructor
            if (courseTitle.trim().length >= 2 && courseSubtitle.trim().length >= 2 && courseDesc.trim().length >= 4) {
              viewModel.adminCreateCourse(
                courseId = "crs_${System.currentTimeMillis()}",
                academyId = ac.id,
                academyName = ac.name,
                title = courseTitle,
                subtitle = courseSubtitle,
                description = courseDesc,
                instructorId = inst?.id ?: "inst_datalabs_faculty",
                instructorName = inst?.fullName ?: "Datalabs Faculty",
                level = courseLevel,
                durationMinutes = courseDuration.toLongOrNull()?.coerceAtLeast(5L) ?: 120L,
                accessTier = courseTier,
                priceUsd = if (courseTier == "FREE") 0.0 else (coursePriceUsd.toDoubleOrNull() ?: 19.0),
                priceUgx = if (courseTier == "FREE") 0L else (coursePriceUgx.toLongOrNull() ?: 70000L),
                coverUri = pickedCoverUri
              )
              courseTitle = ""
              courseSubtitle = ""
              courseDesc = ""
              pickedCoverUri = null
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("admin_create_course_button")
        ) {
          Text("Create Course")
        }
      }
    }

    // 3. Add Lesson + Material
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Add Lesson & Downloadable Material", style = MaterialTheme.typography.titleMedium)

        ExposedDropdownMenuBox(
          expanded = lessonCourseExpanded,
          onExpandedChange = { lessonCourseExpanded = !lessonCourseExpanded }
        ) {
          OutlinedTextField(
            value = lessonCourse?.title ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            label = { Text("Course") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lessonCourseExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = lessonCourseExpanded,
            onDismissRequest = { lessonCourseExpanded = false }
          ) {
            courses.take(40).forEach { crs ->
              DropdownMenuItem(
                text = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                onClick = {
                  lessonCourse = crs
                  lessonCourseExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = lessonTitle,
          onValueChange = { lessonTitle = it },
          label = { Text("Lesson Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = lessonSummary,
          onValueChange = { lessonSummary = it },
          label = { Text("Lesson Summary") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = lessonOrder,
            onValueChange = { lessonOrder = it },
            label = { Text("Order #") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = lessonDuration,
            onValueChange = { lessonDuration = it },
            label = { Text("Minutes") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("WRITTEN", "VIDEO", "AUDIO", "SLIDES", "PDF", "TEMPLATE", "EXERCISE").forEach { fmt ->
            FilterChip(selected = lessonFormat == fmt, onClick = { lessonFormat = fmt }, label = { Text(fmt) })
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Free Preview Lesson", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = lessonFreePreview, onCheckedChange = { lessonFreePreview = it })
        }
        OutlinedTextField(
          value = lessonNotes,
          onValueChange = { lessonNotes = it },
          label = { Text("Lesson Study Notes (Markdown / Text)") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = lessonVideoUrl,
          onValueChange = { lessonVideoUrl = it },
          label = { Text("Optional Video/Audio Stream URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = downloadTitle,
          onValueChange = { downloadTitle = it },
          label = { Text("Downloadable Resource Title (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          onClick = { attachmentPicker.launch(arrayOf("application/pdf", "text/*", "application/zip")) },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (attachmentUri != null) "Attachment Ready for Upload" else "Attach PDF / Dataset / Template")
        }
        Button(
          onClick = {
            val target = lessonCourse ?: return@Button
            if (lessonTitle.trim().length >= 2 && lessonSummary.trim().length >= 2 && lessonNotes.trim().length >= 2) {
              viewModel.adminAddLesson(
                course = target,
                title = lessonTitle,
                summary = lessonSummary,
                orderIndex = lessonOrder.toLongOrNull()?.coerceAtLeast(0L) ?: 1L,
                durationMinutes = lessonDuration.toLongOrNull()?.coerceAtLeast(1L) ?: 15L,
                format = lessonFormat,
                freePreview = lessonFreePreview,
                notesMarkdown = lessonNotes,
                videoUrl = lessonVideoUrl.takeIf { it.isNotBlank() },
                downloadTitle = downloadTitle.takeIf { it.isNotBlank() },
                attachmentUri = attachmentUri
              )
              lessonTitle = ""
              lessonSummary = ""
              lessonNotes = ""
              lessonVideoUrl = ""
              downloadTitle = ""
              attachmentUri = null
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Add Lesson to Course")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminAssessmentsAndPathsSection(
  viewModel: MainViewModel,
  academies: List<Academy>,
  courses: List<Course>
) {
  var targetCourse by remember(courses) { mutableStateOf(courses.firstOrNull()) }
  var courseExpanded by remember { mutableStateOf(false) }

  // Quiz state
  var quizTitle by remember { mutableStateOf("") }
  var quizDesc by remember { mutableStateOf("") }
  var quizType by remember { mutableStateOf("FINAL_EXAM") }
  var passingPercent by remember { mutableStateOf("70") }
  var allowRetakes by remember { mutableStateOf(true) }
  var qPrompt by remember { mutableStateOf("") }
  var qOptionA by remember { mutableStateOf("") }
  var qOptionB by remember { mutableStateOf("") }
  var qOptionC by remember { mutableStateOf("") }
  var qCorrectIndex by remember { mutableStateOf(0) }
  val quizQuestions = remember { mutableStateListOf<QuizQuestion>() }

  // Assignment state
  var asgTitle by remember { mutableStateOf("") }
  var asgInstructions by remember { mutableStateOf("") }
  var asgFormat by remember { mutableStateOf("Written Case Study / Dataset Link") }
  var asgMaxPoints by remember { mutableStateOf("100") }

  // Learning Path state
  var lpAcademy by remember(academies) { mutableStateOf(academies.firstOrNull()) }
  var lpAcademyExpanded by remember { mutableStateOf(false) }
  var lpTitle by remember { mutableStateOf("") }
  var lpDesc by remember { mutableStateOf("") }
  var lpRole by remember { mutableStateOf("") }
  var lpHours by remember { mutableStateOf("20") }
  var lpPremium by remember { mutableStateOf(false) }
  val lpSelectedCourses = remember { mutableStateListOf<Course>() }

  // Instructor state
  var instName by remember { mutableStateOf("") }
  var instTitle by remember { mutableStateOf("") }
  var instSpecialty by remember { mutableStateOf("") }
  var instBio by remember { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Add Quiz
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Quiz, contentDescription = null, tint = ElectricBlue)
          Text("Create Course Quiz", style = MaterialTheme.typography.titleMedium)
        }
        ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }) {
          OutlinedTextField(
            value = targetCourse?.title ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            label = { Text("Course") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
            courses.take(40).forEach { crs ->
              DropdownMenuItem(
                text = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                onClick = { targetCourse = crs; courseExpanded = false }
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = quizType == "MODULE_QUIZ", onClick = { quizType = "MODULE_QUIZ" }, label = { Text("Module Quiz") })
          FilterChip(selected = quizType == "FINAL_EXAM", onClick = { quizType = "FINAL_EXAM" }, label = { Text("Final Exam") })
        }
        OutlinedTextField(value = quizTitle, onValueChange = { quizTitle = it }, label = { Text("Quiz Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = quizDesc, onValueChange = { quizDesc = it }, label = { Text("Quiz Instructions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(value = passingPercent, onValueChange = { passingPercent = it }, label = { Text("Pass %") }, singleLine = true, modifier = Modifier.weight(1f))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Retakes", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(6.dp))
            Switch(checked = allowRetakes, onCheckedChange = { allowRetakes = it })
          }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add Question (${quizQuestions.size} added)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = qPrompt, onValueChange = { qPrompt = it }, label = { Text("Question Prompt") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = qOptionA, onValueChange = { qOptionA = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = qOptionB, onValueChange = { qOptionB = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = qOptionC, onValueChange = { qOptionC = it }, label = { Text("Option C") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("Correct:", style = MaterialTheme.typography.labelMedium)
              listOf("A", "B", "C").forEachIndexed { idx, lbl ->
                FilterChip(selected = qCorrectIndex == idx, onClick = { qCorrectIndex = idx }, label = { Text(lbl) })
              }
            }
            OutlinedButton(
              onClick = {
                val opts = listOf(qOptionA.trim(), qOptionB.trim(), qOptionC.trim()).filter { it.isNotBlank() }
                if (qPrompt.isNotBlank() && opts.size >= 2) {
                  val ans = opts.getOrElse(qCorrectIndex) { opts.first() }
                  quizQuestions.add(
                    QuizQuestion(
                      id = "q_${quizQuestions.size + 1}",
                      type = "MULTIPLE_CHOICE",
                      prompt = qPrompt.trim(),
                      options = opts,
                      correctAnswer = ans,
                      explanation = "Reviewed by Datalabs Faculty",
                      points = 10
                    )
                  )
                  qPrompt = ""; qOptionA = ""; qOptionB = ""; qOptionC = ""
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Append Question to Quiz")
            }
          }
        }

        Button(
          onClick = {
            val c = targetCourse ?: return@Button
            if (quizTitle.trim().length >= 2 && quizDesc.trim().length >= 2 && quizQuestions.isNotEmpty()) {
              viewModel.adminCreateQuiz(
                courseId = c.id,
                title = quizTitle,
                description = quizDesc,
                passingScorePercent = passingPercent.toLongOrNull()?.coerceIn(1L, 100L) ?: 70L,
                maxAttempts = if (allowRetakes) 10L else 1L,
                allowRetakes = allowRetakes,
                assessmentType = quizType,
                questions = quizQuestions.toList()
              )
              quizTitle = ""; quizDesc = ""; quizQuestions.clear()
            }
          },
          enabled = quizQuestions.isNotEmpty(),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Publish Quiz (${quizQuestions.size} Questions)")
        }
      }
    }

    // 2. Add Assignment
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Create Practical Assignment", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = asgTitle, onValueChange = { asgTitle = it }, label = { Text("Assignment Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = asgInstructions, onValueChange = { asgInstructions = it }, label = { Text("Practical Brief & Rubric") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = asgFormat, onValueChange = { asgFormat = it }, label = { Text("Deliverable Format") }, singleLine = true, modifier = Modifier.weight(2f))
          OutlinedTextField(value = asgMaxPoints, onValueChange = { asgMaxPoints = it }, label = { Text("Points") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Button(
          onClick = {
            val c = targetCourse ?: return@Button
            if (asgTitle.trim().length >= 2 && asgInstructions.trim().length >= 4 && asgFormat.trim().length >= 2) {
              viewModel.adminCreateAssignment(
                courseId = c.id,
                title = asgTitle,
                instructions = asgInstructions,
                deliverableFormat = asgFormat,
                maxPoints = asgMaxPoints.toLongOrNull()?.coerceIn(1L, 1000L) ?: 100L
              )
              asgTitle = ""; asgInstructions = ""
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Publish Assignment")
        }
      }
    }

    // 3. Add Learning Path
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Route, contentDescription = null, tint = ElectricBlue)
          Text("Create Structured Learning Path", style = MaterialTheme.typography.titleMedium)
        }
        ExposedDropdownMenuBox(expanded = lpAcademyExpanded, onExpandedChange = { lpAcademyExpanded = !lpAcademyExpanded }) {
          OutlinedTextField(
            value = lpAcademy?.name ?: "Select Primary Academy",
            onValueChange = {},
            readOnly = true,
            label = { Text("Academy") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lpAcademyExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(expanded = lpAcademyExpanded, onDismissRequest = { lpAcademyExpanded = false }) {
            academies.forEach { ac ->
              DropdownMenuItem(text = { Text(ac.name) }, onClick = { lpAcademy = ac; lpAcademyExpanded = false })
            }
          }
        }
        OutlinedTextField(value = lpTitle, onValueChange = { lpTitle = it }, label = { Text("Pathway Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lpDesc, onValueChange = { lpDesc = it }, label = { Text("Outcome Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = lpRole, onValueChange = { lpRole = it }, label = { Text("Target Role") }, singleLine = true, modifier = Modifier.weight(2f))
          OutlinedTextField(value = lpHours, onValueChange = { lpHours = it }, label = { Text("Est. Hours") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Pro Membership Required", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = lpPremium, onCheckedChange = { lpPremium = it })
        }
        Text("Tap up to 5 courses to include (${lpSelectedCourses.size} selected):", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          courses.take(15).forEach { crs ->
            val picked = lpSelectedCourses.any { it.id == crs.id }
            FilterChip(
              selected = picked,
              onClick = {
                if (picked) lpSelectedCourses.removeAll { it.id == crs.id }
                else if (lpSelectedCourses.size < 8) lpSelectedCourses.add(crs)
              },
              label = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
          }
        }
        Button(
          onClick = {
            val ac = lpAcademy ?: return@Button
            if (lpTitle.trim().length >= 2 && lpDesc.trim().length >= 4 && lpRole.trim().length >= 2 && lpSelectedCourses.isNotEmpty()) {
              viewModel.adminCreateLearningPath(
                title = lpTitle,
                description = lpDesc,
                academyId = ac.id,
                targetRole = lpRole,
                selectedCourses = lpSelectedCourses.toList(),
                estimatedHours = lpHours.toLongOrNull()?.coerceIn(1L, 1000L) ?: 20L,
                premiumOnly = lpPremium
              )
              lpTitle = ""; lpDesc = ""; lpRole = ""; lpSelectedCourses.clear()
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Publish Learning Path")
        }
      }
    }

    // 4. Add Instructor
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = ElectricBlue)
          Text("Add Faculty Instructor", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(value = instName, onValueChange = { instName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = instTitle, onValueChange = { instTitle = it }, label = { Text("Academic/Industry Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = instSpecialty, onValueChange = { instSpecialty = it }, label = { Text("Specialty") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = instBio, onValueChange = { instBio = it }, label = { Text("Short Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Button(
          onClick = {
            if (instName.trim().length >= 2 && instTitle.trim().length >= 2 && instSpecialty.trim().length >= 2 && instBio.trim().length >= 4) {
              viewModel.adminCreateInstructor(instName, instTitle, instBio, instSpecialty)
              instName = ""; instTitle = ""; instSpecialty = ""; instBio = ""
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Save Instructor")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentsAndOpsSection(
  viewModel: MainViewModel,
  overview: AdminOverviewData,
  plans: List<SubscriptionPlan>,
  courses: List<Course>
) {
  var annTitle by remember { mutableStateOf("") }
  var annMsg by remember { mutableStateOf("") }
  var annCategory by remember { mutableStateOf("Academy Update") }

  var certCourse by remember(courses) { mutableStateOf(courses.firstOrNull()) }
  var certCourseExpanded by remember { mutableStateOf(false) }

  var notifStudent by remember(overview.students) { mutableStateOf(overview.students.firstOrNull()) }
  var notifStudentExpanded by remember { mutableStateOf(false) }
  var notifTitle by remember { mutableStateOf("") }
  var notifBody by remember { mutableStateOf("") }
  var notifType by remember { mutableStateOf("COURSE_UPDATE") }

  val subsByUid = remember(overview.subscriptions) { overview.subscriptions.associateBy { it.userId } }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Broadcast Announcement
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Campaign, contentDescription = null, tint = ElectricBlue)
          Text("Broadcast Platform Announcement", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Announcement Headline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = annCategory, onValueChange = { annCategory = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = annMsg, onValueChange = { annMsg = it }, label = { Text("Message to All Learners") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Button(
          onClick = {
            if (annTitle.trim().length >= 2 && annMsg.trim().length >= 2 && annCategory.trim().length >= 2) {
              viewModel.adminSendAnnouncement(annTitle, annMsg, annCategory)
              annTitle = ""; annMsg = ""
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("admin_broadcast_button")
        ) {
          Text("Publish Announcement")
        }
      }
    }

    // 2. Targeted Notification to Student
    if (overview.students.isNotEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = ElectricBlue)
            Text("Send Direct Learner Notification", style = MaterialTheme.typography.titleMedium)
          }
          ExposedDropdownMenuBox(expanded = notifStudentExpanded, onExpandedChange = { notifStudentExpanded = !notifStudentExpanded }) {
            OutlinedTextField(
              value = notifStudent?.let { "${it.fullName} (${it.email})" } ?: "Select Learner",
              onValueChange = {},
              readOnly = true,
              label = { Text("Recipient") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notifStudentExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = notifStudentExpanded, onDismissRequest = { notifStudentExpanded = false }) {
              overview.students.forEach { st ->
                DropdownMenuItem(text = { Text("${st.fullName} (${st.email})") }, onClick = { notifStudent = st; notifStudentExpanded = false })
              }
            }
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("COURSE_UPDATE", "NEW_COURSE", "REMINDER", "SUBSCRIPTION", "CERTIFICATE").forEach { t ->
              FilterChip(selected = notifType == t, onClick = { notifType = t }, label = { Text(t) })
            }
          }
          OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("Message Body") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          Button(
            onClick = {
              val s = notifStudent ?: return@Button
              if (notifTitle.trim().length >= 2 && notifBody.trim().length >= 2) {
                viewModel.adminSendUserNotification(s.userId, notifTitle, notifBody, notifType)
                notifTitle = ""; notifBody = ""
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Send Notification")
          }
        }
      }
    }

    // 3. Subscription Plan Pricing (USD & UGX)
    if (plans.isNotEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = WarningGold)
            Text("Subscription Plan Pricing (USD & UGX)", style = MaterialTheme.typography.titleMedium)
          }
          plans.forEach { plan ->
            var usdText by remember(plan) { mutableStateOf(plan.priceUsd.toString()) }
            var ugxText by remember(plan) { mutableStateOf(plan.priceUgx.toString()) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("${plan.name} (${plan.billingCycle})", style = MaterialTheme.typography.titleSmall)
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = usdText, onValueChange = { usdText = it }, label = { Text("USD") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = ugxText, onValueChange = { ugxText = it }, label = { Text("UGX") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                  onClick = {
                    val u = usdText.toDoubleOrNull() ?: plan.priceUsd
                    val ug = ugxText.toLongOrNull() ?: plan.priceUgx
                    viewModel.adminUpdatePlan(plan, u, ug)
                  }
                ) {
                  Text("Save")
                }
              }
            }
          }
        }
      }
    }

    // 4. Student Roster: Subscriptions & Manual Certificate Issuance
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Learners, Subscriptions & Certificates (${overview.students.size})", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = certCourseExpanded, onExpandedChange = { certCourseExpanded = !certCourseExpanded }) {
          OutlinedTextField(
            value = certCourse?.title ?: "Select Course for Certificate Issuance",
            onValueChange = {},
            readOnly = true,
            label = { Text("Course for Manual Certificate Grant") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = certCourseExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(expanded = certCourseExpanded, onDismissRequest = { certCourseExpanded = false }) {
            courses.take(40).forEach { crs ->
              DropdownMenuItem(
                text = { Text(crs.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                onClick = { certCourse = crs; certCourseExpanded = false }
              )
            }
          }
        }

        overview.students.forEach { student ->
          val sub: Subscription? = subsByUid[student.userId]
          val isActivePro = sub?.status == "ACTIVE"
          val enrollmentCount = overview.enrollments.count { it.userId == student.userId }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(student.fullName, style = MaterialTheme.typography.titleSmall)
                  Text(student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    "Role: ${student.role} • Enrolled: $enrollmentCount • Sub: ${sub?.status ?: "FREE"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActivePro) SuccessEmerald else ElectricBlue
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { viewModel.adminGrantSubscription(student.userId, !isActivePro) }
                ) {
                  Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isActivePro) "Revoke Pro" else "Activate Pro")
                }
                TextButton(
                  onClick = {
                    val c = certCourse ?: return@TextButton
                    viewModel.adminIssueCertificateForUser(student.userId, student.fullName, c)
                  }
                ) {
                  Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Issue Cert")
                }
              }
            }
          }
        }
      }
    }
  }
}
