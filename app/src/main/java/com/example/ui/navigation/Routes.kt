package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ExploreRoute
@Serializable data object MyLearningRoute
@Serializable data object CertificatesRoute
@Serializable data object ProfileRoute

@Serializable data class AcademyDetailRoute(val academyId: String)
@Serializable data class CourseDetailRoute(val courseId: String)
@Serializable data class LessonPlayerRoute(val courseId: String, val lessonId: String)
@Serializable data class QuizRoute(val quizId: String, val courseId: String)
@Serializable data class LearningPathRoute(val pathId: String)
@Serializable data object SubscriptionRoute
@Serializable data object NotificationsRoute
@Serializable data object AdminDashboardRoute
@Serializable data object CareerHubRoute
