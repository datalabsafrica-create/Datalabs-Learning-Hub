package com.example

import android.app.Application
import android.util.Log
import com.example.data.ai.AiLearningAssistantService
import com.example.data.billing.PlayBillingManager
import com.example.data.firebase.AnalyticsLogger
import com.example.data.firebase.LearningNotificationHelper
import com.example.data.firebase.MaterialAndCertificateStorage
import com.example.data.firebase.configureAppCheck
import com.example.data.repository.AuthPreferencesRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.StudentRepository
import com.example.data.seed.CatalogSeeder
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AppContainer(application: Application) {
  private val app = application
  private val databaseId: String by lazy { app.getString(R.string.firestore_database_id) }

  val firestore: FirebaseFirestore by lazy {
    runCatching {
      if (FirebaseApp.getApps(app).isEmpty()) {
        FirebaseApp.initializeApp(app)
      }
    }
    FirebaseFirestore.getInstance(databaseId).apply {
      runCatching {
        firestoreSettings = firestoreSettings {
          setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
      }
    }
  }

  val authPreferencesRepository by lazy { AuthPreferencesRepository(app) }
  val catalogRepository by lazy { CatalogRepository(firestore) }
  val studentRepository by lazy { StudentRepository(firestore) }
  val catalogSeeder by lazy { CatalogSeeder(firestore) }
  val analyticsLogger by lazy { AnalyticsLogger(app) }
  val storageAndPdf by lazy { MaterialAndCertificateStorage(app) }
  val notificationHelper by lazy { LearningNotificationHelper(app) }
  val aiAssistantService by lazy { AiLearningAssistantService() }
  val playBillingManager by lazy { PlayBillingManager(app) }
}

class DatalabsApplication : Application() {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val startupWarmup = CompletableDeferred<Unit>()

  lateinit var container: AppContainer
    private set

  fun isStartupWarmupComplete(): Boolean = startupWarmup.isCompleted

  suspend fun awaitStartupWarmup(timeoutMs: Long = 450L) {
    if (startupWarmup.isCompleted) return
    withTimeoutOrNull(timeoutMs) {
      startupWarmup.await()
    }
  }

  override fun onCreate() {
    super.onCreate()
    runCatching {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
    }.onFailure { t ->
      Log.e("DatalabsApp", "Synchronous FirebaseApp initialization error", t)
    }
    container = AppContainer(this)
    // Pre-warm App Check and Remember-Me preferences on IO so MainActivity renders frame 1 immediately.
    appScope.launch {
      try {
        configureAppCheck(this@DatalabsApplication, null)

        val firebaseAuth = runCatching { Firebase.auth }.getOrNull()
        val hasUser = runCatching { firebaseAuth?.currentUser != null }.getOrDefault(false)
        container.authPreferencesRepository.evaluateColdStartSession(
          hasSignedInUser = hasUser,
          onSignOutRequired = { runCatching { firebaseAuth?.signOut() } }
        )
      } catch (t: Throwable) {
        Log.e("DatalabsApp", "Startup warmup error", t)
      } finally {
        startupWarmup.complete(Unit)
      }
      runCatching { container.notificationHelper.ensureChannels() }
    }
  }
}
