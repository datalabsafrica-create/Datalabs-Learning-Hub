package com.example

import com.example.data.model.AchievementsCalculator
import com.example.data.model.Certificate
import com.example.data.model.Enrollment
import com.example.data.model.LessonProgress
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile
import com.example.data.seed.AcademyCatalogDefinitions
import com.example.data.seed.CategoryDefinitions
import com.example.data.seed.CourseContentBlueprint
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentBlueprintTest {

  @Test
  fun `every course in AI Academy has complete modules downloads quizzes and assignments`() {
    assertEquals(1, AcademyCatalogDefinitions.academies.size)
    assertEquals(AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID, AcademyCatalogDefinitions.academies.first().id)
    assertTrue(CategoryDefinitions.categories.isNotEmpty())
    var totalCourses = 0
    val resolvedPrimaryCategories = mutableSetOf<String>()
    val allSubtitles = mutableListOf<String>()
    val allLessonTitles = mutableListOf<String>()
    val allCaseStudies = mutableListOf<String>()
    val allAssignmentTitles = mutableListOf<String>()
    val allDownloadTitles = mutableListOf<String>()
    val allModuleQuizFirstPrompts = mutableListOf<String>()
    val allFinalQuizFirstPrompts = mutableListOf<String>()

    for (academy in AcademyCatalogDefinitions.academies) {
      academy.courses.forEachIndexed { idx, courseTitle ->
        totalCourses++
        val slug = courseTitle.lowercase()
          .replace(Regex("[^a-z0-9]+"), "_")
          .trim('_')
          .take(48)
        val isFree = idx < 3 || courseTitle.contains("for Beginners", ignoreCase = true)
        val level = when {
          courseTitle.contains("Beginner", ignoreCase = true) || idx <= 1 -> "BEGINNER"
          courseTitle.contains("Building Apps", ignoreCase = true) ||
            courseTitle.contains("Advanced", ignoreCase = true) ||
            courseTitle.contains("Masterclass", ignoreCase = true) ||
            courseTitle.contains("Accelerator", ignoreCase = true) -> "ADVANCED"
          else -> "INTERMEDIATE"
        }

        val curriculum = CourseContentBlueprint.buildForCourse(
          academy = academy,
          courseTitle = courseTitle,
          slug = slug,
          isFree = isFree,
          level = level
        )

        resolvedPrimaryCategories.add(curriculum.primaryCategory)
        allSubtitles.add(curriculum.subtitle)
        allAssignmentTitles.add(curriculum.assignmentTitle)

        assertTrue(curriculum.primaryCategory.isNotBlank())
        assertTrue(curriculum.categories.isNotEmpty())
        assertTrue(courseTitle.length in 2..160)
        assertTrue(curriculum.subtitle.length in 2..240)
        assertTrue(curriculum.description.length in 4..4000)
        assertEquals(4, curriculum.lessons.size)
        assertEquals(4L, curriculum.lessonCount)
        assertEquals(listOf("l1", "l2", "l3", "l4"), curriculum.lessons.map { it.suffix })
        assertTrue(curriculum.lessons.first().freePreview)

        curriculum.lessons.forEach { lesson ->
          allLessonTitles.add(lesson.title)
          allDownloadTitles.add(lesson.downloadTitle)
          allCaseStudies.add(lesson.caseStudyText)
          assertTrue(lesson.moduleTitle.isNotBlank())
          assertTrue(lesson.title.length in 2..160)
          assertTrue(lesson.summary.length in 2..600)
          assertTrue(
            "Expected instructional format WRITTEN or SLIDES on ${lesson.title}, got ${lesson.format}",
            lesson.format in setOf("WRITTEN", "SLIDES")
          )
          assertTrue(
            "Expected rich educational notes in ${lesson.title} (${lesson.notesMarkdown.length} chars)",
            lesson.notesMarkdown.length in 1000..50000
          )
          assertTrue(lesson.slidesText.lines().size >= 5)
          assertTrue(lesson.downloadTitle.isNotBlank())
          assertTrue(lesson.downloadContent.isNotBlank())
          assertTrue(lesson.exercisePrompt.isNotBlank())
          assertTrue(lesson.keyTakeaways.isNotEmpty())
          assertTrue(lesson.caseStudyText.isNotBlank())
        }
        assertTrue(curriculum.lessons.any { it.notesMarkdown.contains("|---") })

        // Module Checkpoint Quiz
        assertTrue(curriculum.moduleQuizTitle.length in 2..160)
        val moduleQuestions = Json.decodeFromString<List<QuizQuestion>>(curriculum.moduleQuizQuestionsJson)
        assertTrue(moduleQuestions.size >= 3)
        assertEquals(100, moduleQuestions.sumOf { it.points })
        assertTrue(moduleQuestions.all { it.explanation.isNotBlank() })
        assertTrue(moduleQuestions.any { it.type == "TRUE_FALSE" })
        allModuleQuizFirstPrompts.add(moduleQuestions.first().prompt)

        // Final Course Examination
        assertTrue(curriculum.quizTitle.length in 2..160)
        val questions = Json.decodeFromString<List<QuizQuestion>>(curriculum.quizQuestionsJson)
        assertTrue(questions.size >= 4)
        assertEquals(100, questions.sumOf { it.points })
        assertTrue(questions.all { it.explanation.isNotBlank() })
        assertTrue(questions.any { it.type == "TRUE_FALSE" })
        allFinalQuizFirstPrompts.add(questions.first().prompt)

        assertTrue(curriculum.assignmentTitle.length in 2..160)
        assertTrue(curriculum.assignmentInstructions.length in 10..5000)
      }
    }

    assertEquals(5, totalCourses)
    assertEquals(5, allSubtitles.distinct().size)
    assertEquals(20, allLessonTitles.distinct().size)
    assertEquals(20, allCaseStudies.distinct().size)
    assertEquals(5, allAssignmentTitles.distinct().size)
    assertEquals(20, allDownloadTitles.distinct().size)
    assertEquals(5, allModuleQuizFirstPrompts.distinct().size)
    assertEquals(5, allFinalQuizFirstPrompts.distinct().size)
    assertTrue(resolvedPrimaryCategories.isNotEmpty())
    assertTrue(
      AcademyCatalogDefinitions.academies.none { academy ->
        academy.courses.any { it.equals("Building Confidence", ignoreCase = true) }
      }
    )
  }

  @Test
  fun `achievements calculator unlocks earned milestones accurately`() {
    val badges = AchievementsCalculator.computeBadges(
      profile = UserProfile(streakDays = 4L),
      enrollments = listOf(
        Enrollment(id = "e1", courseId = "c1", academyId = "a1", status = "COMPLETED"),
        Enrollment(id = "e2", courseId = "c2", academyId = "a2", status = "ACTIVE")
      ),
      lessonProgress = listOf(
        LessonProgress(lessonId = "l1", completed = true),
        LessonProgress(lessonId = "l2", completed = true),
        LessonProgress(lessonId = "l3", completed = true),
        LessonProgress(lessonId = "l4", completed = true),
        LessonProgress(lessonId = "l5", completed = true)
      ),
      quizAttempts = listOf(
        QuizAttempt(quizId = "q1", passed = true, scorePercent = 90L)
      ),
      certificates = listOf(Certificate(id = "cert1"))
    )

    assertTrue(badges.first { it.id == "first_lesson" }.unlocked)
    assertTrue(badges.first { it.id == "lesson_streak_5" }.unlocked)
    assertTrue(badges.first { it.id == "quiz_master" }.unlocked)
    assertTrue(badges.first { it.id == "distinction_85" }.unlocked)
    assertTrue(badges.first { it.id == "streak_3" }.unlocked)
    assertTrue(badges.first { it.id == "course_graduate" }.unlocked)
    assertTrue(badges.first { it.id == "certified_pro" }.unlocked)
    assertFalse(badges.first { it.id == "multi_academy" }.unlocked)

    val explorerBadges = AchievementsCalculator.computeBadges(
      profile = UserProfile(streakDays = 4L),
      enrollments = listOf(
        Enrollment(id = "e1", courseId = "c1", academyId = "academy_ai", status = "COMPLETED"),
        Enrollment(id = "e2", courseId = "c2", academyId = "academy_ai", status = "ACTIVE"),
        Enrollment(id = "e3", courseId = "c3", academyId = "academy_ai", status = "ACTIVE")
      ),
      lessonProgress = emptyList(),
      quizAttempts = emptyList(),
      certificates = emptyList()
    )
    assertTrue(explorerBadges.first { it.id == "multi_academy" }.unlocked)
  }

  @Test
  fun `all 5 courses 20 lessons quizzes and assignments strictly satisfy firestore rules schema constraints`() {
    val allowedLevels = setOf("BEGINNER", "INTERMEDIATE", "ADVANCED")
    val allowedLessonFormats = setOf(
      "VIDEO", "WRITTEN", "AUDIO", "SLIDES", "PDF", "TEMPLATE", "EXERCISE", "ASSIGNMENT", "QUIZ", "PROJECT"
    )
    val validIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    val academy = AcademyCatalogDefinitions.academies.single()
    assertTrue(academy.id.matches(validIdRegex))
    assertTrue(academy.name.length in 2..120)
    assertTrue(academy.shortName.length in 2..40)
    assertTrue(academy.description.length in 4..600)
    assertEquals(5, academy.courses.size)

    academy.courses.forEachIndexed { idx, courseTitle ->
      val slug = courseTitle.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(48)
      val courseId = "${academy.id.removePrefix("academy_")}_$slug"
      assertTrue(courseId.length in 1..128 && courseId.matches(validIdRegex))

      val isFree = idx < 3 || courseTitle.contains("for Beginners", ignoreCase = true)
      val level = when {
        courseTitle.contains("Beginner", ignoreCase = true) || idx <= 1 -> "BEGINNER"
        courseTitle.contains("Building Apps", ignoreCase = true) ||
          courseTitle.contains("Advanced", ignoreCase = true) ||
          courseTitle.contains("Masterclass", ignoreCase = true) ||
          courseTitle.contains("Accelerator", ignoreCase = true) -> "ADVANCED"
        else -> "INTERMEDIATE"
      }
      assertTrue(level in allowedLevels)

      val curriculum = CourseContentBlueprint.buildForCourse(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level
      )

      // Course document parity with isValidCourse
      assertTrue(curriculum.durationMinutes in 5L..50000L)
      assertTrue(curriculum.lessonCount in 0L..500L)
      assertTrue(curriculum.whatYouWillLearn.size <= 15 && curriculum.whatYouWillLearn.all { it.length <= 220 })
      assertTrue(curriculum.prerequisites.size <= 10 && curriculum.prerequisites.all { it.length <= 220 })
      assertTrue(curriculum.formats.size <= 12 && curriculum.formats.all { it.length <= 40 })
      assertTrue(curriculum.tags.size <= 15 && curriculum.tags.all { it.length <= 60 })
      assertTrue(curriculum.primaryCategory.length <= 80)
      assertTrue(curriculum.categories.size <= 10 && curriculum.categories.all { it.length <= 80 })

      // Lessons + LessonContents parity with isValidLesson & isValidLessonContent
      assertEquals(4, curriculum.lessons.size)
      curriculum.lessons.forEach { lesson ->
        val lessonId = "${courseId}_${lesson.suffix}"
        assertTrue(lessonId.length in 1..128 && lessonId.matches(validIdRegex))
        assertTrue(lesson.moduleTitle.length <= 160)
        assertTrue(lesson.title.length in 2..160)
        assertTrue(lesson.summary.length in 2..600)
        assertTrue(lesson.orderIndex in 0L..500L)
        assertTrue(lesson.durationMinutes in 1L..600L)
        assertTrue(lesson.format in allowedLessonFormats)
        assertTrue(
          "Expected substantial teaching in ${lessonId} notesMarkdown (was ${lesson.notesMarkdown.length} chars)",
          lesson.notesMarkdown.length in 1000..50000
        )
        assertTrue(lesson.slidesText.length <= 20000)
        assertTrue(lesson.downloadTitle.length <= 200)
        assertTrue(lesson.downloadContent.length <= 30000)
        assertTrue(lesson.exercisePrompt.length <= 5000)
        assertTrue(lesson.keyTakeaways.size in 1..12 && lesson.keyTakeaways.all { it.length <= 300 })
        assertTrue(lesson.caseStudyText.length in 20..5000)
      }

      // Module Quiz + Final Quiz parity with isValidQuiz
      val moduleQuizId = "quiz_${courseId}_m2"
      val finalQuizId = "quiz_$courseId"
      assertTrue(moduleQuizId.matches(validIdRegex))
      assertTrue(finalQuizId.matches(validIdRegex))
      assertTrue(curriculum.moduleQuizTitle.length in 2..160)
      assertTrue(curriculum.moduleQuizDescription.length in 2..800)
      assertTrue(curriculum.moduleQuizQuestionsJson.length in 2..60000)
      assertTrue(curriculum.quizTitle.length in 2..160)
      assertTrue(curriculum.quizDescription.length in 2..800)
      assertTrue(curriculum.quizQuestionsJson.length in 2..60000)

      // Assignment parity with isValidAssignment
      val assignId = "assign_$courseId"
      assertTrue(assignId.matches(validIdRegex))
      assertTrue(curriculum.assignmentTitle.length in 2..160)
      assertTrue(curriculum.assignmentInstructions.length in 4..5000)
      assertTrue(curriculum.assignmentDeliverableFormat.length in 2..120)
    }
  }

  @Test
  fun `fallback blueprint for new AI Academy courses delivers rich teaching and distinct module case studies`() {
    val academy = AcademyCatalogDefinitions.academies.single()
    val customCurriculum = CourseContentBlueprint.buildForCourse(
      academy = academy,
      courseTitle = "Applied AI Research Workflows",
      slug = "applied_ai_research_workflows",
      isFree = false,
      level = "INTERMEDIATE"
    )
    assertEquals(4, customCurriculum.lessons.size)
    assertEquals(4, customCurriculum.lessons.map { it.caseStudyText }.distinct().size)
    customCurriculum.lessons.forEach { lesson ->
      assertTrue(lesson.format in setOf("WRITTEN", "SLIDES"))
      assertTrue(lesson.notesMarkdown.length in 1000..50000)
      assertTrue(lesson.slidesText.lines().size >= 5)
    }
  }
}
