package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.Academy
import com.example.data.model.Course
import com.example.data.model.LearningPath
import com.example.data.model.UiState
import com.example.data.seed.AcademyCatalogDefinitions
import com.example.data.seed.CourseContentBlueprint
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.DatalabsTheme
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.StudentDataSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenRenderTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `home screen renders AI Academy and all 5 courses without crashing`() {
    val academyDef = AcademyCatalogDefinitions.academies.single()
    val academy = Academy(
      id = academyDef.id,
      name = academyDef.name,
      shortName = academyDef.shortName,
      description = academyDef.description,
      iconKey = academyDef.iconKey,
      accentHex = academyDef.accentHex,
      displayOrder = academyDef.displayOrder,
      courseCount = academyDef.courses.size.toLong(),
      published = true
    )
    val courses = academyDef.courses.mapIndexed { idx, title ->
      val slug = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(48)
      val courseId = "${academy.id.removePrefix("academy_")}_$slug"
      val isFree = idx < 3 || title.contains("for Beginners", ignoreCase = true)
      val level = when {
        title.contains("Beginner", ignoreCase = true) || idx <= 1 -> "BEGINNER"
        title.contains("Building Apps", ignoreCase = true) -> "ADVANCED"
        else -> "INTERMEDIATE"
      }
      val curriculum = CourseContentBlueprint.buildForCourse(academyDef, title, slug, isFree, level)
      Course(
        id = courseId,
        academyId = academy.id,
        academyName = academy.name,
        title = title,
        subtitle = curriculum.subtitle,
        description = curriculum.description,
        instructorId = academyDef.instructorId,
        instructorName = academyDef.instructorName,
        level = level,
        durationMinutes = curriculum.durationMinutes,
        lessonCount = curriculum.lessonCount,
        moduleCount = 4L,
        accessTier = if (isFree) "FREE" else "PREMIUM",
        published = true,
        featured = idx < 2,
        category = curriculum.primaryCategory,
        categories = curriculum.categories
      )
    }
    val snapshot = HubCatalogSnapshot(
      academies = listOf(academy),
      courses = courses,
      learningPaths = listOf(
        LearningPath(
          id = "path_ai_professional",
          title = "Become an AI-Powered Professional",
          description = "5-stage AI Academy path",
          academyId = academy.id,
          targetRole = "AI-Powered Professional",
          courseIds = courses.map { it.id },
          courseTitles = courses.map { it.title },
          published = true
        )
      )
    )

    composeTestRule.setContent {
      DatalabsTheme {
        HomeScreen(
          catalogState = UiState.Success(snapshot),
          studentState = UiState.Success(StudentDataSnapshot()),
          isAdmin = true,
          isSeedingCatalog = false,
          onOpenExplore = {},
          onOpenCategory = {},
          onOpenCareerHub = {},
          onOpenNotifications = {},
          onOpenProfile = {},
          onOpenAdmin = {},
          onOpenAcademy = {},
          onOpenCourse = {},
          onResumeLesson = { _, _ -> },
          onOpenLearningPath = {},
          onToggleSaveCourse = { _, _ -> },
          onSyncStarterCatalog = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hero_explore_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("academy_card_academy_ai"))
    composeTestRule.onNodeWithTag("academy_card_academy_ai").assertIsDisplayed()
  }
}
