package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthManager
import com.example.data.repository.AuthPreferencesRepository
import com.example.data.repository.RememberMePolicy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
  authPreferences: AuthPreferencesRepository,
  onAuthSuccess: (user: FirebaseUser?, rememberMe: Boolean) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var credentialManager by remember { mutableStateOf<CredentialManager?>(null) }

  val savedPrefs by authPreferences.stateFlow.collectAsStateWithLifecycle()
  val rememberMe = savedPrefs.rememberMe
  var isSigningIn by remember { mutableStateOf(false) }
  var isCheckingQuickSignIn by remember { mutableStateOf(false) }
  var autoSignInAttempted by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(context) {
    if (credentialManager == null) {
      credentialManager = withContext(Dispatchers.IO) {
        runCatching { CredentialManager.create(context) }.getOrNull()
      }
    }
  }

  LaunchedEffect(savedPrefs.loadedFromDisk, credentialManager) {
    val cm = credentialManager ?: return@LaunchedEffect
    if (!savedPrefs.loadedFromDisk || autoSignInAttempted) return@LaunchedEffect
    autoSignInAttempted = true
    if (!RememberMePolicy.shouldAttemptAutoSignIn(savedPrefs, authPreferences.suppressAutoSignInThisSession)) {
      return@LaunchedEffect
    }
    // Allow initial window surface sync to complete before launching Play Services credential IPC.
    delay(350L)
    val latestPrefs = authPreferences.stateFlow.value
    if (isSigningIn || !RememberMePolicy.shouldAttemptAutoSignIn(latestPrefs, authPreferences.suppressAutoSignInThisSession)) {
      return@LaunchedEffect
    }
    isCheckingQuickSignIn = true
    AuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = cm,
      onAuthSuccess = { user ->
        isCheckingQuickSignIn = false
        onAuthSuccess(user, authPreferences.stateFlow.value.rememberMe)
      },
      onUnauthenticated = {
        isCheckingQuickSignIn = false
      },
      scope = scope
    )
  }

  val rememberedName = savedPrefs.rememberedDisplayName?.takeIf { it.isNotBlank() }
  val rememberedEmail = savedPrefs.rememberedEmail?.takeIf { it.isNotBlank() }
  val firstName = rememberedName?.substringBefore(" ")?.takeIf { it.isNotBlank() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyDeep)
      .drawBehind {
        if (size.minDimension > 0f) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(ElectricBlue.copy(alpha = 0.22f), Color.Transparent),
              center = Offset(size.width * 0.85f, size.height * 0.12f),
              radius = (size.maxDimension * 0.65f).coerceAtLeast(1f)
            )
          )
        }
      }
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Compact Brand Identity
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ElectricBlue
        ) {
          Icon(
            imageVector = Icons.Filled.School,
            contentDescription = "Datalabs Learning Hub logo",
            tint = Color.White,
            modifier = Modifier
              .padding(10.dp)
              .size(26.dp)
          )
        }
        Column {
          Text(
            text = "DATALABS LEARNING HUB",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "AI Academy • Practical Skills",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF93C5FD)
          )
        }
      }

      // Concise Headline
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (firstName != null) "Welcome back, $firstName" else "Sign in to start learning",
          style = MaterialTheme.typography.displayMedium,
          color = Color.White
        )
        Text(
          text = "One tap with Google unlocks your AI Academy courses, progress sync, and certificates.",
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFFCBD5E1)
        )
      }

      // Streamlined Sign-In Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Saved / Remembered Account Banner (when user previously opted into Remember Me)
          AnimatedVisibility(visible = rememberedEmail != null || rememberedName != null) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = NavyElevated,
              border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue),
                  contentAlignment = Alignment.Center
                ) {
                  val initial = (rememberedName ?: rememberedEmail ?: "D").first().uppercase()
                  Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = rememberedName ?: "Remembered Account",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (rememberedEmail != null) {
                    Text(
                      text = rememberedEmail,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF93C5FD),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                TextButton(
                  onClick = {
                    authPreferences.clearRememberedAccountAsync()
                  },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("switch_remembered_account_button")
                ) {
                  Text("Switch", color = Color(0xFF93C5FD))
                }
              }
            }
          }

          // Remember Me Prompt Toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                authPreferences.setRememberMeAsync(
                  enabled = !rememberMe,
                  markPromptHandled = false
                )
              }
              .padding(vertical = 4.dp)
              .testTag("remember_me_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.BookmarkAdded,
              contentDescription = null,
              tint = if (rememberMe) Color(0xFF60A5FA) else Color(0xFF94A3B8),
              modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Remember me",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
              )
              Text(
                text = if (rememberMe) {
                  "Stay signed in automatically next time"
                } else {
                  "Sign out automatically when the app closes"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
              )
            }
            Switch(
              checked = rememberMe,
              onCheckedChange = { checked ->
                authPreferences.setRememberMeAsync(
                  enabled = checked,
                  markPromptHandled = false
                )
              },
              modifier = Modifier.testTag("remember_me_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricBlue
              )
            )
          }

          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

          AnimatedVisibility(visible = errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Button(
            onClick = {
              errorMessage = null
              isSigningIn = true
              scope.launch {
                val cm = credentialManager ?: withContext(Dispatchers.IO) {
                  runCatching { CredentialManager.create(context) }.getOrNull()
                }?.also { credentialManager = it }
                AuthManager.onGoogleSignInClicked(
                  context = context,
                  credentialManager = cm,
                  onAuthSuccess = { user ->
                    isSigningIn = false
                    onAuthSuccess(user, authPreferences.stateFlow.value.rememberMe)
                  },
                  onAuthError = { msg ->
                    isSigningIn = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = {
                    isSigningIn = false
                  }
                )
              }
            },
            enabled = !isSigningIn,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ElectricBlue,
              contentColor = Color.White
            )
          ) {
            if (isSigningIn) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.size(10.dp))
              Text("Signing in...")
            } else {
              Text(
                text = if (firstName != null) "Continue as $firstName" else "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (isCheckingQuickSignIn && !isSigningIn) {
            Text(
              text = "Checking saved Google session...",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF93C5FD)
            )
          }
        }
      }

      Text(
        text = "Your learning history, quizzes, and certificates are secured with your Google account.",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8)
      )
    }
  }
}
