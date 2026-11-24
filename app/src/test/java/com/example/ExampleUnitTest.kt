package com.example

import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.CareerPortfolioHelper
import com.example.data.model.Certificate
import com.example.data.model.Course
import com.example.data.model.CourseCertificationPolicy
import com.example.data.model.Enrollment
import com.example.data.model.Lesson
import com.example.data.model.LessonProgress
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.data.repository.AuthManager
import com.example.data.repository.AuthPreferencesState
import com.example.data.repository.RememberMePolicy
import com.example.data.seed.AcademyCatalogDefinitions
import com.example.data.seed.AcademyProgressionHelper
import com.example.ui.components.LessonNotesHelper
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.parseAccentColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests verifying core startup state, Remember Me policy, and pure helpers.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun authPreferencesStateDefaults_areSafe() {
    val state = AuthPreferencesState()
    assertTrue(state.rememberMe)
    assertFalse(state.rememberPromptHandled)
    assertFalse(state.loadedFromDisk)
    assertNull(state.rememberedEmail)
  }

  @Test
  fun authStateFlow_handlesNullFirebaseAuthSafely() = runBlocking {
    val user = AuthManager.authStateFlow(auth = null).first()
    assertNull(user)
  }

  @Test
  fun parseAccentColor_handlesInvalidInputWithFallback() {
    assertEquals(ElectricBlue, parseAccentColor("", ElectricBlue))
    assertEquals(ElectricBlue, parseAccentColor("invalid_hex", ElectricBlue))
  }

  @Test
  fun coldStartSignOut_triggersOnlyWhenLoadedSignedInAndRememberMeFalse() {
    val sessionOnlyLoaded = AuthPreferencesState(rememberMe = false, loadedFromDisk = true)
    assertTrue(RememberMePolicy.shouldSignOutOnColdStart(hasSignedInUser = true, prefs = sessionOnlyLoaded))
    assertFalse(RememberMePolicy.shouldSignOutOnColdStart(hasSignedInUser = false, prefs = sessionOnlyLoaded))

    val notLoadedYet = AuthPreferencesState(rememberMe = false, loadedFromDisk = false)
    assertFalse(RememberMePolicy.shouldSignOutOnColdStart(hasSignedInUser = true, prefs = notLoadedYet))

    val rememberedLoaded = AuthPreferencesState(rememberMe = true, loadedFromDisk = true)
    assertFalse(RememberMePolicy.shouldSignOutOnColdStart(hasSignedInUser = true, prefs = rememberedLoaded))
  }

  @Test
  fun autoSignIn_gatesCorrectlyOnPrefsAndSessionSuppression() {
    val eligible = AuthPreferencesState(
      rememberMe = true,
      rememberPromptHandled = true,
      rememberedEmail = "learner@example.com",
      loadedFromDisk = true
    )
    assertTrue(RememberMePolicy.shouldAttemptAutoSignIn(eligible, suppressForSession = false))
    assertFalse(RememberMePolicy.shouldAttemptAutoSignIn(eligible, suppressForSession = true))
    assertFalse(RememberMePolicy.shouldAttemptAutoSignIn(eligible.copy(rememberMe = false), suppressForSession = false))
    assertFalse(RememberMePolicy.shouldAttemptAutoSignIn(eligible.copy(rememberedEmail = " "), suppressForSession = false))
    assertFalse(RememberMePolicy.shouldAttemptAutoSignIn(eligible.copy(rememberPromptHandled = false), suppressForSession = false))
    assertFalse(RememberMePolicy.shouldAttemptAutoSignIn(eligible.copy(loadedFromDisk = false), suppressForSession = false))
  }

  @Test
  fun applyRememberMeChange_updatesIdentityAndPromptStateCleanly() {
    val initial = AuthPreferencesState(
      rememberMe = true,
      rememberPromptHandled = false,
      rememberedEmail = "learner@example.com",
      rememberedDisplayName = "Ada Lovelace",
      loadedFromDisk = true
    )

    val toggledOff = RememberMePolicy.applyRememberMeChange(
      current = initial,
      enabled = false,
      markPromptHandled = false
    )
    assertFalse(toggledOff.rememberMe)
    assertFalse(toggledOff.rememberPromptHandled)
    assertNull(toggledOff.rememberedEmail)
    assertNull(toggledOff.rememberedDisplayName)

    val confirmedOn = RememberMePolicy.applyRememberMeChange(
      current = toggledOff,
      enabled = true,
      markPromptHandled = true,
      email = "  ada@datalabs.org ",
      displayName = " Ada Lovelace "
    )
    assertTrue(confirmedOn.rememberMe)
    assertTrue(confirmedOn.rememberPromptHandled)
    assertEquals("ada@datalabs.org", confirmedOn.rememberedEmail)
    assertEquals("Ada Lovelace", confirmedOn.rememberedDisplayName)
  }

  private fun sampleAiCoursesReversed(): List<Course> {
    return AcademyCatalogDefinitions.aiAcademyStages.reversed().map { stage ->
      Course(
        id = stage.courseId,
        academyId = AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID,
        title = "Course ${stage.stageNumber}",
        subtitle = stage.outcomeTag
      )
    }
  }

  @Test
  fun computeMastery_sortsCanonicalStagesAndRecommendsStage1WhenUnenrolled() {
    val summary = AcademyProgressionHelper.computeMastery(
      courses = sampleAiCoursesReversed(),
      enrollments = emptyList(),
      certificates = emptyList()
    )

    assertEquals(listOf(1, 2, 3, 4, 5), summary.nodes.map { it.stageNumber })
    assertEquals(5, summary.totalCourses)
    assertEquals(0, summary.completedCoursesCount)
    assertEquals(0, summary.overallProgressPercent)
    assertFalse(summary.allRequiredCoursesCompleted)
    assertEquals("ai_artificial_intelligence_for_beginners", summary.nextRecommendedNode?.course?.id)
    assertTrue(summary.nodes[0].prerequisiteMet)
    assertFalse(summary.nodes[1].prerequisiteMet)
  }

  @Test
  fun computeMastery_prioritizesActiveIncompleteCourseAndTracksSoftPrerequisites() {
    val enrollments = listOf(
      Enrollment(
        id = "u_s1",
        userId = "u",
        courseId = "ai_artificial_intelligence_for_beginners",
        status = "COMPLETED",
        progressPercent = 100L
      ),
      Enrollment(
        id = "u_s3",
        userId = "u",
        courseId = "ai_prompt_engineering",
        status = "ACTIVE",
        progressPercent = 40L,
        lastLessonId = "lesson_3_2"
      )
    )

    val summary = AcademyProgressionHelper.computeMastery(
      courses = sampleAiCoursesReversed(),
      enrollments = enrollments,
      certificates = emptyList()
    )

    assertEquals(1, summary.completedCoursesCount)
    assertEquals(1, summary.inProgressCoursesCount)
    assertEquals(28, summary.overallProgressPercent) // (100 + 40) / 5
    // Stage 3 is actively in progress, so it becomes the recommended resume target
    assertEquals("ai_prompt_engineering", summary.nextRecommendedNode?.course?.id)
    // Stage 2 prerequisite (Stage 1) is met; Stage 3 prerequisite (Stage 2) is not yet met
    assertTrue(summary.nodes[1].prerequisiteMet)
    assertFalse(summary.nodes[2].prerequisiteMet)
  }

  @Test
  fun computeMastery_unlocksSpecializationWhenAllFiveCoursesCompleted() {
    val completedEnrollments = AcademyCatalogDefinitions.requiredSpecializationCourseIds.map { cid ->
      Enrollment(
        id = "u_$cid",
        userId = "u",
        courseId = cid,
        status = "COMPLETED",
        progressPercent = 100L
      )
    }
    val specCert = Certificate(
      id = "u_${AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID}",
      certificateCode = "DLH-SPEC-1234",
      userId = "u",
      courseId = AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID,
      courseTitle = AcademyCatalogDefinitions.AI_SPECIALIZATION_TITLE
    )

    val summary = AcademyProgressionHelper.computeMastery(
      courses = sampleAiCoursesReversed(),
      enrollments = completedEnrollments,
      certificates = listOf(specCert)
    )

    assertTrue(summary.allRequiredCoursesCompleted)
    assertEquals(5, summary.completedCoursesCount)
    assertEquals(100, summary.overallProgressPercent)
    assertNull(summary.nextRecommendedNode)
    assertEquals("DLH-SPEC-1234", summary.specializationCertificate?.certificateCode)
  }

  @Test
  fun lessonNotesHelper_upsertsAndExtractsLabSectionPreservingPersonalNotes() {
    val personalNotes = "Key insight: Always isolate untrusted input in XML tags."
    val firstSave = LessonNotesHelper.upsertLabResponse(
      existingNotes = personalNotes,
      labResponse = "<payload>Invoice #42</payload>"
    )

    assertTrue(LessonNotesHelper.hasSavedLabResponse(firstSave))
    assertTrue(firstSave.startsWith(personalNotes))
    assertEquals("<payload>Invoice #42</payload>", LessonNotesHelper.extractLabDraft(firstSave))

    val updatedSave = LessonNotesHelper.upsertLabResponse(
      existingNotes = firstSave,
      labResponse = "<payload>Updated Invoice #42</payload>"
    )
    assertTrue(updatedSave.startsWith(personalNotes))
    assertEquals("<payload>Updated Invoice #42</payload>", LessonNotesHelper.extractLabDraft(updatedSave))
    assertEquals(1, updatedSave.split(LessonNotesHelper.LAB_SECTION_HEADER).size - 1)

    val clearedLab = LessonNotesHelper.upsertLabResponse(
      existingNotes = updatedSave,
      labResponse = "   "
    )
    assertFalse(LessonNotesHelper.hasSavedLabResponse(clearedLab))
    assertEquals(personalNotes, clearedLab)
  }

  @Test
  fun certificationPolicy_gatesOnLessonsModuleQuizFinalExamAndAssignment() {
    val lessons = (1..4).map { idx -> Lesson(id = "c1_l$idx", courseId = "c1", title = "Lesson $idx") }
    val moduleQuiz = Quiz(id = "quiz_c1_m2", courseId = "c1", assessmentType = "MODULE_QUIZ", title = "Module Quiz", passingScorePercent = 70L)
    val finalQuiz = Quiz(id = "quiz_c1", courseId = "c1", assessmentType = "FINAL_EXAM", title = "Final Exam", passingScorePercent = 70L)
    val assignment = Assignment(id = "assign_c1", courseId = "c1", title = "Practical Deliverable")

    // Case 1: Only lessons complete -> 3 assessment blocks remain
    val onlyLessonsDone = CourseCertificationPolicy.evaluateReadiness(
      lessons = lessons,
      completedLessonIds = lessons.map { it.id }.toSet(),
      quizzes = listOf(moduleQuiz, finalQuiz),
      quizAttempts = listOf(QuizAttempt(quizId = finalQuiz.id, passed = false, scorePercent = 50L)),
      assignments = listOf(assignment),
      assignmentSubmissions = emptyMap()
    )
    assertTrue(onlyLessonsDone.lessonsComplete)
    assertFalse(onlyLessonsDone.moduleQuizPassed)
    assertFalse(onlyLessonsDone.finalQuizPassed)
    assertFalse(onlyLessonsDone.assignmentSubmitted)
    assertFalse(onlyLessonsDone.isEligible)
    assertEquals(3, onlyLessonsDone.blockingReasons.size)

    // Case 2: All lessons + both quizzes passed + assignment submitted -> eligible
    val ready = CourseCertificationPolicy.evaluateReadiness(
      lessons = lessons,
      completedLessonIds = lessons.map { it.id }.toSet(),
      quizzes = listOf(moduleQuiz, finalQuiz),
      quizAttempts = listOf(
        QuizAttempt(quizId = moduleQuiz.id, passed = true, scorePercent = 100L),
        QuizAttempt(quizId = finalQuiz.id, passed = true, scorePercent = 75L)
      ),
      assignments = listOf(assignment),
      assignmentSubmissions = mapOf(
        assignment.id to AssignmentSubmission(id = "u1", assignmentId = assignment.id, status = "SUBMITTED")
      )
    )
    assertTrue(ready.isEligible)
    assertTrue(ready.blockingReasons.isEmpty())
    assertTrue(ready.assignmentSubmitted)
  }

  @Test
  fun careerPortfolioHelper_computesLiveReadinessAndBuildsGroundedPitch() {
    val courses = sampleAiCoursesReversed()
    val enrollments = listOf(
      Enrollment(
        id = "u_s1",
        userId = "u",
        courseId = "ai_artificial_intelligence_for_beginners",
        status = "COMPLETED",
        progressPercent = 100L
      ),
      Enrollment(
        id = "u_s2",
        userId = "u",
        courseId = "ai_ai_for_productivity",
        status = "ACTIVE",
        progressPercent = 50L
      )
    )
    val lessonProgress = listOf(
      LessonProgress(
        id = "lp1",
        courseId = "ai_artificial_intelligence_for_beginners",
        lessonId = "l1",
        completed = true,
        studentNotes = LessonNotesHelper.upsertLabResponse("", "Checked privacy policy")
      ),
      LessonProgress(
        id = "lp2",
        courseId = "ai_artificial_intelligence_for_beginners",
        lessonId = "l2",
        completed = true,
        studentNotes = LessonNotesHelper.upsertLabResponse("", "Redacted client PII")
      ),
      LessonProgress(
        id = "lp3",
        courseId = "ai_ai_for_productivity",
        lessonId = "l1",
        completed = true,
        studentNotes = "Just regular notes without lab"
      ),
      LessonProgress(
        id = "lp4",
        courseId = "ai_ai_for_productivity",
        lessonId = "l2",
        completed = false,
        studentNotes = LessonNotesHelper.upsertLabResponse("", "Drafted 2 weekly templates")
      )
    )
    val certs = listOf(
      Certificate(
        id = "cert_1",
        certificateCode = "DLH-AI-101",
        courseId = "ai_artificial_intelligence_for_beginners",
        courseTitle = "Artificial Intelligence for Beginners"
      )
    )

    val snapshot = CareerPortfolioHelper.computeReadiness(
      courses = courses,
      enrollments = enrollments,
      lessonProgress = lessonProgress,
      quizAttempts = listOf(
        QuizAttempt(quizId = "q1", courseId = "ai_artificial_intelligence_for_beginners", passed = true)
      ),
      certificates = certs
    )

    assertEquals(1, snapshot.completedCoursesCount)
    assertEquals(3, snapshot.labsLoggedCount)
    assertEquals(1, snapshot.verifiedCertificatesCount)
    // 1/5 * 50 = 10 + 1/5 * 25 = 5 + 3/10 * 25 = 7.5 -> 22%
    assertEquals(22, snapshot.readinessScorePercent)
    assertEquals("Emerging Practitioner", snapshot.readinessTier)
    assertEquals("ai_ai_for_productivity", snapshot.nextDeliverableCourseId)

    val stage1Item = snapshot.deliverables.first { it.spec.stageNumber == 1 }
    assertEquals("Verified Proof", stage1Item.statusLabel)
    assertEquals(2, stage1Item.labsLoggedCount)
    assertEquals(1, stage1Item.passedQuizCount)

    val pitch = CareerPortfolioHelper.buildPortfolioPitch("Grace Hopper", snapshot)
    assertTrue(pitch.contains("Grace Hopper"))
    assertTrue(pitch.contains("DLH-AI-101"))
    assertTrue(pitch.contains("Practice Labs Logged: 3"))
  }

  @Test
  fun recommendCoursesInStageOrder_respectsStageSequenceAndInterests() {
    val courses = AcademyCatalogDefinitions.aiAcademyStages.reversed().map { stage ->
      Course(
        id = stage.courseId,
        academyId = AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID,
        title = stage.trackLabel,
        tags = if (stage.stageNumber == 4) listOf("entrepreneurship", "roi") else listOf("ai"),
        published = true
      )
    }
    val enrollments = listOf(
      Enrollment(
        id = "u_s1",
        courseId = "ai_artificial_intelligence_for_beginners",
        status = "COMPLETED",
        progressPercent = 100L
      )
    )

    // Without specific interests, recommends Stage 2, 3, 4, 5 in canonical order
    val defaultOrder = CareerPortfolioHelper.recommendCoursesInStageOrder(
      courses = courses,
      enrollments = enrollments,
      profile = UserProfile(),
      limit = 5
    )
    assertEquals(
      listOf(
        "ai_ai_for_productivity",
        "ai_prompt_engineering",
        "ai_ai_for_business_and_entrepreneurs",
        "ai_building_apps_with_ai"
      ),
      defaultOrder.map { it.id }
    )

    // With entrepreneurship interest, Stage 4 surfaces first while others remain in stage order
    val interestOrder = CareerPortfolioHelper.recommendCoursesInStageOrder(
      courses = courses,
      enrollments = enrollments,
      profile = UserProfile(careerInterests = listOf("entrepreneurship")),
      limit = 5
    )
    assertEquals("ai_ai_for_business_and_entrepreneurs", interestOrder.first().id)
    assertEquals("ai_ai_for_productivity", interestOrder[1].id)
  }
}
