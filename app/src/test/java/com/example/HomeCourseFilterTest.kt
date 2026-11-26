package com.example

import com.example.data.model.Course
import com.example.ui.components.HomeCourseFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCourseFilterTest {

  private val sampleFirebaseCourses = listOf(
    Course(
      id = "course_1",
      academyId = "academy_ai",
      academyName = "AI Academy",
      title = "AI Foundations & Literacy",
      level = "BEGINNER",
      category = "Artificial Intelligence",
      categories = listOf("Artificial Intelligence", "Technology & Productivity"),
      tags = listOf("ai", "literacy")
    ),
    Course(
      id = "course_2",
      academyId = "academy_ai",
      academyName = "AI Academy",
      title = "Prompt Engineering Mastery",
      level = "INTERMEDIATE",
      category = "Artificial Intelligence",
      categories = listOf("Artificial Intelligence"),
      tags = listOf("prompts")
    ),
    Course(
      id = "course_3",
      academyId = "academy_ai",
      academyName = "AI Academy",
      title = "AI Automation for Business Teams",
      level = "ADVANCED",
      category = "AI for Business",
      categories = listOf("AI for Business", "Entrepreneurship"),
      tags = listOf("automation", "workflows")
    )
  )

  @Test
  fun searchQueryMatchesCategoryNameFromFirebaseCourses() {
    val results = HomeCourseFilter.filterCourses(
      courses = sampleFirebaseCourses,
      query = "AI for Business",
      selectedCategory = null,
      selectedLevel = null
    )
    assertEquals(listOf("course_3"), results.map { it.id })
  }

  @Test
  fun searchQueryMatchesSkillLevelFromFirebaseCourses() {
    val results = HomeCourseFilter.filterCourses(
      courses = sampleFirebaseCourses,
      query = "intermediate",
      selectedCategory = null,
      selectedLevel = null
    )
    assertEquals(listOf("course_2"), results.map { it.id })
  }

  @Test
  fun explicitCategoryAndSkillLevelChipsFilterAccurately() {
    val byCategory = HomeCourseFilter.filterCourses(
      courses = sampleFirebaseCourses,
      query = "",
      selectedCategory = "Artificial Intelligence",
      selectedLevel = null
    )
    assertEquals(listOf("course_1", "course_2"), byCategory.map { it.id })

    val byCategoryAndLevel = HomeCourseFilter.filterCourses(
      courses = sampleFirebaseCourses,
      query = "",
      selectedCategory = "Artificial Intelligence",
      selectedLevel = "BEGINNER"
    )
    assertEquals(listOf("course_1"), byCategoryAndLevel.map { it.id })
  }

  @Test
  fun extractCategoriesAndSkillLevelsPreservesCanonicalOrderAndFirebaseValues() {
    val levels = HomeCourseFilter.extractSkillLevels(sampleFirebaseCourses)
    assertEquals(listOf("BEGINNER", "INTERMEDIATE", "ADVANCED"), levels)

    val categories = HomeCourseFilter.extractCategories(sampleFirebaseCourses)
    assertTrue(categories.contains("Artificial Intelligence"))
    assertTrue(categories.contains("AI for Business"))
    assertTrue(categories.contains("Entrepreneurship"))
  }
}
