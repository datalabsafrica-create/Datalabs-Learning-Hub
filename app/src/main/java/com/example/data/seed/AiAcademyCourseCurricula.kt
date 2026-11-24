package com.example.data.seed

/**
 * Dispatcher for the 5 flagship AI Academy courses (Stages 1–5).
 * Every lesson (l1..l4) provides full pedagogical explanations, multiple real-world examples,
 * reference comparison tables, structured 4-part case studies, and 5-slide concept decks.
 */
object AiAcademyCourseCurricula {

  fun buildIfAiCourse(
    academy: SeedAcademySpec,
    courseTitle: String,
    slug: String,
    isFree: Boolean,
    level: String
  ): GeneratedCourseCurriculum? {
    if (academy.id != AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID) return null
    val primaryCategory = CategoryDefinitions.resolvePrimaryCategory(academy.id, courseTitle)
    val categories = CategoryDefinitions.resolveCategories(academy.id, courseTitle)

    return when (courseTitle) {
      "Artificial Intelligence for Beginners" -> AiAcademyCurriculaFoundations.buildBeginnersCurriculum(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level,
        primaryCategory = primaryCategory,
        categories = categories
      )
      "AI for Productivity" -> AiAcademyCurriculaFoundations.buildProductivityCurriculum(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level,
        primaryCategory = primaryCategory,
        categories = categories
      )
      "Prompt Engineering" -> AiAcademyCurriculaApplied.buildPromptEngineeringCurriculum(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level,
        primaryCategory = primaryCategory,
        categories = categories
      )
      "AI for Business and Entrepreneurs" -> AiAcademyCurriculaApplied.buildBusinessAiCurriculum(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level,
        primaryCategory = primaryCategory,
        categories = categories
      )
      "Building Apps with AI" -> AiAcademyCurriculaApplied.buildAppBuilderCurriculum(
        academy = academy,
        courseTitle = courseTitle,
        slug = slug,
        isFree = isFree,
        level = level,
        primaryCategory = primaryCategory,
        categories = categories
      )
      else -> null
    }
  }
}
