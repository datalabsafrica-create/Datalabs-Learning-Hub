package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

object AuthManager {
  private const val TAG = "Auth"
  private const val AUTO_SIGN_IN_TIMEOUT_MS = 5_000L
  private const val FIREBASE_EXCHANGE_TIMEOUT_MS = 15_000L
  private const val SIGN_OUT_CLEAR_TIMEOUT_MS = 3_000L
  const val BOOTSTRAP_ADMIN_EMAIL = "cm776773934@gmail.com"

  fun authStateFlow(auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()): Flow<FirebaseUser?> = callbackFlow {
    if (auth == null) {
      trySend(null)
      close()
      return@callbackFlow
    }
    trySend(auth.currentUser)
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    val existingUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    if (existingUser != null) {
      onAuthSuccess(existingUser)
      return
    }
    val activityContext = context.findActivity()
    if (credentialManager == null || activityContext == null) {
      onUnauthenticated()
      return
    }
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Throwable) {
      onUnauthenticated()
      return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val cred = withTimeout(AUTO_SIGN_IN_TIMEOUT_MS) {
          credentialManager.getCredential(activityContext, request).credential
        }
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          val result = withTimeout(FIREBASE_EXCHANGE_TIMEOUT_MS) {
            Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
          }
          onAuthSuccess(result.user ?: Firebase.auth.currentUser)
        } else {
          onUnauthenticated()
        }
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Auto sign-in timed out: ${e.message}")
        onUnauthenticated()
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
        Log.d(TAG, "Auto sign-in skipped: ${t.message}")
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    if (credentialManager == null) {
      onAuthError("Google Credential Manager is unavailable on this device.")
      return
    }
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Throwable) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val activityContext = context.findActivity()
          ?: run {
            onAuthError("Unable to resolve Activity for Google Sign-In")
            return@launch
          }
        val cred = credentialManager.getCredential(activityContext, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          val result = withTimeout(FIREBASE_EXCHANGE_TIMEOUT_MS) {
            Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
          }
          onAuthSuccess(result.user ?: Firebase.auth.currentUser)
        } else {
          onAuthError("Unexpected credential type returned")
        }
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Firebase sign-in timed out", e)
        onAuthError("Sign in timed out. Please check your internet connection and try again.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: NoCredentialException) {
        Log.w(TAG, "No Google credential available", e)
        onAuthError("No Google account found on this device. Add a Google account in Settings and try again.")
      } catch (t: Throwable) {
        Log.e(TAG, "Google Sign-In failed", t)
        onAuthError(t.localizedMessage ?: "Sign in failed. Please try again.")
      }
    }
  }

  fun signOut(
    credentialManager: CredentialManager?,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          withTimeoutOrNull(SIGN_OUT_CLEAR_TIMEOUT_MS) {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
          }
        } catch (t: Throwable) {
          Log.w(TAG, "Failed to clear credential state: ${t.message}")
        } finally {
          runCatching { Firebase.auth.signOut() }
          onSignOutComplete()
        }
      }
    }
  }
}
