package com.example.data.model

import com.example.data.seed.AcademyCatalogDefinitions
import com.example.data.seed.AcademyProgressionHelper
import com.example.ui.components.LessonNotesHelper

data class StageDeliverableSpec(
  val courseId: String,
  val stageNumber: Int,
  val deliverableTitle: String,
  val careerProofSummary: String,
  val targetPillarId: String
)

data class PortfolioDeliverableItem(
  val spec: StageDeliverableSpec,
  val course: Course?,
  val courseCompleted: Boolean,
  val progressPercent: Int,
  val labsLoggedCount: Int,
  val passedQuizCount: Int,
  val certificate: Certificate?
) {
  val statusLabel: String
    get() = when {
      certificate != null -> "Verified Proof"
      courseCompleted -> "Completed"
      labsLoggedCount > 0 || progressPercent > 0 -> "In Progress ($progressPercent%)"
      else -> "Not Started"
    }
}

data class CareerReadinessSnapshot(
  val deliverables: List<PortfolioDeliverableItem> = emptyList(),
  val completedCoursesCount: Int = 0,
  val totalCoursesCount: Int = 5,
  val labsLoggedCount: Int = 0,
  val verifiedCertificatesCount: Int = 0,
  val specializationEarned: Boolean = false,
  val readinessScorePercent: Int = 0,
  val readinessTier: String = "Foundation Stage",
  val nextDeliverableCourseId: String? = null
)

object CareerPortfolioHelper {

  val stageDeliverables: List<StageDeliverableSpec> = listOf(
    StageDeliverableSpec(
      courseId = "ai_artificial_intelligence_for_beginners",
      stageNumber = 1,
      deliverableTitle = "Safe AI Audit & Personal Rulebook",
      careerProofSummary = "Shows employers responsible AI verification, hallucination checks, and workplace data privacy judgment.",
      targetPillarId = "workplace"
    ),
    StageDeliverableSpec(
      courseId = "ai_ai_for_productivity",
      stageNumber = 2,
      deliverableTitle = "Weekly Time-Audit & 2-Template Productivity Pack",
      careerProofSummary = "Demonstrates measurable hours saved using structured meeting, email, and research workflows.",
      targetPillarId = "cv_resume"
    ),
    StageDeliverableSpec(
      courseId = "ai_prompt_engineering",
      stageNumber = 3,
      deliverableTitle = "Engineered JSON Prompt + 5-Case Eval Scorecard",
      careerProofSummary = "Proves technical rigor: constrained JSON schemas, edge-case testing, and prompt iteration.",
      targetPillarId = "portfolio"
    ),
    StageDeliverableSpec(
      courseId = "ai_ai_for_business_and_entrepreneurs",
      stageNumber = 4,
      deliverableTitle = "SME AI Opportunity Brief & 90-Day ROI Plan",
      careerProofSummary = "Ideal for consulting & freelancing pitches — quantifies cost, risk, and payback period for clients.",
      targetPillarId = "freelancing"
    ),
    StageDeliverableSpec(
      courseId = "ai_building_apps_with_ai",
      stageNumber = 5,
      deliverableTitle = "End-to-End Production AI App Architecture Spec",
      careerProofSummary = "Capstone engineering proof: system architecture, UX flow, safety guardrails, and QA plan.",
      targetPillarId = "interview"
    )
  )

  private val specByCourseId: Map<String, StageDeliverableSpec> = stageDeliverables.associateBy { it.courseId }

  fun deliverableForCourse(courseId: String): StageDeliverableSpec? = specByCourseId[courseId]

  fun computeReadiness(
    courses: List<Course>,
    enrollments: List<Enrollment>,
    lessonProgress: List<LessonProgress>,
    quizAttempts: List<QuizAttempt>,
    certificates: List<Certificate>
  ): CareerReadinessSnapshot {
    val courseById = courses.associateBy { it.id }
    val enrollmentByCourseId = enrollments.associateBy { it.courseId }
    val labsByCourseId = lessonProgress
      .filter { LessonNotesHelper.hasSavedLabResponse(it.studentNotes) }
      .groupBy { it.courseId }
      .mapValues { (_, items) -> items.size }
    val passedQuizzesByCourseId = quizAttempts
      .filter { it.passed }
      .groupBy { it.courseId }
      .mapValues { (_, items) -> items.map { it.quizId }.distinct().size }
    val certByCourseId = certificates.associateBy { it.courseId }

    val items = stageDeliverables.map { spec ->
      val enr = enrollmentByCourseId[spec.courseId]
      val completed = enr?.status == "COMPLETED" || (enr?.progressPercent ?: 0L) >= 100L
      val pct = when {
        completed -> 100
        enr != null -> enr.progressPercent.toInt().coerceIn(0, 99)
        else -> 0
      }
      PortfolioDeliverableItem(
        spec = spec,
        course = courseById[spec.courseId],
        courseCompleted = completed,
        progressPercent = pct,
        labsLoggedCount = labsByCourseId[spec.courseId] ?: 0,
        passedQuizCount = passedQuizzesByCourseId[spec.courseId] ?: 0,
        certificate = certByCourseId[spec.courseId]
      )
    }

    val totalCourses = stageDeliverables.size
    val completedCourses = items.count { it.courseCompleted }
    val verifiedStageCerts = items.count { it.certificate != null }
    val totalLabsLogged = items.sumOf { it.labsLoggedCount }
    val specEarned = certificates.any {
      it.courseId == AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID
    }

    // Weighted career readiness derived exclusively from real Firestore learner records:
    // 50 pts for course completions across 5 stages,
    // 25 pts for verified certificates across 5 stages,
    // 25 pts for hands-on practice labs logged (10+ labs = full 25 pts).
    val courseComponent = ((completedCourses.toDouble() / totalCourses) * 50.0)
    val certComponent = ((verifiedStageCerts.toDouble() / totalCourses) * 25.0)
    val labComponent = ((totalLabsLogged.coerceAtMost(10).toDouble() / 10.0) * 25.0)
    val score = (courseComponent + certComponent + labComponent).toInt().coerceIn(0, 100)

    val tier = when {
      score >= 80 || specEarned -> "Job & Client Ready"
      score >= 50 -> "Portfolio Builder"
      score >= 20 -> "Emerging Practitioner"
      else -> "Foundation Stage"
    }

    val nextTarget = items.firstOrNull { !it.courseCompleted && it.progressPercent > 0 }?.spec?.courseId
      ?: items.firstOrNull { !it.courseCompleted }?.spec?.courseId
      ?: items.firstOrNull { it.certificate == null }?.spec?.courseId

    return CareerReadinessSnapshot(
      deliverables = items,
      completedCoursesCount = completedCourses,
      totalCoursesCount = totalCourses,
      labsLoggedCount = totalLabsLogged,
      verifiedCertificatesCount = verifiedStageCerts,
      specializationEarned = specEarned,
      readinessScorePercent = score,
      readinessTier = tier,
      nextDeliverableCourseId = nextTarget
    )
  }

  /**
   * Builds a clean plain-text portfolio summary grounded solely in earned courses,
   * practice labs, and verified certificate codes.
   */
  fun buildPortfolioPitch(
    learnerName: String,
    snapshot: CareerReadinessSnapshot
  ): String {
    val cleanName = learnerName.ifBlank { "Datalabs AI Academy Learner" }
    val verifiedItems = snapshot.deliverables.filter { it.certificate != null || it.courseCompleted }
    return buildString {
      appendLine("$cleanName — AI Academy Applied Portfolio")
      appendLine("Readiness Tier: ${snapshot.readinessTier} (${snapshot.readinessScorePercent}%)")
      appendLine("Completed Courses: ${snapshot.completedCoursesCount}/${snapshot.totalCoursesCount} | Practice Labs Logged: ${snapshot.labsLoggedCount} | Verified Certificates: ${snapshot.verifiedCertificatesCount}")
      if (verifiedItems.isEmpty()) {
        append("Currently building foundational AI proof-of-work deliverables inside Datalabs Learning Hub.")
      } else {
        appendLine()
        appendLine("Verified Proof-of-Work Deliverables:")
        verifiedItems.forEach { item ->
          val codePart = item.certificate?.certificateCode?.let { " [Cert: $it]" } ?: ""
          appendLine("- Stage ${item.spec.stageNumber}: ${item.spec.deliverableTitle}$codePart")
        }
      }
    }.trim()
  }

  /**
   * Orders un-enrolled AI Academy courses logically along the 5-stage roadmap while
   * elevating matches to the student's stated profile interests.
   */
  fun recommendCoursesInStageOrder(
    courses: List<Course>,
    enrollments: List<Enrollment>,
    profile: UserProfile?,
    limit: Int = 5
  ): List<Course> {
    val enrolledIds = enrollments.map { it.courseId }.toSet()
    val stageOrder = AcademyProgressionHelper.computeMastery(
      courses = courses,
      enrollments = enrollments,
      certificates = emptyList()
    ).nodes.associate { it.course.id to it.stageNumber }

    val interests = (profile?.careerInterests.orEmpty() + profile?.learningInterests.orEmpty())
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() }

    return courses
      .filter { it.published && !enrolledIds.contains(it.id) }
      .sortedWith(
        compareByDescending<Course> { course ->
          val haystack = "${course.title} ${course.academyName} ${course.category} ${course.tags.joinToString(" ")}".lowercase()
          interests.count { haystack.contains(it) }
        }.thenBy { course ->
          stageOrder[course.id] ?: 99
        }.thenByDescending { it.featured }
      )
      .take(limit)
  }
}
