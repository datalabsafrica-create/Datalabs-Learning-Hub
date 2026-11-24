package com.example.data.seed

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CatalogSeeder(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private data class BatchOp(val apply: (com.google.firebase.firestore.WriteBatch) -> Unit)

  suspend fun seedInitialCatalog(forceResync: Boolean = false): Result<Int> = withContext(Dispatchers.Default) {
    return@withContext try {
      val activeAcademy = AcademyCatalogDefinitions.academies.first()
      val expectedCourseIds = activeAcademy.courses.map { courseTitle ->
        val slug = courseTitle.lowercase()
          .replace(Regex("[^a-z0-9]+"), "_")
          .trim('_')
          .take(48)
        "${activeAcademy.id.removePrefix("academy_")}_$slug"
      }.toSet()
      val expectedLessonIds = expectedCourseIds.flatMap { cid ->
        listOf("${cid}_l1", "${cid}_l2", "${cid}_l3", "${cid}_l4")
      }.toSet()
      val expectedQuizIds = expectedCourseIds.flatMap { cid ->
        listOf("quiz_${cid}_m2", "quiz_$cid")
      }.toSet()
      val expectedAssignmentIds = expectedCourseIds.map { cid -> "assign_$cid" }.toSet()
      val activePathId = "path_ai_professional"

      val existingInstructorsSnap = db.collection("instructors").get().await()
      val existingPlansSnap = db.collection("subscriptionPlans").get().await()
      val existingAnnouncementsSnap = db.collection("announcements").get().await()
      val existingAcademiesSnap = db.collection("academies").get().await()
      val existingCoursesSnap = db.collection("courses").get().await()
      val existingLessonsSnap = db.collection("lessons").get().await()
      val existingContentsSnap = db.collection("lessonContents").get().await()
      val existingQuizzesSnap = db.collection("quizzes").get().await()
      val existingAssignmentsSnap = db.collection("assignments").get().await()
      val existingPathsSnap = db.collection("learningPaths").get().await()

      if (!forceResync) {
        val singleAcademyDoc = existingAcademiesSnap.documents.singleOrNull()
        val hasCompleteCurriculum =
          singleAcademyDoc != null &&
            singleAcademyDoc.id == AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID &&
            singleAcademyDoc.getLong("courseCount") == 5L &&
            singleAcademyDoc.getLong("curriculumVersion") == AcademyCatalogDefinitions.CURRICULUM_VERSION &&
            existingCoursesSnap.documents.map { it.id }.toSet() == expectedCourseIds &&
            existingLessonsSnap.documents.map { it.id }.toSet().containsAll(expectedLessonIds) &&
            existingContentsSnap.documents.map { it.id }.toSet().containsAll(expectedLessonIds) &&
            existingQuizzesSnap.documents.map { it.id }.toSet().containsAll(expectedQuizIds) &&
            existingAssignmentsSnap.documents.map { it.id }.toSet().containsAll(expectedAssignmentIds) &&
            existingPathsSnap.documents.any { it.id == activePathId } &&
            existingPathsSnap.documents.all { it.getString("academyId") == AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID }
        if (hasCompleteCurriculum) {
          return@withContext Result.success(0)
        }
      }

      val instructorById = existingInstructorsSnap.documents.associateBy { it.id }
      val planById = existingPlansSnap.documents.associateBy { it.id }
      val announcementById = existingAnnouncementsSnap.documents.associateBy { it.id }
      val academyById = existingAcademiesSnap.documents.associateBy { it.id }
      val courseById = existingCoursesSnap.documents.associateBy { it.id }
      val lessonById = existingLessonsSnap.documents.associateBy { it.id }
      val contentById = existingContentsSnap.documents.associateBy { it.id }
      val quizById = existingQuizzesSnap.documents.associateBy { it.id }
      val assignmentById = existingAssignmentsSnap.documents.associateBy { it.id }
      val pathById = existingPathsSnap.documents.associateBy { it.id }

      val preCleanupOps = mutableListOf<BatchOp>()
      val ops = mutableListOf<BatchOp>()

      fun resolveCreatedAt(existingDoc: com.google.firebase.firestore.DocumentSnapshot?): Any {
        if (existingDoc != null && existingDoc.exists()) {
          val ts = existingDoc.getTimestamp("createdAt")
          if (ts != null) return ts
          preCleanupOps += BatchOp { batch -> batch.delete(existingDoc.reference) }
        }
        return FieldValue.serverTimestamp()
      }

      // 1. Instructors
      for (inst in AcademyCatalogDefinitions.instructors) {
        val existingDoc = instructorById[inst.id]
        val createdAtVal = resolveCreatedAt(existingDoc)
        ops += BatchOp { batch ->
          batch.set(
            db.collection("instructors").document(inst.id),
            mapOf(
              "fullName" to inst.fullName,
              "title" to inst.title,
              "bio" to inst.bio,
              "specialty" to inst.specialty,
              "createdAt" to createdAtVal,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }

      // 2. Subscription Plans (USD & UGX)
      val monthlyCreatedAt = resolveCreatedAt(planById["plan_monthly_pro"])
      val annualCreatedAt = resolveCreatedAt(planById["plan_annual_pro"])
      ops += BatchOp { batch ->
        batch.set(
          db.collection("subscriptionPlans").document("plan_monthly_pro"),
          mapOf(
            "name" to "Datalabs Pro Monthly",
            "billingCycle" to "MONTHLY",
            "priceUsd" to 12.0,
            "priceUgx" to 45000L,
            "description" to "Full access to AI Academy, premium courses, downloadable practice kits, AI Study Assistant, and verified certificates.",
            "features" to listOf(
              "Full access to all 5 AI Academy courses",
              "AI-Powered Professional Learning Pathway",
              "Downloadable practice kits, templates & checklists",
              "Verified completion certificates & PDF export",
              "Module checkpoints & final examinations"
            ),
            "active" to true,
            "createdAt" to monthlyCreatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        batch.set(
          db.collection("subscriptionPlans").document("plan_annual_pro"),
          mapOf(
            "name" to "Datalabs Pro Annual",
            "billingCycle" to "ANNUAL",
            "priceUsd" to 99.0,
            "priceUgx" to 360000L,
            "description" to "Best value annual membership for AI practitioners, builders, and entrepreneurs worldwide.",
            "features" to listOf(
              "Save over 30% vs monthly billing",
              "Full premium catalogue in AI Academy",
              "Portfolio project reviews & assessments",
              "Priority certificate verification",
              "International USD & Uganda UGX support"
            ),
            "active" to true,
            "createdAt" to annualCreatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      // 3. Platform Announcements
      val welcomeAnnCreatedAt = resolveCreatedAt(announcementById["ann_welcome_hub"])
      ops += BatchOp { batch ->
        batch.set(
          db.collection("announcements").document("ann_welcome_hub"),
          mapOf(
            "title" to "Welcome to AI Academy on Datalabs Learning Hub",
            "message" to "Explore 5 complete AI Academy courses covering foundations, everyday productivity, prompt engineering, business automation, and building apps with AI—each with 4 modules, case studies, practice kits, module quizzes, final exams, and an AI Study Assistant.",
            "category" to "Academy Update",
            "academyId" to AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID,
            "published" to true,
            "createdAt" to welcomeAnnCreatedAt
          )
        )
        if (announcementById.containsKey("ann_data_projects")) {
          batch.delete(db.collection("announcements").document("ann_data_projects"))
        }
      }

      // Track active IDs so any deprecated catalog documents are cleaned up cleanly
      val courseIdByTitle = mutableMapOf<String, String>()
      val activeAcademyIds = mutableSetOf<String>()
      val activeCourseIds = mutableSetOf<String>()
      val activeLessonIds = mutableSetOf<String>()
      val activeQuizIds = mutableSetOf<String>()
      val activeAssignmentIds = mutableSetOf<String>()

      // 4. Active Academy, Courses, 4 Lessons + Gated LessonContents, Dual Quizzes, and Assignment
      for (academy in AcademyCatalogDefinitions.academies) {
        activeAcademyIds += academy.id
        val academyCreatedAt = resolveCreatedAt(academyById[academy.id])
        ops += BatchOp { batch ->
          batch.set(
            db.collection("academies").document(academy.id),
            mapOf(
              "name" to academy.name,
              "shortName" to academy.shortName,
              "description" to academy.description,
              "iconKey" to academy.iconKey,
              "accentHex" to academy.accentHex,
              "displayOrder" to academy.displayOrder,
              "courseCount" to academy.courses.size.toLong(),
              "curriculumVersion" to AcademyCatalogDefinitions.CURRICULUM_VERSION,
              "published" to true,
              "createdAt" to academyCreatedAt,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        academy.courses.forEachIndexed { idx, courseTitle ->
          val slug = courseTitle.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(48)
          val courseId = "${academy.id.removePrefix("academy_")}_$slug"
          courseIdByTitle[courseTitle] = courseId
          activeCourseIds += courseId

          val isFree = idx < 3 || courseTitle.contains("for Beginners", ignoreCase = true)
          val accessTier = if (isFree) "FREE" else "PREMIUM"
          val level = when {
            courseTitle.contains("Beginner", ignoreCase = true) || idx <= 1 -> "BEGINNER"
            courseTitle.contains("Building Apps", ignoreCase = true) ||
              courseTitle.contains("Advanced", ignoreCase = true) ||
              courseTitle.contains("Masterclass", ignoreCase = true) ||
              courseTitle.contains("Accelerator", ignoreCase = true) -> "ADVANCED"
            else -> "INTERMEDIATE"
          }
          val featured = idx < 2

          val curriculum = CourseContentBlueprint.buildForCourse(
            academy = academy,
            courseTitle = courseTitle,
            slug = slug,
            isFree = isFree,
            level = level
          )

          val existingCourseDoc = courseById[courseId]
          val courseCreatedAt = resolveCreatedAt(existingCourseDoc)
          val preservedEnrollmentCount = (existingCourseDoc?.getLong("enrollmentCount") ?: 0L).coerceAtLeast(0L)
          val preservedPopularityScore = (existingCourseDoc?.getLong("popularityScore") ?: 0L).coerceAtLeast(0L)

          ops += BatchOp { batch ->
            batch.set(
              db.collection("courses").document(courseId),
              mapOf(
                "academyId" to academy.id,
                "academyName" to academy.name,
                "title" to courseTitle,
                "subtitle" to curriculum.subtitle,
                "description" to curriculum.description,
                "instructorId" to academy.instructorId,
                "instructorName" to academy.instructorName,
                "level" to level,
                "durationMinutes" to curriculum.durationMinutes,
                "lessonCount" to curriculum.lessonCount,
                "moduleCount" to 4L,
                "accessTier" to accessTier,
                "priceUsd" to if (isFree) 0.0 else 19.0,
                "priceUgx" to if (isFree) 0L else 70000L,
                "certificateAvailable" to true,
                "published" to true,
                "featured" to featured,
                "enrollmentCount" to preservedEnrollmentCount,
                "popularityScore" to preservedPopularityScore,
                "whatYouWillLearn" to curriculum.whatYouWillLearn,
                "prerequisites" to curriculum.prerequisites,
                "formats" to curriculum.formats,
                "tags" to curriculum.tags,
                "category" to curriculum.primaryCategory,
                "categories" to curriculum.categories,
                "createdAt" to courseCreatedAt,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
          }

          // All 4 lessons + 4 gated LessonContents per course
          curriculum.lessons.forEach { lesson ->
            val lessonId = "${courseId}_${lesson.suffix}"
            activeLessonIds += lessonId
            val lessonCreatedAt = resolveCreatedAt(lessonById[lessonId])
            val contentCreatedAt = resolveCreatedAt(contentById[lessonId])
            ops += BatchOp { batch ->
              batch.set(
                db.collection("lessons").document(lessonId),
                mapOf(
                  "courseId" to courseId,
                  "academyId" to academy.id,
                  "moduleTitle" to lesson.moduleTitle,
                  "title" to lesson.title,
                  "summary" to lesson.summary,
                  "orderIndex" to lesson.orderIndex,
                  "durationMinutes" to lesson.durationMinutes,
                  "format" to lesson.format,
                  "freePreview" to lesson.freePreview,
                  "premiumOnly" to !lesson.freePreview,
                  "createdAt" to lessonCreatedAt,
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
              batch.set(
                db.collection("lessonContents").document(lessonId),
                mapOf(
                  "courseId" to courseId,
                  "freePreview" to lesson.freePreview,
                  "premiumOnly" to !lesson.freePreview,
                  "notesMarkdown" to lesson.notesMarkdown,
                  "slidesText" to lesson.slidesText,
                  "downloadTitle" to lesson.downloadTitle,
                  "downloadContent" to lesson.downloadContent,
                  "exercisePrompt" to lesson.exercisePrompt,
                  "keyTakeaways" to lesson.keyTakeaways,
                  "caseStudyText" to lesson.caseStudyText,
                  "createdAt" to contentCreatedAt,
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }

          // Module Checkpoint Quiz + Final Examination + Practical Assignment for EVERY course
          val moduleQuizId = "quiz_${courseId}_m2"
          val finalQuizId = "quiz_$courseId"
          val assignId = "assign_$courseId"
          activeQuizIds += moduleQuizId
          activeQuizIds += finalQuizId
          activeAssignmentIds += assignId

          val moduleQuizCreatedAt = resolveCreatedAt(quizById[moduleQuizId])
          val finalQuizCreatedAt = resolveCreatedAt(quizById[finalQuizId])
          val assignCreatedAt = resolveCreatedAt(assignmentById[assignId])

          ops += BatchOp { batch ->
            batch.set(
              db.collection("quizzes").document(moduleQuizId),
              mapOf(
                "courseId" to courseId,
                "assessmentType" to "MODULE_QUIZ",
                "moduleIndex" to 2L,
                "title" to curriculum.moduleQuizTitle,
                "description" to curriculum.moduleQuizDescription,
                "passingScorePercent" to 70L,
                "maxAttempts" to 10L,
                "allowRetakes" to true,
                "questionsJson" to curriculum.moduleQuizQuestionsJson,
                "createdAt" to moduleQuizCreatedAt,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
            batch.set(
              db.collection("quizzes").document(finalQuizId),
              mapOf(
                "courseId" to courseId,
                "assessmentType" to "FINAL_EXAM",
                "moduleIndex" to 4L,
                "title" to curriculum.quizTitle,
                "description" to curriculum.quizDescription,
                "passingScorePercent" to 70L,
                "maxAttempts" to 5L,
                "allowRetakes" to true,
                "questionsJson" to curriculum.quizQuestionsJson,
                "createdAt" to finalQuizCreatedAt,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
            batch.set(
              db.collection("assignments").document(assignId),
              mapOf(
                "courseId" to courseId,
                "title" to curriculum.assignmentTitle,
                "instructions" to curriculum.assignmentInstructions,
                "deliverableFormat" to curriculum.assignmentDeliverableFormat,
                "maxPoints" to 100L,
                "createdAt" to assignCreatedAt,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
          }
        }
      }

      // 5. Structured Learning Path for AI Academy
      fun resolveIds(titles: List<String>): Pair<List<String>, List<String>> {
        val ids = mutableListOf<String>()
        val resolvedTitles = mutableListOf<String>()
        for (t in titles) {
          val found = courseIdByTitle[t]
            ?: courseIdByTitle.entries.firstOrNull { it.key.contains(t, ignoreCase = true) }?.value
          if (found != null) {
            ids += found
            resolvedTitles += t
          }
        }
        return ids to resolvedTitles
      }

      val (pathIds, pathTitles) = resolveIds(activeAcademy.courses)
      val pathCreatedAt = resolveCreatedAt(pathById[activePathId])
      ops += BatchOp { batch ->
        batch.set(
          db.collection("learningPaths").document(activePathId),
          mapOf(
            "title" to "Become an AI-Powered Professional",
            "description" to "Step-by-step AI Academy pathway combining all ${pathIds.size} structured courses from foundations to building real AI applications.",
            "academyId" to AcademyCatalogDefinitions.ACTIVE_ACADEMY_ID,
            "targetRole" to "AI-Powered Professional",
            "courseIds" to pathIds,
            "courseTitles" to pathTitles,
            "estimatedHours" to (pathIds.size * 3L).coerceAtLeast(6L),
            "premiumOnly" to false,
            "published" to true,
            "createdAt" to pathCreatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      // Clean up any other academies/courses/lessons/quizzes/assignments/paths from previously seeded catalogs
      // (User accounts, enrollments, lessonProgress, certificates, and subscriptions are never modified or deleted.)
      existingAcademiesSnap.documents.filter { it.id !in activeAcademyIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingCoursesSnap.documents.filter { it.id !in activeCourseIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingLessonsSnap.documents.filter { it.id !in activeLessonIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingContentsSnap.documents.filter { it.id !in activeLessonIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingQuizzesSnap.documents.filter { it.id !in activeQuizIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingAssignmentsSnap.documents.filter { it.id !in activeAssignmentIds }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }
      existingPathsSnap.documents.filter { it.id != activePathId }.forEach { doc ->
        ops += BatchOp { batch -> batch.delete(doc.reference) }
      }

      // Phase A (only if malformed legacy docs lacked createdAt): delete before re-creating
      preCleanupOps.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { it.apply(batch) }
        batch.commit().await()
      }

      // Phase B: Commit all active updates/creates and stale-doc deletions in 400-operation chunks
      ops.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { it.apply(batch) }
        batch.commit().await()
      }

      Result.success(ops.size)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
