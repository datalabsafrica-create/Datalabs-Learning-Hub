package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Course
import com.example.data.model.UiState
import com.example.data.seed.CategoryDefinitions
import com.example.ui.components.AcademyCard
import com.example.ui.components.CourseCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.parseAccentColor
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  compareCourseIds: List<String>,
  selectedCategory: String? = null,
  onSelectCategory: (String?) -> Unit = {},
  onOpenAcademy: (String) -> Unit,
  onOpenCourse: (String) -> Unit,
  onToggleSaveCourse: (Course, Boolean) -> Unit,
  onToggleCompare: (String) -> Unit,
  onClearCompare: () -> Unit
) {
  if (catalogState is UiState.Loading) {
    LoadingStateView("Loading course directory...")
    return
  }
  if (catalogState is UiState.Error) {
    ErrorStateCard(message = catalogState.message)
    return
  }

  val catalog = (catalogState as UiState.Success).data
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val savedIds = student.savedCourses.map { it.courseId }.toSet()
  val enrollmentMap = student.enrollments.associateBy { it.courseId }

  val academies = remember(catalog.academies) { catalog.academies.filter { it.published } }
  val allCourses = remember(catalog.courses) { catalog.courses.filter { it.published } }
  val accentMap = remember(academies) { academies.associate { it.id to parseAccentColor(it.accentHex) } }
  val activeCategories = remember(allCourses) {
    CategoryDefinitions.nonEmptyCategoriesForCourses(allCourses)
  }

  var query by remember { mutableStateOf("") }
  var selectedAcademyId by remember { mutableStateOf<String?>(null) }
  var selectedLevel by remember { mutableStateOf<String?>(null) }
  var selectedTier by remember { mutableStateOf<String?>(null) }
  var selectedDuration by remember { mutableStateOf<String?>(null) }
  var selectedFormat by remember { mutableStateOf<String?>(null) }
  var showCompareSheet by remember { mutableStateOf(false) }

  val filteredCourses = remember(
    allCourses, query, selectedAcademyId, selectedCategory, selectedLevel, selectedTier, selectedDuration, selectedFormat
  ) {
    val categoryScoped = if (selectedCategory.isNullOrBlank()) allCourses
    else CategoryDefinitions.coursesForCategory(allCourses, selectedCategory)
    val q = query.trim().lowercase()
    categoryScoped.filter { course ->
      val matchesQuery = q.isBlank() ||
        course.title.lowercase().contains(q) ||
        course.academyName.lowercase().contains(q) ||
        course.instructorName.lowercase().contains(q) ||
        course.tags.any { it.lowercase().contains(q) } ||
        course.description.lowercase().contains(q)
      val matchesAcademy = selectedAcademyId == null || course.academyId == selectedAcademyId
      val matchesLevel = selectedLevel == null || course.level.equals(selectedLevel, ignoreCase = true)
      val matchesTier = selectedTier == null || course.accessTier.equals(selectedTier, ignoreCase = true)
      val matchesDuration = when (selectedDuration) {
        "SHORT" -> course.durationMinutes <= 120L
        "MEDIUM" -> course.durationMinutes in 121L..240L
        "LONG" -> course.durationMinutes > 240L
        else -> true
      }
      val matchesFormat = selectedFormat == null || course.formats.any { it.equals(selectedFormat, ignoreCase = true) }
      matchesQuery && matchesAcademy && matchesLevel && matchesTier && matchesDuration && matchesFormat
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("explore_screen"),
      contentPadding = PaddingValues(bottom = if (compareCourseIds.isNotEmpty()) 96.dp else 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Explore & Discover",
            style = MaterialTheme.typography.headlineLarge
          )
          Text(
            text = if (academies.size <= 1) {
              "Browse all ${allCourses.size} complete courses in ${academies.firstOrNull()?.name ?: "AI Academy"}."
            } else {
              "Browse ${academies.size} academies and ${allCourses.size} practical courses."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("explore_search_input"),
          placeholder = { Text("Search course, topic, skill, or instructor...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp)
        )
      }

      // Filter chips: Tier & Level
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Filled.FilterList,
              contentDescription = "Filters",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilterChip(
              selected = selectedTier == "FREE",
              onClick = { selectedTier = if (selectedTier == "FREE") null else "FREE" },
              label = { Text("Free") }
            )
            FilterChip(
              selected = selectedTier == "PREMIUM",
              onClick = { selectedTier = if (selectedTier == "PREMIUM") null else "PREMIUM" },
              label = { Text("Premium") }
            )
            listOf("BEGINNER" to "Beginner", "INTERMEDIATE" to "Intermediate", "ADVANCED" to "Advanced")
              .forEach { (key, label) ->
                FilterChip(
                  selected = selectedLevel == key,
                  onClick = { selectedLevel = if (selectedLevel == key) null else key },
                  label = { Text(label) }
                )
              }
            listOf("SHORT" to "≤ 2h", "MEDIUM" to "2–4h", "LONG" to "4h+")
              .forEach { (key, label) ->
                FilterChip(
                  selected = selectedDuration == key,
                  onClick = { selectedDuration = if (selectedDuration == key) null else key },
                  label = { Text(label) }
                )
              }
            listOf("WRITTEN", "SLIDES", "PDF", "TEMPLATE", "EXERCISE", "QUIZ", "ASSIGNMENT", "PROJECT")
              .forEach { fmt ->
                FilterChip(
                  selected = selectedFormat == fmt,
                  onClick = { selectedFormat = if (selectedFormat == fmt) null else fmt },
                  label = { Text(fmt.lowercase().replaceFirstChar { it.uppercase() }) }
                )
              }
          }

          // Active Skill Track Categories Filter Row
          if (activeCategories.size > 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All Categories") }
              )
              activeCategories.forEach { cat ->
                FilterChip(
                  selected = selectedCategory == cat.name,
                  onClick = {
                    onSelectCategory(if (selectedCategory == cat.name) null else cat.name)
                  },
                  label = { Text(cat.name) },
                  modifier = Modifier.testTag("explore_cat_${cat.id}")
                )
              }
            }
          }

          // Academy filter row (only shown when multiple academies are active)
          if (academies.size > 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = selectedAcademyId == null,
                onClick = { selectedAcademyId = null },
                label = { Text("All Academies") }
              )
              academies.forEach { academy ->
                FilterChip(
                  selected = selectedAcademyId == academy.id,
                  onClick = {
                    selectedAcademyId = if (selectedAcademyId == academy.id) null else academy.id
                  },
                  label = { Text(academy.shortName) }
                )
              }
            }
          }
        }
      }

      // Active Academy Spotlight or Multi-Academy Carousel
      if (selectedAcademyId == null && query.isBlank() && academies.isNotEmpty()) {
        item {
          if (academies.size == 1) {
            val mainAcademy = academies.first()
            val count = allCourses.count { it.academyId == mainAcademy.id }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              AcademyCard(
                academy = mainAcademy,
                actualCourseCount = count,
                onClick = { onOpenAcademy(mainAcademy.id) },
                modifier = Modifier.fillMaxWidth()
              )
            }
          } else {
            Text(
              text = "Academies",
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(academies, key = { it.id }) { academy ->
                val count = allCourses.count { it.academyId == academy.id }
                AcademyCard(
                  academy = academy,
                  actualCourseCount = count,
                  onClick = { onOpenAcademy(academy.id) },
                  modifier = Modifier.width(236.dp)
                )
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Courses (${filteredCourses.size})",
            style = MaterialTheme.typography.titleLarge
          )
          if (selectedAcademyId != null || selectedCategory != null || selectedLevel != null ||
            selectedTier != null || selectedDuration != null || selectedFormat != null || query.isNotBlank()
          ) {
            TextButton(
              onClick = {
                query = ""
                onSelectCategory(null)
                selectedAcademyId = null
                selectedLevel = null
                selectedTier = null
                selectedDuration = null
                selectedFormat = null
              }
            ) {
              Text("Clear filters")
            }
          }
        }
      }

      if (filteredCourses.isEmpty()) {
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            EmptyStateCard(
              icon = Icons.Filled.Search,
              title = "No courses match these filters",
              description = "Adjust your search or clear filters to explore all ${allCourses.size} courses."
            )
          }
        }
      } else {
        items(filteredCourses, key = { it.id }) { course ->
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            CourseCard(
              course = course,
              accentColor = accentMap[course.academyId] ?: ElectricBlue,
              isSaved = savedIds.contains(course.id),
              isCompared = compareCourseIds.contains(course.id),
              hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
              progressPercent = enrollmentMap[course.id]?.progressPercent,
              onClick = { onOpenCourse(course.id) },
              onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) },
              onToggleCompare = { onToggleCompare(course.id) }
            )
          }
        }
      }
    }

    // Floating Course Comparison Tray
    if (compareCourseIds.isNotEmpty()) {
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NavyDeep,
        shadowElevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = Color(0xFF93C5FD))
            Text(
              text = "${compareCourseIds.size}/3 selected",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClearCompare) {
              Text("Clear", color = Color(0xFFCBD5E1))
            }
            Button(
              onClick = { showCompareSheet = true },
              enabled = compareCourseIds.size >= 2
            ) {
              Text("Compare")
            }
          }
        }
      }
    }
  }

  if (showCompareSheet) {
    val selected = allCourses.filter { compareCourseIds.contains(it.id) }
    ModalBottomSheet(onDismissRequest = { showCompareSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Course Comparison", style = MaterialTheme.typography.headlineMedium)
        selected.forEach { course ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(course.academyName, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
              Text("Level: ${course.level} • Duration: ${formatDuration(course.durationMinutes)} • Lessons: ${course.lessonCount}")
              Text("Tier: ${course.accessTier} • Certificate: ${if (course.certificateAvailable) "Yes" else "No"}")
              Text("Formats: ${course.formats.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
              Button(
                onClick = {
                  showCompareSheet = false
                  onOpenCourse(course.id)
                },
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Text("View Course")
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
