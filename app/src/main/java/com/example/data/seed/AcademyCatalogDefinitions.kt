package com.example.data.seed

import com.example.data.model.Certificate
import com.example.data.model.Course
import com.example.data.model.Enrollment

data class SeedAcademySpec(
  val id: String,
  val name: String,
  val shortName: String,
  val description: String,
  val iconKey: String,
  val accentHex: String,
  val displayOrder: Long,
  val instructorId: String,
  val instructorName: String,
  val courses: List<String>
)

data class SeedInstructorSpec(
  val id: String,
  val fullName: String,
  val title: String,
  val bio: String,
  val specialty: String
)

data class AcademyStageMeta(
  val courseId: String,
  val stageNumber: Int,
  val trackLabel: String,
  val outcomeTag: String,
  val prerequisiteCourseId: String?,
  val prerequisiteTitle: String?
)

data class CourseProgressionNode(
  val course: Course,
  val stageNumber: Int,
  val trackLabel: String,
  val outcomeTag: String,
  val enrollment: Enrollment?,
  val isCompleted: Boolean,
  val progressPercent: Int,
  val prerequisiteTitle: String?,
  val prerequisiteMet: Boolean,
  val isNextRecommended: Boolean
)

data class AcademyMasterySummary(
  val nodes: List<CourseProgressionNode> = emptyList(),
  val totalCourses: Int = 0,
  val completedCoursesCount: Int = 0,
  val inProgressCoursesCount: Int = 0,
  val overallProgressPercent: Int = 0,
  val nextRecommendedNode: CourseProgressionNode? = null,
  val allRequiredCoursesCompleted: Boolean = false,
  val specializationCertificate: Certificate? = null
)

object AcademyCatalogDefinitions {

  const val ACTIVE_ACADEMY_ID = "academy_ai"
  const val CURRICULUM_VERSION = 4L
  const val AI_SPECIALIZATION_COURSE_ID = "spec_academy_ai"
  const val AI_SPECIALIZATION_TITLE = "AI Academy Specialization: Practical AI & App Engineering"

  val aiAcademyStages = listOf(
    AcademyStageMeta(
      courseId = "ai_artificial_intelligence_for_beginners",
      stageNumber = 1,
      trackLabel = "Stage 1 • Foundation",
      outcomeTag = "Understand how LLMs & AI systems work",
      prerequisiteCourseId = null,
      prerequisiteTitle = null
    ),
    AcademyStageMeta(
      courseId = "ai_ai_for_productivity",
      stageNumber = 2,
      trackLabel = "Stage 2 • Workplace",
      outcomeTag = "Save 5+ hours/week with daily AI workflows",
      prerequisiteCourseId = "ai_artificial_intelligence_for_beginners",
      prerequisiteTitle = "Stage 1: Artificial Intelligence for Beginners"
    ),
    AcademyStageMeta(
      courseId = "ai_prompt_engineering",
      stageNumber = 3,
      trackLabel = "Stage 3 • Precision",
      outcomeTag = "Engineer reliable structured prompts & specs",
      prerequisiteCourseId = "ai_ai_for_productivity",
      prerequisiteTitle = "Stage 2: AI for Productivity"
    ),
    AcademyStageMeta(
      courseId = "ai_ai_for_business_and_entrepreneurs",
      stageNumber = 4,
      trackLabel = "Stage 4 • Strategy & ROI",
      outcomeTag = "Deploy automated AI pipelines for business",
      prerequisiteCourseId = "ai_prompt_engineering",
      prerequisiteTitle = "Stage 3: Prompt Engineering"
    ),
    AcademyStageMeta(
      courseId = "ai_building_apps_with_ai",
      stageNumber = 5,
      trackLabel = "Stage 5 • Builder Capstone",
      outcomeTag = "Ship full AI-assisted software applications",
      prerequisiteCourseId = "ai_ai_for_business_and_entrepreneurs",
      prerequisiteTitle = "Stage 4: AI for Business and Entrepreneurs"
    )
  )

  val requiredSpecializationCourseIds: List<String> = aiAcademyStages.map { it.courseId }

  val instructors = listOf(
    SeedInstructorSpec(
      id = "inst_daniel_kato",
      fullName = "Daniel Kato",
      title = "AI Systems & Automation Architect",
      bio = "Builds applied AI workflows, prompt engineering systems, and AI-powered applications for modern teams and entrepreneurs.",
      specialty = "Artificial Intelligence & App Engineering"
    )
  )

  val academies = listOf(
    SeedAcademySpec(
      id = ACTIVE_ACADEMY_ID,
      name = "AI Academy",
      shortName = "AI Academy",
      description = "Master practical artificial intelligence, everyday productivity, prompt engineering, business automation, and AI-assisted app building.",
      iconKey = "ai",
      accentHex = "#2563EB",
      displayOrder = 1L,
      instructorId = "inst_daniel_kato",
      instructorName = "Daniel Kato",
      courses = listOf(
        "Artificial Intelligence for Beginners",
        "AI for Productivity",
        "Prompt Engineering",
        "AI for Business and Entrepreneurs",
        "Building Apps with AI"
      )
    )
  )
}

object AcademyProgressionHelper {

  fun computeMastery(
    courses: List<Course>,
    enrollments: List<Enrollment>,
    certificates: List<Certificate>
  ): AcademyMasterySummary {
    val stageById = AcademyCatalogDefinitions.aiAcademyStages.associateBy { it.courseId }
    val enrollmentByCourseId = enrollments.associateBy { it.courseId }
    val completedCourseIds = enrollments
      .filter { it.status == "COMPLETED" || it.progressPercent >= 100L }
      .map { it.courseId }
      .toSet()

    val orderedCourses = courses.sortedWith(
      compareBy<Course>(
        { stageById[it.id]?.stageNumber ?: 99 },
        { it.title }
      )
    )

    // Pick next recommended course:
    // 1) Active in-progress course (most recently updated or earliest in roadmap)
    // 2) First incomplete course in stage order
    val activeIncomplete = orderedCourses.firstOrNull { course ->
      val enr = enrollmentByCourseId[course.id]
      enr != null && !completedCourseIds.contains(course.id)
    }
    val firstIncomplete = activeIncomplete ?: orderedCourses.firstOrNull { course ->
      !completedCourseIds.contains(course.id)
    }

    val nodes = orderedCourses.mapIndexed { index, course ->
      val meta = stageById[course.id]
      val enr = enrollmentByCourseId[course.id]
      val isCompleted = completedCourseIds.contains(course.id)
      val pct = when {
        isCompleted -> 100
        enr != null -> enr.progressPercent.toInt().coerceIn(0, 99)
        else -> 0
      }
      val prereqMet = meta?.prerequisiteCourseId == null || completedCourseIds.contains(meta.prerequisiteCourseId)
      CourseProgressionNode(
        course = course,
        stageNumber = meta?.stageNumber ?: (index + 1),
        trackLabel = meta?.trackLabel ?: "Stage ${index + 1} • Applied AI",
        outcomeTag = meta?.outcomeTag ?: course.subtitle,
        enrollment = enr,
        isCompleted = isCompleted,
        progressPercent = pct,
        prerequisiteTitle = meta?.prerequisiteTitle,
        prerequisiteMet = prereqMet,
        isNextRecommended = firstIncomplete?.id == course.id
      )
    }

    val total = nodes.size
    val completedCount = nodes.count { it.isCompleted }
    val inProgressCount = nodes.count { !it.isCompleted && it.enrollment != null }
    val overallPct = if (total == 0) 0 else (nodes.sumOf { it.progressPercent } / total).coerceIn(0, 100)
    val allRequiredDone = AcademyCatalogDefinitions.requiredSpecializationCourseIds.all {
      completedCourseIds.contains(it)
    }
    val specCert = certificates.firstOrNull {
      it.courseId == AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID
    }

    return AcademyMasterySummary(
      nodes = nodes,
      totalCourses = total,
      completedCoursesCount = completedCount,
      inProgressCoursesCount = inProgressCount,
      overallProgressPercent = overallPct,
      nextRecommendedNode = nodes.firstOrNull { it.isNextRecommended },
      allRequiredCoursesCompleted = allRequiredDone,
      specializationCertificate = specCert
    )
  }
}
