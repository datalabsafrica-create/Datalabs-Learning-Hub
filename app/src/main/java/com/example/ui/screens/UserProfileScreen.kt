package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.DatalabsApplication
import com.example.R
import com.example.data.repository.AuthManager
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private data class FirebaseUserProfileSnapshot(
  val uid: String,
  val displayName: String,
  val email: String,
  val photoUrl: String?,
  val isEmailVerified: Boolean,
  val providerLabel: String,
  val createdAtMillis: Long?
)

private fun FirebaseUser.toProfileSnapshot(
  fallbackName: String,
  fallbackEmail: String
): FirebaseUserProfileSnapshot {
  val resolvedProviderId = providerData
    .firstOrNull { it.providerId != "firebase" }
    ?.providerId
    ?: providerId
  val formattedProvider = when (resolvedProviderId) {
    "google.com" -> "Google"
    "password" -> "Email & Password"
    else -> "Firebase Auth"
  }
  return FirebaseUserProfileSnapshot(
    uid = uid,
    displayName = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackName,
    email = email?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackEmail,
    photoUrl = photoUrl?.toString()?.takeIf { it.isNotBlank() },
    isEmailVerified = isEmailVerified,
    providerLabel = formattedProvider,
    createdAtMillis = metadata?.creationTimestamp?.takeIf { it > 0L }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
  modifier: Modifier = Modifier,
  auth: FirebaseAuth? = remember { runCatching { Firebase.auth }.getOrNull() },
  onBack: (() -> Unit)? = null,
  onSignOutComplete: () -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val fallbackName = stringResource(R.string.user_profile_fallback_name)
  val fallbackEmail = stringResource(R.string.user_profile_fallback_email)

  val initialUser = remember(auth) { runCatching { auth?.currentUser }.getOrNull() }
  val authFlow = remember(auth) {
    runCatching { AuthManager.authStateFlow(auth) }
      .getOrElse { flowOf(initialUser) }
  }
  val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = initialUser)

  var credentialManager by remember { mutableStateOf<CredentialManager?>(null) }
  LaunchedEffect(context) {
    if (credentialManager == null) {
      credentialManager = withContext(Dispatchers.IO) {
        runCatching { CredentialManager.create(context) }.getOrNull()
      }
    }
  }

  var refreshTrigger by remember { mutableIntStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }
  var isSigningOut by remember { mutableStateOf(false) }
  var isSigningIn by remember { mutableStateOf(false) }
  var syncNotice by remember { mutableStateOf<String?>(null) }
  var profileSnapshot by remember(currentUser?.uid, fallbackName, fallbackEmail) {
    mutableStateOf(currentUser?.toProfileSnapshot(fallbackName, fallbackEmail))
  }

  val cachedNoticeText = stringResource(R.string.user_profile_sync_cached_notice)

  LaunchedEffect(currentUser?.uid, refreshTrigger) {
    val activeUser = currentUser
    if (activeUser == null) {
      profileSnapshot = null
      syncNotice = null
      isRefreshing = false
      return@LaunchedEffect
    }
    profileSnapshot = activeUser.toProfileSnapshot(fallbackName, fallbackEmail)
    isRefreshing = true
    syncNotice = null
    try {
      withContext(Dispatchers.IO) {
        activeUser.reload().await()
      }
      val reloadedUser = auth?.currentUser ?: activeUser
      profileSnapshot = reloadedUser.toProfileSnapshot(fallbackName, fallbackEmail)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      syncNotice = cachedNoticeText
    } finally {
      isRefreshing = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .testTag("user_profile_screen")
  ) {
    TopAppBar(
      title = {
        Text(
          text = stringResource(R.string.user_profile_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      },
      navigationIcon = {
        if (onBack != null) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("user_profile_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.user_profile_back_description)
            )
          }
        }
      },
      actions = {
        if (currentUser != null) {
          IconButton(
            onClick = { refreshTrigger++ },
            enabled = !isRefreshing && !isSigningOut,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("user_profile_refresh_button")
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = stringResource(R.string.user_profile_refresh_description)
            )
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background
      )
    )

    AnimatedVisibility(visible = isRefreshing) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .align(Alignment.TopCenter),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        val snapshot = profileSnapshot
        if (snapshot == null) {
          item {
            Card(
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.AccountCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(56.dp)
                )
                Text(
                  text = stringResource(R.string.user_profile_signed_out_title),
                  style = MaterialTheme.typography.headlineSmall
                )
                Text(
                  text = stringResource(R.string.user_profile_signed_out_subtitle),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(visible = syncNotice != null) {
                  Text(
                    text = syncNotice.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                  )
                }
                Button(
                  onClick = {
                    isSigningIn = true
                    syncNotice = null
                    scope.launchSignIn(
                      context = context,
                      credentialManager = credentialManager,
                      onCredentialManagerReady = { credentialManager = it },
                      onSuccess = {
                        isSigningIn = false
                        refreshTrigger++
                      },
                      onCancelled = { isSigningIn = false },
                      onError = { msg ->
                        isSigningIn = false
                        syncNotice = msg
                      }
                    )
                  },
                  enabled = !isSigningIn,
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("user_profile_sign_in_button")
                ) {
                  if (isSigningIn) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.dp
                    )
                  } else {
                    Text(stringResource(R.string.user_profile_sign_in_google))
                  }
                }
              }
            }
          }
        } else {
          item {
            UserIdentityHeroCard(snapshot = snapshot)
          }

          syncNotice?.let { notice ->
            item {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = notice,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
              }
            }
          }

          item {
            UserAccountDetailsCard(snapshot = snapshot)
          }

          item {
            Button(
              onClick = {
                isSigningOut = true
                (context.applicationContext as? DatalabsApplication)
                  ?.container
                  ?.authPreferencesRepository
                  ?.markExplicitSignOut()
                scope.launchSignOut(
                  context = context,
                  auth = auth,
                  credentialManager = credentialManager,
                  onCredentialManagerReady = { credentialManager = it },
                  onComplete = {
                    isSigningOut = false
                    onSignOutComplete()
                  }
                )
              },
              enabled = !isSigningOut,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              ),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 52.dp)
                .testTag("sign_out_button")
            ) {
              if (isSigningOut) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onError,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.user_profile_signing_out))
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.user_profile_sign_out))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UserIdentityHeroCard(snapshot: FirebaseUserProfileSnapshot) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = NavyDeep)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(NavyDeep, NavySurface, NavyElevated)
          )
        )
        .drawBehind {
          drawCircle(
            color = ElectricBlueBright.copy(alpha = 0.22f),
            radius = size.minDimension * 0.65f,
            center = Offset(size.width * 0.9f, size.height * 0.15f)
          )
        }
        .padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ElectricBlue),
          contentAlignment = Alignment.Center
        ) {
          if (!snapshot.photoUrl.isNullOrBlank()) {
            AsyncImage(
              model = snapshot.photoUrl,
              contentDescription = stringResource(R.string.user_profile_avatar_description),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Text(
              text = snapshot.displayName.firstOrNull()?.uppercase() ?: "U",
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = snapshot.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("user_profile_display_name")
          )
          Text(
            text = snapshot.email,
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricBlueSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("user_profile_email")
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = if (snapshot.isEmailVerified) {
                SuccessEmerald.copy(alpha = 0.2f)
              } else {
                WarningGold.copy(alpha = 0.2f)
              },
              shape = RoundedCornerShape(999.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = if (snapshot.isEmailVerified) SuccessEmerald else WarningGold,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = if (snapshot.isEmailVerified) {
                    stringResource(R.string.user_profile_verified)
                  } else {
                    stringResource(R.string.user_profile_unverified)
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White
                )
              }
            }
            Surface(
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(999.dp)
            ) {
              Text(
                text = snapshot.providerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UserAccountDetailsCard(snapshot: FirebaseUserProfileSnapshot) {
  val memberSinceFormatted = remember(snapshot.createdAtMillis) {
    snapshot.createdAtMillis?.let { millis ->
      runCatching { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis)) }.getOrNull()
    }
  }

  Card(
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        text = stringResource(R.string.user_profile_details_heading),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      ProfileDetailRow(
        icon = Icons.Outlined.Person,
        label = stringResource(R.string.user_profile_display_name_label),
        value = snapshot.displayName
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      ProfileDetailRow(
        icon = Icons.Outlined.Email,
        label = stringResource(R.string.user_profile_email_label),
        value = snapshot.email
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      ProfileDetailRow(
        icon = Icons.Outlined.VerifiedUser,
        label = stringResource(R.string.user_profile_status_label),
        value = if (snapshot.isEmailVerified) {
          stringResource(R.string.user_profile_verified)
        } else {
          stringResource(R.string.user_profile_unverified)
        }
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      ProfileDetailRow(
        icon = Icons.Outlined.Badge,
        label = stringResource(R.string.user_profile_provider_label),
        value = snapshot.providerLabel
      )

      if (memberSinceFormatted != null) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ProfileDetailRow(
          icon = Icons.Outlined.CalendarToday,
          label = stringResource(R.string.user_profile_member_since_label),
          value = memberSinceFormatted
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      ProfileDetailRow(
        icon = Icons.Outlined.AccountCircle,
        label = stringResource(R.string.user_profile_uid_label),
        value = snapshot.uid
      )
    }
  }
}

@Composable
private fun ProfileDetailRow(
  icon: ImageVector,
  label: String,
  value: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.size(38.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(20.dp)
        )
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

private fun kotlinx.coroutines.CoroutineScope.launchSignIn(
  context: android.content.Context,
  credentialManager: CredentialManager?,
  onCredentialManagerReady: (CredentialManager) -> Unit,
  onSuccess: () -> Unit,
  onCancelled: () -> Unit,
  onError: (String) -> Unit
) {
  launch {
    val cm = credentialManager ?: withContext(Dispatchers.IO) {
      runCatching { CredentialManager.create(context) }.getOrNull()
    }?.also(onCredentialManagerReady)
    AuthManager.onGoogleSignInClicked(
      context = context,
      credentialManager = cm,
      onAuthSuccess = { onSuccess() },
      onAuthError = onError,
      scope = this,
      onAuthCancelled = onCancelled
    )
  }
}

private fun kotlinx.coroutines.CoroutineScope.launchSignOut(
  context: android.content.Context,
  auth: FirebaseAuth?,
  credentialManager: CredentialManager?,
  onCredentialManagerReady: (CredentialManager) -> Unit,
  onComplete: () -> Unit
) {
  launch {
    val cm = credentialManager ?: withContext(Dispatchers.IO) {
      runCatching { CredentialManager.create(context) }.getOrNull()
    }?.also(onCredentialManagerReady)
    AuthManager.signOut(
      credentialManager = cm,
      scope = this,
      onSignOutComplete = {
        runCatching { auth?.signOut() }
        onComplete()
      }
    )
  }
}
