package com.example.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.DatalabsApplication
import com.example.data.repository.AuthManager
import com.example.ui.screens.AcademyDetailScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CareerHubScreen
import com.example.ui.screens.CertificatesScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearningPathScreen
import com.example.ui.screens.LessonPlayerScreen
import com.example.ui.screens.MyLearningScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.NavyDeep
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.flowOf

private data class TopLevelTab(
  val label: String,
  val icon: ImageVector,
  val route: Any,
  val testTag: String,
  val matches: (androidx.navigation.NavDestination?) -> Boolean
)

@Composable
fun DatalabsApp() {
  val app = LocalContext.current.applicationContext as DatalabsApplication
  val authPreferences = app.container.authPreferencesRepository

  var warmupReady by remember { mutableStateOf(app.isStartupWarmupComplete()) }

  LaunchedEffect(app) {
    if (!warmupReady) {
      try {
        app.awaitStartupWarmup(450L)
      } finally {
        warmupReady = true
      }
    }
  }

  if (!warmupReady) {
    // Matches android:windowBackground seamlessly with zero animation/render contention during window sync.
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(NavyDeep)
    )
    return
  }

  val initialUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }
  val authFlow = remember {
    runCatching { AuthManager.authStateFlow() }
      .getOrElse { flowOf(initialUser) }
  }
  val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = initialUser)

  val user = currentUser
  if (user == null) {
    AuthScreen(
      authPreferences = authPreferences,
      onAuthSuccess = { signedInUser, rememberMe ->
        authPreferences.saveSignedInSessionAsync(
          email = signedInUser?.email,
          displayName = signedInUser?.displayName,
          rememberMe = rememberMe
        )
      }
    )
  } else {
    key(user.uid) {
      AuthenticatedAppShell(
        uid = user.uid,
        userEmail = user.email,
        userName = user.displayName
      )
    }
  }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AuthenticatedAppShell(
  uid: String,
  userEmail: String?,
  userName: String?
) {
  val app = LocalContext.current.applicationContext as DatalabsApplication
  val authPreferences = app.container.authPreferencesRepository
  val authPrefs by authPreferences.stateFlow.collectAsStateWithLifecycle()
  val viewModel: MainViewModel = viewModel(
    key = uid,
    factory = MainViewModel.Factory.create(app, uid)
  )

  val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
  val studentState by viewModel.studentState.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isSeedingCatalog by viewModel.isSeedingCatalog.collectAsStateWithLifecycle()
  val compareCourseIds by viewModel.compareCourseIds.collectAsStateWithLifecycle()
  val verifiedCertificateResult by viewModel.verifiedCertificateResult.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(viewModel) {
    viewModel.uiMessage.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination
  var activeExploreCategory by remember { mutableStateOf<String?>(null) }

  val tabs = remember {
    listOf(
      TopLevelTab("Home", Icons.Filled.Home, HomeRoute, "nav_home") {
        it?.hasRoute<HomeRoute>() == true
      },
      TopLevelTab("Explore", Icons.Filled.Explore, ExploreRoute, "nav_explore") {
        it?.hasRoute<ExploreRoute>() == true
      },
      TopLevelTab("My Learning", Icons.Filled.MenuBook, MyLearningRoute, "nav_my_learning") {
        it?.hasRoute<MyLearningRoute>() == true
      },
      TopLevelTab("Certificates", Icons.Filled.Verified, CertificatesRoute, "nav_certificates") {
        it?.hasRoute<CertificatesRoute>() == true
      },
      TopLevelTab("Profile", Icons.Filled.Person, ProfileRoute, "nav_profile") {
        it?.hasRoute<ProfileRoute>() == true
      }
    )
  }

  val showTopLevelNav = tabs.any { it.matches(currentDest) }

  fun navigateTopLevel(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showTopLevelNav && !useNavigationRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { tab ->
              val selected = tab.matches(currentDest)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopLevel(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                  Text(
                    text = tab.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                },
                modifier = Modifier.testTag(tab.testTag)
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (showTopLevelNav && useNavigationRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface
          ) {
            tabs.forEach { tab ->
              val selected = tab.matches(currentDest)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag(tab.testTag)
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = HomeRoute
          ) {
            composable<HomeRoute> {
              HomeScreen(
                catalogState = catalogState,
                studentState = studentState,
                isAdmin = isAdmin,
                isSeedingCatalog = isSeedingCatalog,
                onOpenExplore = {
                  activeExploreCategory = null
                  navigateTopLevel(ExploreRoute)
                },
                onOpenCategory = { categoryName ->
                  activeExploreCategory = categoryName
                  navigateTopLevel(ExploreRoute)
                },
                onOpenCareerHub = { navController.navigate(CareerHubRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onOpenProfile = { navigateTopLevel(ProfileRoute) },
                onOpenAdmin = { navController.navigate(AdminDashboardRoute) },
                onOpenAcademy = { academyId -> navController.navigate(AcademyDetailRoute(academyId)) },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onResumeLesson = { courseId, lessonId ->
                  navController.navigate(LessonPlayerRoute(courseId, lessonId))
                },
                onOpenLearningPath = { pathId -> navController.navigate(LearningPathRoute(pathId)) },
                onToggleSaveCourse = { course, saved -> viewModel.toggleSavedCourse(course, saved) },
                onSyncStarterCatalog = { viewModel.adminResyncStarterCatalog() }
              )
            }

            composable<ExploreRoute> {
              ExploreScreen(
                catalogState = catalogState,
                studentState = studentState,
                compareCourseIds = compareCourseIds,
                selectedCategory = activeExploreCategory,
                onSelectCategory = { activeExploreCategory = it },
                onOpenAcademy = { academyId -> navController.navigate(AcademyDetailRoute(academyId)) },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onToggleSaveCourse = { course, saved -> viewModel.toggleSavedCourse(course, saved) },
                onToggleCompare = { courseId -> viewModel.toggleCompareCourse(courseId) },
                onClearCompare = { viewModel.clearCompareCourses() }
              )
            }

            composable<MyLearningRoute> {
              MyLearningScreen(
                catalogState = catalogState,
                studentState = studentState,
                onOpenExplore = { navigateTopLevel(ExploreRoute) },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onResumeLesson = { courseId, lessonId ->
                  navController.navigate(LessonPlayerRoute(courseId, lessonId))
                },
                onOpenLearningPath = { pathId -> navController.navigate(LearningPathRoute(pathId)) },
                onToggleSaveCourse = { course, saved -> viewModel.toggleSavedCourse(course, saved) }
              )
            }

            composable<CertificatesRoute> {
              CertificatesScreen(
                studentState = studentState,
                verifiedResult = verifiedCertificateResult,
                onVerifyCertificate = { code -> viewModel.verifyCertificate(code) },
                onExportPdf = { cert -> viewModel.exportAndShareCertificatePdf(cert) },
                onOpenExplore = { navigateTopLevel(ExploreRoute) }
              )
            }

            composable<ProfileRoute> {
              ProfileScreen(
                viewModel = viewModel,
                studentState = studentState,
                isAdmin = isAdmin,
                onOpenSubscription = { navController.navigate(SubscriptionRoute) },
                onOpenAdmin = { navController.navigate(AdminDashboardRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onOpenUserProfile = { navController.navigate(UserProfileRoute) }
              )
            }

            composable<UserProfileRoute> {
              UserProfileScreen(
                onBack = { navController.popBackStack() }
              )
            }

            composable<AcademyDetailRoute> { entry ->
              val route = entry.toRoute<AcademyDetailRoute>()
              AcademyDetailScreen(
                academyId = route.academyId,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onResumeLesson = { courseId, lessonId ->
                  navController.navigate(LessonPlayerRoute(courseId, lessonId))
                },
                onOpenLearningPath = { pathId -> navController.navigate(LearningPathRoute(pathId)) },
                onToggleSaveCourse = { course, saved -> viewModel.toggleSavedCourse(course, saved) },
                onOpenCertificates = { navigateTopLevel(CertificatesRoute) },
                onClaimSpecialization = { viewModel.claimAcademySpecializationCertificate() }
              )
            }

            composable<CourseDetailRoute> { entry ->
              val route = entry.toRoute<CourseDetailRoute>()
              CourseDetailScreen(
                courseId = route.courseId,
                viewModel = viewModel,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() },
                onOpenLesson = { cid, lid -> navController.navigate(LessonPlayerRoute(cid, lid)) },
                onOpenQuiz = { qid, cid -> navController.navigate(QuizRoute(qid, cid)) },
                onOpenSubscription = { navController.navigate(SubscriptionRoute) },
                onOpenCertificates = { navigateTopLevel(CertificatesRoute) }
              )
            }

            composable<LessonPlayerRoute> { entry ->
              val route = entry.toRoute<LessonPlayerRoute>()
              LessonPlayerScreen(
                courseId = route.courseId,
                lessonId = route.lessonId,
                viewModel = viewModel,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() },
                onNavigateLesson = { nextLessonId ->
                  navController.navigate(LessonPlayerRoute(route.courseId, nextLessonId)) {
                    popUpTo<LessonPlayerRoute> { inclusive = true }
                  }
                },
                onOpenSubscription = { navController.navigate(SubscriptionRoute) },
                onOpenCertificates = { navigateTopLevel(CertificatesRoute) }
              )
            }

            composable<QuizRoute> { entry ->
              val route = entry.toRoute<QuizRoute>()
              QuizScreen(
                quizId = route.quizId,
                courseId = route.courseId,
                viewModel = viewModel,
                studentState = studentState,
                onBack = { navController.popBackStack() }
              )
            }

            composable<LearningPathRoute> { entry ->
              val route = entry.toRoute<LearningPathRoute>()
              LearningPathScreen(
                pathId = route.pathId,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onResumeLesson = { courseId, lessonId ->
                  navController.navigate(LessonPlayerRoute(courseId, lessonId))
                },
                onOpenCertificates = { navigateTopLevel(CertificatesRoute) }
              )
            }

            composable<SubscriptionRoute> {
              SubscriptionScreen(
                viewModel = viewModel,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() }
              )
            }

            composable<NotificationsRoute> {
              NotificationsScreen(
                viewModel = viewModel,
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() }
              )
            }

            composable<AdminDashboardRoute> {
              AdminDashboardScreen(
                viewModel = viewModel,
                catalogState = catalogState,
                isAdmin = isAdmin,
                isSeedingCatalog = isSeedingCatalog,
                onBack = { navController.popBackStack() }
              )
            }

            composable<CareerHubRoute> {
              CareerHubScreen(
                catalogState = catalogState,
                studentState = studentState,
                onBack = { navController.popBackStack() },
                onOpenCourse = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
                onOpenLearningPath = { pathId -> navController.navigate(LearningPathRoute(pathId)) },
                onOpenCertificates = { navigateTopLevel(CertificatesRoute) },
                onToggleSaveCourse = { course, saved -> viewModel.toggleSavedCourse(course, saved) }
              )
            }
          }
        }
      }
    }
  }

  if (authPrefs.loadedFromDisk && !authPrefs.rememberPromptHandled) {
    AlertDialog(
      onDismissRequest = {
        authPreferences.setRememberMeAsync(
          enabled = true,
          markPromptHandled = true,
          email = userEmail,
          displayName = userName
        )
      },
      title = { Text("Remember you on this device?") },
      text = {
        Text(
          text = "Keep ${userEmail ?: "your Google account"} signed in across app launches for 1-tap access, or stay signed in only for this session."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            authPreferences.setRememberMeAsync(
              enabled = true,
              markPromptHandled = true,
              email = userEmail,
              displayName = userName
            )
          },
          modifier = Modifier.testTag("remember_me_confirm_button")
        ) {
          Text("Remember me")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            authPreferences.setRememberMeAsync(
              enabled = false,
              markPromptHandled = true
            )
          },
          modifier = Modifier.testTag("remember_me_session_only_button")
        ) {
          Text("This session only")
        }
      }
    )
  }
}
