package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "datalabs_auth_prefs",
  corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

data class AuthPreferencesState(
  val rememberMe: Boolean = true,
  val rememberPromptHandled: Boolean = false,
  val rememberedEmail: String? = null,
  val rememberedDisplayName: String? = null,
  val loadedFromDisk: Boolean = false
)

object RememberMePolicy {
  fun shouldSignOutOnColdStart(
    hasSignedInUser: Boolean,
    prefs: AuthPreferencesState
  ): Boolean {
    return hasSignedInUser && prefs.loadedFromDisk && !prefs.rememberMe
  }

  fun shouldAttemptAutoSignIn(
    prefs: AuthPreferencesState,
    suppressForSession: Boolean
  ): Boolean {
    return prefs.loadedFromDisk &&
      prefs.rememberMe &&
      prefs.rememberPromptHandled &&
      !prefs.rememberedEmail.isNullOrBlank() &&
      !suppressForSession
  }

  fun applyRememberMeChange(
    current: AuthPreferencesState,
    enabled: Boolean,
    markPromptHandled: Boolean,
    email: String? = null,
    displayName: String? = null
  ): AuthPreferencesState {
    return current.copy(
      rememberMe = enabled,
      rememberPromptHandled = if (markPromptHandled) true else current.rememberPromptHandled,
      rememberedEmail = if (enabled) {
        email?.trim()?.takeIf { it.isNotEmpty() } ?: current.rememberedEmail
      } else {
        null
      },
      rememberedDisplayName = if (enabled) {
        displayName?.trim()?.takeIf { it.isNotEmpty() } ?: current.rememberedDisplayName
      } else {
        null
      }
    )
  }
}

class AuthPreferencesRepository(
  private val context: Context,
  ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  private companion object {
    const val TAG = "AuthPreferences"
  }

  private object Keys {
    val REMEMBER_ME = booleanPreferencesKey("remember_me")
    val REMEMBER_PROMPT_HANDLED = booleanPreferencesKey("remember_prompt_handled")
    val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
    val REMEMBERED_DISPLAY_NAME = stringPreferencesKey("remembered_display_name")
  }

  private val repoScope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val stateLock = Any()
  private var mutationVersion: Long = 0L
  private var pendingWrites: Int = 0

  private val initialLoadDeferred = CompletableDeferred<AuthPreferencesState>()
  private val coldStartEvaluated = AtomicBoolean(false)

  private val _stateFlow = MutableStateFlow(AuthPreferencesState())
  val stateFlow: StateFlow<AuthPreferencesState> = _stateFlow.asStateFlow()

  // Preserved alias for callers collecting Flow<AuthPreferencesState>
  val state: Flow<AuthPreferencesState>
    get() = stateFlow

  val coldStartChecked: Boolean
    get() = coldStartEvaluated.get()

  @Volatile
  var suppressAutoSignInThisSession: Boolean = false
    private set

  init {
    startObservingDisk()
  }

  private fun startObservingDisk() {
    repoScope.launch {
      context.authDataStore.data
        .catch { exception ->
          if (exception is CancellationException) throw exception
          Log.w(TAG, "Falling back to empty auth preferences after read failure", exception)
          emit(emptyPreferences())
        }
        .map { prefs -> prefs.toAuthState() }
        .collect { diskState ->
          synchronized(stateLock) {
            if (pendingWrites == 0) {
              _stateFlow.value = diskState
            } else {
              // Ensure loadedFromDisk propagates even while a user write is in flight.
              _stateFlow.value = _stateFlow.value.copy(loadedFromDisk = true)
            }
          }
          if (!initialLoadDeferred.isCompleted) {
            initialLoadDeferred.complete(_stateFlow.value)
          }
        }
    }
  }

  suspend fun awaitLoaded(timeoutMs: Long = 450L): AuthPreferencesState {
    if (_stateFlow.value.loadedFromDisk) return _stateFlow.value
    return withTimeoutOrNull(timeoutMs) {
      initialLoadDeferred.await()
    } ?: _stateFlow.value
  }

  suspend fun readCurrent(): AuthPreferencesState = awaitLoaded()

  /**
   * Runs once per cold app process launch on Dispatchers.IO.
   * If the user previously chose "session only" (rememberMe == false), signs out the stale Firebase session
   * so they are cleanly prompted to sign in again when reopening the app.
   */
  suspend fun evaluateColdStartSession(
    hasSignedInUser: Boolean,
    onSignOutRequired: () -> Unit
  ): AuthPreferencesState {
    val current = awaitLoaded()
    if (current.loadedFromDisk && coldStartEvaluated.compareAndSet(false, true)) {
      if (RememberMePolicy.shouldSignOutOnColdStart(hasSignedInUser, current)) {
        suppressAutoSignInThisSession = true
        runCatching { onSignOutRequired() }
      }
    }
    return _stateFlow.value
  }

  /**
   * Optimistically updates Compose state synchronously in 0ms and persists atomically on repoScope
   * so caller coroutine cancellation (e.g., AuthScreen exiting composition) never drops the write.
   */
  fun setRememberMeAsync(
    enabled: Boolean,
    markPromptHandled: Boolean = true,
    email: String? = null,
    displayName: String? = null
  ) {
    applyOptimisticUpdate { current ->
      RememberMePolicy.applyRememberMeChange(
        current = current,
        enabled = enabled,
        markPromptHandled = markPromptHandled,
        email = email,
        displayName = displayName
      )
    }
    persistAsync { prefs ->
      prefs[Keys.REMEMBER_ME] = enabled
      if (markPromptHandled) {
        prefs[Keys.REMEMBER_PROMPT_HANDLED] = true
      }
      if (enabled) {
        if (!email.isNullOrBlank()) prefs[Keys.REMEMBERED_EMAIL] = email.trim()
        if (!displayName.isNullOrBlank()) prefs[Keys.REMEMBERED_DISPLAY_NAME] = displayName.trim()
      } else {
        prefs.remove(Keys.REMEMBERED_EMAIL)
        prefs.remove(Keys.REMEMBERED_DISPLAY_NAME)
      }
    }
  }

  suspend fun setRememberMe(
    enabled: Boolean,
    markPromptHandled: Boolean = true,
    email: String? = null,
    displayName: String? = null
  ) {
    applyOptimisticUpdate { current ->
      RememberMePolicy.applyRememberMeChange(
        current = current,
        enabled = enabled,
        markPromptHandled = markPromptHandled,
        email = email,
        displayName = displayName
      )
    }
    persistSuspend { prefs ->
      prefs[Keys.REMEMBER_ME] = enabled
      if (markPromptHandled) {
        prefs[Keys.REMEMBER_PROMPT_HANDLED] = true
      }
      if (enabled) {
        if (!email.isNullOrBlank()) prefs[Keys.REMEMBERED_EMAIL] = email.trim()
        if (!displayName.isNullOrBlank()) prefs[Keys.REMEMBERED_DISPLAY_NAME] = displayName.trim()
      } else {
        prefs.remove(Keys.REMEMBERED_EMAIL)
        prefs.remove(Keys.REMEMBERED_DISPLAY_NAME)
      }
    }
  }

  fun saveSignedInSessionAsync(
    email: String?,
    displayName: String?,
    rememberMe: Boolean
  ) {
    suppressAutoSignInThisSession = false
    coldStartEvaluated.set(true)
    setRememberMeAsync(
      enabled = rememberMe,
      markPromptHandled = true,
      email = email,
      displayName = displayName
    )
  }

  suspend fun saveSignedInSession(
    email: String?,
    displayName: String?,
    rememberMe: Boolean
  ) {
    suppressAutoSignInThisSession = false
    coldStartEvaluated.set(true)
    setRememberMe(
      enabled = rememberMe,
      markPromptHandled = true,
      email = email,
      displayName = displayName
    )
  }

  fun clearRememberedAccountAsync() {
    applyOptimisticUpdate { current ->
      current.copy(rememberedEmail = null, rememberedDisplayName = null)
    }
    persistAsync { prefs ->
      prefs.remove(Keys.REMEMBERED_EMAIL)
      prefs.remove(Keys.REMEMBERED_DISPLAY_NAME)
    }
  }

  suspend fun clearRememberedAccount() {
    applyOptimisticUpdate { current ->
      current.copy(rememberedEmail = null, rememberedDisplayName = null)
    }
    persistSuspend { prefs ->
      prefs.remove(Keys.REMEMBERED_EMAIL)
      prefs.remove(Keys.REMEMBERED_DISPLAY_NAME)
    }
  }

  fun markExplicitSignOut() {
    suppressAutoSignInThisSession = true
  }

  private fun applyOptimisticUpdate(transform: (AuthPreferencesState) -> AuthPreferencesState) {
    synchronized(stateLock) {
      mutationVersion++
      _stateFlow.value = transform(_stateFlow.value)
    }
  }

  private fun persistAsync(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    synchronized(stateLock) { pendingWrites++ }
    repoScope.launch {
      persistInternal(block)
    }
  }

  private suspend fun persistSuspend(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    synchronized(stateLock) { pendingWrites++ }
    withContext(repoScope.coroutineContext + NonCancellable) {
      persistInternal(block)
    }
  }

  private suspend fun persistInternal(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    try {
      withContext(NonCancellable) {
        runCatching {
          context.authDataStore.edit(block)
        }.onFailure { t ->
          Log.w(TAG, "Failed to persist auth preferences", t)
        }
      }
    } finally {
      synchronized(stateLock) {
        pendingWrites = (pendingWrites - 1).coerceAtLeast(0)
      }
    }
  }

  private fun Preferences.toAuthState(): AuthPreferencesState {
    return AuthPreferencesState(
      rememberMe = this[Keys.REMEMBER_ME] ?: true,
      rememberPromptHandled = this[Keys.REMEMBER_PROMPT_HANDLED] ?: false,
      rememberedEmail = this[Keys.REMEMBERED_EMAIL],
      rememberedDisplayName = this[Keys.REMEMBERED_DISPLAY_NAME],
      loadedFromDisk = true
    )
  }
}
