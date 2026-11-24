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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.Academy
import com.example.data.model.Course
import com.example.data.model.Enrollment
import com.example.data.model.UiState
import com.example.data.seed.AcademyProgressionHelper
import com.example.data.seed.CategoryDefinitions
import com.example.ui.components.AcademyCard
import com.example.ui.components.CourseCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LearningPathCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.academyIconFor
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.parseAccentColor
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot

@Composable
fun HomeScreen(
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  isAdmin: Boolean,
  isSeedingCatalog: Boolean,
  onOpenExplore: () -> Unit,
  onOpenCategory: (String) -> Unit,
  onOpenCareerHub: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenAdmin: () -> Unit,
  onOpenAcademy: (String) -> Unit,
  onOpenCourse: (String) -> Unit,
  onResumeLesson: (courseId: String, lessonId: String) -> Unit,
  onOpenLearningPath: (String) -> Unit,
  onToggleSaveCourse: (Course, Boolean) -> Unit,
  onSyncStarterCatalog: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }

  when {
    catalogState is UiState.Loading || isSeedingCatalog -> {
      LoadingStateView(
        message = if (isSeedingCatalog) "Initializing AI Academy & Courses..." else "Loading your learning hub..."
      )
      return
    }
    catalogState is UiState.Error -> {
      ErrorStateCard(message = catalogState.message)
      return
    }
  }

  val catalog = (catalogState as UiState.Success).data
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val hasSubscription = student.subscription?.activeEntitlement == true
  val unlockedCourseIds = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }.map { it.courseId }.toSet()
  }
  val savedIds = student.savedCourses.map { it.courseId }.toSet()
  val enrollmentByCourse = student.enrollments.associateBy { it.courseId }
  val unreadNotifications = student.notifications.count { !it.read }

  val publishedCourses = remember(catalog.courses) {
    catalog.courses.filter { it.published }
  }
  val publishedCoursesById = remember(publishedCourses) {
    publishedCourses.associateBy { it.id }
  }
  val publishedAcademies = remember(catalog.academies) {
    catalog.academies.filter { it.published }
  }
  val academyColorMap = remember(publishedAcademies) {
    publishedAcademies.associate { it.id to parseAccentColor(it.accentHex, ElectricBlue) }
  }
  val activeCategories = remember(publishedCourses) {
    CategoryDefinitions.nonEmptyCategoriesForCourses(publishedCourses)
  }

  val activeEnrollments = remember(student.enrollments, publishedCoursesById) {
    student.enrollments.filter {
      publishedCoursesById.containsKey(it.courseId) &&
        (it.status == "ACTIVE" || it.progressPercent < 100L)
    }
  }
  val featuredCourses = remember(publishedCourses) {
    publishedCourses.filter { it.featured }.take(10)
  }
  val popularCourses = remember(publishedCourses) {
    publishedCourses.filter { it.enrollmentCount > 0L }
      .sortedByDescending { it.enrollmentCount }
      .take(10)
  }
  val freeCourses = remember(publishedCourses) {
    publishedCourses.filter { !it.premiumOnly }.take(10)
  }
  val recommendedCourses = remember(publishedCourses, student.profile, student.enrollments) {
    val interests = (student.profile?.careerInterests.orEmpty() + student.profile?.learningInterests.orEmpty())
      .map { it.lowercase() }
    val completedIds = student.enrollments.filter { it.status == "COMPLETED" }.map { it.courseId }.toSet()
    val goalWords = student.profile?.learningGoal?.lowercase()?.split(" ")?.filter { it.length > 3 }.orEmpty()
    publishedCourses
      .filter { !completedIds.contains(it.id) }
      .sortedByDescending { course ->
        val haystack = "${course.title} ${course.academyName} ${course.tags.joinToString(" ")}".lowercase()
        var score = 0
        interests.forEach { if (haystack.contains(it)) score += 3 }
        goalWords.forEach { if (haystack.contains(it)) score += 1 }
        if (course.featured) score += 1
        score
      }
      .take(8)
  }

  val searchMatches = remember(searchQuery, publishedCourses, publishedAcademies) {
    val q = searchQuery.trim().lowercase()
    if (q.isBlank()) null
    else {
      val matchingAcademies = publishedAcademies.filter {
        it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
      }
      val matchingCourses = publishedCourses.filter {
        it.title.lowercase().contains(q) ||
          it.academyName.lowercase().contains(q) ||
          it.instructorName.lowercase().contains(q) ||
          it.tags.any { tag -> tag.lowercase().contains(q) } ||
          it.description.lowercase().contains(q)
      }
      matchingAcademies to matchingCourses
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("home_screen"),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // 1. Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(NavyDeep),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.School,
              contentDescription = "Datalabs Learning Hub",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Welcome back, ${student.profile?.fullName?.substringBefore(" ") ?: "Learner"}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Datalabs Learning Hub",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (isAdmin) {
            IconButton(
              onClick = onOpenAdmin,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("home_admin_button")
            ) {
              Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = "Admin Console",
                tint = ElectricBlue
              )
            }
          }
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("home_notifications_button")
          ) {
            BadgedBox(
              badge = {
                if (unreadNotifications > 0) {
                  Badge { Text("$unreadNotifications") }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications"
              )
            }
          }
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(ElectricBlue.copy(alpha = 0.14f))
              .clickable(onClick = onOpenProfile)
              .testTag("home_profile_avatar"),
            contentAlignment = Alignment.Center
          ) {
            val photoUrl = student.profile?.photoUrl
            if (!photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = photoUrl,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Text(
                text = (student.profile?.fullName?.firstOrNull() ?: 'D').uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 2. Search Bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .testTag("home_search_input"),
        placeholder = { Text("Search AI courses, skills, topics, instructors...") },
        leadingIcon = {
          Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )
    }

    when {
      // If user is searching right on Home, show instant filtered results
      searchMatches != null -> {
        val (matchedAcademies, matchedCourses) = searchMatches
        item {
          Text(
            text = "Search Results (${matchedCourses.size} courses, ${matchedAcademies.size} academies)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
        if (matchedAcademies.isNotEmpty()) {
          item {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(matchedAcademies, key = { it.id }) { academy ->
                val count = publishedCourses.count { it.academyId == academy.id }
                AcademyCard(
                  academy = academy,
                  actualCourseCount = count,
                  onClick = { onOpenAcademy(academy.id) },
                  modifier = Modifier.width(240.dp)
                )
              }
            }
          }
        }
        if (matchedCourses.isEmpty()) {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              EmptyStateCard(
                icon = Icons.Filled.Search,
                title = "No matching courses",
                description = "Try another keyword or open Explore to filter by academy, skill level, and tier.",
                actionLabel = "Open Explore",
                onAction = onOpenExplore
              )
            }
          }
        } else {
          items(matchedCourses.take(25), key = { it.id }) { course ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              CourseCard(
                course = course,
                accentColor = academyColorMap[course.academyId] ?: ElectricBlue,
                isSaved = savedIds.contains(course.id),
                hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
                progressPercent = enrollmentByCourse[course.id]?.progressPercent,
                onClick = { onOpenCourse(course.id) },
                onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) }
              )
            }
          }
        }
      }

      // Empty catalog fallback if admin hasn't initialized yet
      publishedAcademies.isEmpty() -> {
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            EmptyStateCard(
              icon = Icons.Filled.School,
              title = "Learning Catalogue Ready to Initialize",
              description = if (isAdmin) {
                "Tap below to populate AI Academy, its 5 complete courses, learning path, and assessments into Firestore."
              } else {
                "Your platform administrator has not initialized AI Academy yet. Please check back shortly."
              },
              actionLabel = if (isAdmin) "Initialize Catalogue" else null,
              onAction = if (isAdmin) onSyncStarterCatalog else null
            )
          }
        }
      }

      else -> {
        // 3. Hero Section
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDeep)
      ) {
        Column {
          AsyncImage(
            model = R.drawable.img_hero_learning,
            contentDescription = "Datalabs Learning Hub Hero",
            modifier = Modifier
              .fillMaxWidth()
              .height(145.dp),
            contentScale = ContentScale.Crop
          )
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Your future is built by what you learn.",
              style = MaterialTheme.typography.displayMedium,
              color = Color.White
            )
            Text(
              text = "Explore practical skills that help you build a career, start a business, earn online and navigate the modern world.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFCBD5E1)
            )
            Row(
              modifier = Modifier.padding(top = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = onOpenExplore,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ElectricBlue,
                  contentColor = Color.White
                ),
                modifier = Modifier.testTag("hero_explore_button")
              ) {
                Text("Explore Courses")
              }
              val latest = activeEnrollments.firstOrNull()
              OutlinedButton(
                onClick = {
                  if (latest != null && !latest.lastLessonId.isNullOrBlank()) {
                    onResumeLesson(latest.courseId, latest.lastLessonId)
                  } else if (latest != null) {
                    onOpenCourse(latest.courseId)
                  } else {
                    onOpenExplore()
                  }
                },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.testTag("hero_continue_button")
              ) {
                Text("Continue Learning")
              }
            }
          }
        }
      }
    }

    // 4. Continue Learning
    if (activeEnrollments.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Continue Learning",
          actionLabel = "My Learning",
          onAction = null
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(activeEnrollments, key = { it.id }) { enrollment ->
            ContinueLearningCard(
              enrollment = enrollment,
              onContinue = {
                val lid = enrollment.lastLessonId
                if (!lid.isNullOrBlank()) {
                  onResumeLesson(enrollment.courseId, lid)
                } else {
                  onOpenCourse(enrollment.courseId)
                }
              }
            )
          }
        }
      }
    }

    // 4b. Main Active Academy Spotlight (AI Academy Guided Flagship Card)
    if (publishedAcademies.isNotEmpty()) {
      item {
        if (publishedAcademies.size == 1) {
          val mainAcademy = publishedAcademies.first()
          val academyCourses = publishedCourses.filter { it.academyId == mainAcademy.id }
          val mastery = remember(academyCourses, student.enrollments, student.certificates) {
            AcademyProgressionHelper.computeMastery(
              courses = academyCourses,
              enrollments = student.enrollments,
              certificates = student.certificates
            )
          }
          val accent = parseAccentColor(mainAcademy.accentHex, ElectricBlue)
          SectionHeader(
            title = "AI Academy Roadmap",
            actionLabel = "View 5 Stages",
            onAction = { onOpenAcademy(mainAcademy.id) }
          )
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clip(RoundedCornerShape(18.dp))
              .clickable { onOpenAcademy(mainAcademy.id) }
              .testTag("academy_card_${mainAcademy.id}"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
          ) {
            Column {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp)
                  .background(accent)
              )
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = academyIconFor(mainAcademy.iconKey),
                        contentDescription = mainAcademy.name,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = mainAcademy.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = "${mastery.completedCoursesCount}/${mastery.totalCourses} courses mastered • ${mastery.overallProgressPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mastery.allRequiredCoursesCompleted) SuccessEmerald else accent,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                  Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                  ) {
                    Text(
                      text = "${academyCourses.size} stages",
                      style = MaterialTheme.typography.labelMedium,
                      color = accent,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }

                LinearProgressIndicator(
                  progress = { mastery.overallProgressPercent / 100f },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  color = if (mastery.allRequiredCoursesCompleted) SuccessEmerald else accent
                )

                Text(
                  text = mainAcademy.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )

                val nextNode = mastery.nextRecommendedNode
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = when {
                      mastery.allRequiredCoursesCompleted -> "Specialization Certificate unlocked"
                      nextNode != null -> "Next: Stage ${nextNode.stageNumber} • ${nextNode.course.title}"
                      else -> "Start Stage 1 (Free)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = "Open Roadmap",
                      style = MaterialTheme.typography.labelLarge,
                      color = accent,
                      fontWeight = FontWeight.Bold
                    )
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = null,
                      tint = accent,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        } else {
          SectionHeader(
            title = "Explore Academies (${publishedAcademies.size})",
            actionLabel = "Browse all",
            onAction = onOpenExplore
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(publishedAcademies, key = { it.id }) { academy ->
              val count = publishedCourses.count { it.academyId == academy.id }
              AcademyCard(
                academy = academy,
                actualCourseCount = count,
                onClick = { onOpenAcademy(academy.id) },
                modifier = Modifier.width(248.dp)
              )
            }
          }
        }
      }
    }

    // 4c. Career & Employment Readiness Hub Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onOpenCareerHub)
          .testTag("home_career_hub_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(ElectricBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Filled.BusinessCenter, contentDescription = null, tint = ElectricBlue)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Career & Employment Readiness Hub",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Apply AI skills to CVs, LinkedIn, STAR interviews, portfolios & freelance playbooks.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Career Hub", tint = ElectricBlue)
        }
      }
    }

    // 5. Active Skill Tracks
    if (activeCategories.isNotEmpty()) {
      item {
        SectionHeader(
          title = "AI Skill Tracks (${activeCategories.size})",
          actionLabel = "All Filters",
          onAction = onOpenExplore
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(activeCategories, key = { it.id }) { cat ->
            val count = CategoryDefinitions.coursesForCategory(publishedCourses, cat.name).size
            val catColor = parseAccentColor(cat.accentHex, ElectricBlue)
            Card(
              modifier = Modifier
                .width(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenCategory(cat.name) }
                .testTag("home_cat_${cat.id}"),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, catColor.copy(alpha = 0.35f))
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(color = catColor.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                      text = "$count courses",
                      style = MaterialTheme.typography.labelSmall,
                      color = catColor,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
                Text(cat.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  text = cat.shortDescription,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }

    // 6. Featured Courses
    if (featuredCourses.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Featured Courses",
          actionLabel = "See all",
          onAction = onOpenExplore
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(featuredCourses, key = { it.id }) { course ->
            CourseCard(
              course = course,
              accentColor = academyColorMap[course.academyId] ?: ElectricBlue,
              isSaved = savedIds.contains(course.id),
              hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
              progressPercent = enrollmentByCourse[course.id]?.progressPercent,
              onClick = { onOpenCourse(course.id) },
              onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) },
              modifier = Modifier.width(300.dp)
            )
          }
        }
      }
    }

    // 7. Popular Courses (ONLY rendered when real engagement exists — zero fake stats)
    if (popularCourses.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Popular Courses",
          actionLabel = "Explore",
          onAction = onOpenExplore
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(popularCourses, key = { it.id }) { course ->
            CourseCard(
              course = course,
              accentColor = academyColorMap[course.academyId] ?: ElectricBlue,
              isSaved = savedIds.contains(course.id),
              hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
              progressPercent = enrollmentByCourse[course.id]?.progressPercent,
              onClick = { onOpenCourse(course.id) },
              onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) },
              modifier = Modifier.width(300.dp)
            )
          }
        }
      }
    }

    // 8. Structured Learning Paths
    if (catalog.learningPaths.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Structured Learning Paths",
          actionLabel = null,
          onAction = null
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(catalog.learningPaths, key = { it.id }) { path ->
            val progress = student.pathProgress.firstOrNull { it.pathId == path.id }?.progressPercent ?: 0L
            LearningPathCard(
              path = path,
              progressPercent = progress,
              onClick = { onOpenLearningPath(path.id) },
              modifier = Modifier.width(280.dp)
            )
          }
        }
      }
    }

    // 9. Free Learning
    if (freeCourses.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Free Learning",
          actionLabel = "All Free",
          onAction = onOpenExplore
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(freeCourses, key = { it.id }) { course ->
            CourseCard(
              course = course,
              accentColor = academyColorMap[course.academyId] ?: ElectricBlue,
              isSaved = savedIds.contains(course.id),
              hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
              progressPercent = enrollmentByCourse[course.id]?.progressPercent,
              onClick = { onOpenCourse(course.id) },
              onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) },
              modifier = Modifier.width(290.dp)
            )
          }
        }
      }
    }

    // 10. Recommended for You
    if (recommendedCourses.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Recommended for You",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
          )
        }
      }
      items(recommendedCourses, key = { it.id }) { course ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          CourseCard(
            course = course,
            accentColor = academyColorMap[course.academyId] ?: ElectricBlue,
            isSaved = savedIds.contains(course.id),
            hasSubscription = hasSubscription || unlockedCourseIds.contains(course.id),
            progressPercent = enrollmentByCourse[course.id]?.progressPercent,
            onClick = { onOpenCourse(course.id) },
            onToggleSave = { onToggleSaveCourse(course, savedIds.contains(course.id)) }
          )
        }
      }
    }
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  actionLabel: String?,
  onAction: (() -> Unit)?
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 8.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
private fun ContinueLearningCard(
  enrollment: Enrollment,
  onContinue: () -> Unit
) {
  Card(
    modifier = Modifier
      .width(280.dp)
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onContinue),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
        Surface(
          color = ElectricBlue.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${enrollment.progressPercent}% COMPLETE",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Text(
          text = "${enrollment.completedLessonsCount}/${enrollment.totalLessonsCount} lessons",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = enrollment.courseTitle,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "Last: ${enrollment.lastLessonTitle ?: "Lesson 1"}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      LinearProgressIndicator(
        progress = { (enrollment.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = ElectricBlue
      )
      Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Continue")
      }
    }
  }
}
