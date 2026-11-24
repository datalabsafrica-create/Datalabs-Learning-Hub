package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.data.model.Course
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.TierBadge
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.AdminOverviewData
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  viewModel: MainViewModel,
  catalogState: UiState<HubCatalogSnapshot>,
  isAdmin: Boolean,
  isSeedingCatalog: Boolean,
  onBack: () -> Unit
) {
  if (!isAdmin) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Administrator Console") },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp)
      ) {
        EmptyStateCard(
          icon = Icons.Filled.Lock,
          title = "Administrator Access Required",
          description = "This management console is restricted to verified Datalabs Learning Hub administrators.",
          actionLabel = "Go Back",
          onAction = onBack
        )
      }
    }
    return
  }

  val overviewState by viewModel.adminOverview.collectAsStateWithLifecycle()
  LaunchedEffect(isAdmin) {
    if (isAdmin) viewModel.loadAdminOverview()
  }

  val catalog = (catalogState as? UiState.Success)?.data ?: HubCatalogSnapshot()
  val overview = (overviewState as? UiState.Success)?.data ?: AdminOverviewData()

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Analytics", "Catalog & Courses", "Assessments & Paths", "Students & Ops")

  var courseSearch by remember { mutableStateOf("") }
  var editingCourse by remember { mutableStateOf<Course?>(null) }
  val filteredCourses = remember(catalog.courses, courseSearch) {
    val q = courseSearch.trim().lowercase()
    if (q.isBlank()) catalog.courses.take(30)
    else catalog.courses.filter {
      it.title.lowercase().contains(q) || it.academyName.lowercase().contains(q)
    }.take(40)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Admin Management Console") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("admin_dashboard_screen")
    ) {
      ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
        tabs.forEachIndexed { idx, label ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = { Text(label) }
          )
        }
      }

      if (overviewState is UiState.Loading && selectedTab == 0) {
        LoadingStateView("Loading platform analytics...")
        return@Column
      }
      if (overviewState is UiState.Error && selectedTab == 3) {
        ErrorStateCard(
          message = (overviewState as UiState.Error).message,
          onRetry = { viewModel.loadAdminOverview() }
        )
        return@Column
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        when (selectedTab) {
          0 -> {
            item {
              AdminAnalyticsSection(
                catalog = catalog,
                overview = overview,
                isSeeding = isSeedingCatalog,
                onResyncStarterCatalog = { viewModel.adminResyncStarterCatalog() }
              )
            }
          }

          1 -> {
            item {
              AdminCatalogFormsSection(
                viewModel = viewModel,
                academies = catalog.academies,
                instructors = catalog.instructors,
                courses = catalog.courses
              )
            }
            item {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Manage Courses (${catalog.courses.size} total)",
                  style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                  value = courseSearch,
                  onValueChange = { courseSearch = it },
                  label = { Text("Filter courses by title or academy...") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("admin_course_filter_input")
                )
              }
            }
            items(filteredCourses, key = { it.id }) { course ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(course.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text(
                        "${course.academyName} • ${course.lessonCount} lessons • ${course.enrollmentCount} enrolled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    TierBadge(isPremium = course.premiumOnly, hasSubscription = false)
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                      selected = course.published,
                      onClick = { viewModel.adminToggleCoursePublish(course) },
                      leadingIcon = {
                        Icon(
                          if (course.published) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp)
                        )
                      },
                      label = { Text(if (course.published) "Published" else "Hidden") }
                    )
                    FilterChip(
                      selected = course.featured,
                      onClick = { viewModel.adminToggleCourseFeatured(course) },
                      leadingIcon = {
                        Icon(
                          if (course.featured) Icons.Filled.Star else Icons.Filled.StarBorder,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = if (course.featured) WarningGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      },
                      label = { Text(if (course.featured) "Featured" else "Feature") }
                    )
                    OutlinedButton(onClick = { viewModel.adminToggleCourseTier(course) }) {
                      Text(if (course.premiumOnly) "Set Free" else "Set Pro")
                    }
                    OutlinedButton(
                      onClick = { editingCourse = course },
                      modifier = Modifier.testTag("admin_edit_course_${course.id}")
                    ) {
                      Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Edit")
                    }
                  }
                }
              }
            }
          }

          2 -> {
            item {
              AdminLessonAndQuizEditorsCard(
                viewModel = viewModel,
                courses = catalog.courses
              )
            }
            item {
              AdminAssessmentsAndPathsSection(
                viewModel = viewModel,
                academies = catalog.academies,
                courses = catalog.courses
              )
            }
          }

          3 -> {
            item {
              AdminSubmissionReviewCard(
                viewModel = viewModel,
                overview = overview,
                courses = catalog.courses
              )
            }
            item {
              AdminSingleCourseUnlockSection(
                viewModel = viewModel,
                students = overview.students,
                courses = catalog.courses,
                overview = overview
              )
            }
            item {
              AdminStudentsAndOpsSection(
                viewModel = viewModel,
                overview = overview,
                plans = catalog.plans,
                courses = catalog.courses
              )
            }
          }
        }
      }
    }
  }

  editingCourse?.let { crs ->
    AdminEditCourseDialog(
      course = crs,
      onDismiss = { editingCourse = null },
      onSave = { title, subtitle, desc, lvl, tier, usd, ugx, cat ->
        viewModel.adminUpdateCourse(crs, title, subtitle, desc, lvl, tier, usd, ugx, cat)
        editingCourse = null
      }
    )
  }
}
