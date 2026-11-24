package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.DatalabsApplication
import com.example.data.repository.AuthManager
import com.example.data.model.UiState
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  viewModel: MainViewModel,
  studentState: UiState<StudentDataSnapshot>,
  isAdmin: Boolean,
  onOpenSubscription: () -> Unit,
  onOpenAdmin: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  if (studentState is UiState.Loading) {
    LoadingStateView("Loading profile...")
    return
  }
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val profile = student.profile
  val context = LocalContext.current
  val app = context.applicationContext as DatalabsApplication
  val authPreferences = app.container.authPreferencesRepository
  val authPrefs by authPreferences.stateFlow.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  var credentialManager by remember { mutableStateOf<CredentialManager?>(null) }
  LaunchedEffect(context) {
    if (credentialManager == null) {
      credentialManager = withContext(Dispatchers.IO) {
        runCatching { CredentialManager.create(context) }.getOrNull()
      }
    }
  }

  var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
  var learningGoal by remember(profile) { mutableStateOf(profile?.learningGoal ?: "") }
  var weeklyHours by remember(profile) { mutableLongStateOf(profile?.weeklyTargetHours ?: 5L) }
  var reminders by remember(profile) { mutableStateOf(profile?.remindersEnabled ?: true) }
  var courseUpdates by remember(profile) { mutableStateOf(profile?.courseUpdatesEnabled ?: true) }
  var announcements by remember(profile) { mutableStateOf(profile?.announcementsEnabled ?: true) }
  var achievements by remember(profile) { mutableStateOf(profile?.achievementsEnabled ?: true) }

  val careerOptions = listOf(
    "AI Specialist", "Prompt Engineer", "Entrepreneur", "Freelancer", "Automation Builder", "App Builder", "Productivity Leader"
  )
  val learningOptions = listOf(
    "Artificial Intelligence", "Prompt Engineering", "AI Productivity", "AI for Business", "Building Apps with AI", "Automation"
  )
  val selectedCareers = remember(profile) {
    mutableStateListOf<String>().apply { addAll(profile?.careerInterests.orEmpty()) }
  }
  val selectedTopics = remember(profile) {
    mutableStateListOf<String>().apply { addAll(profile?.learningInterests.orEmpty()) }
  }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      viewModel.sendLocalStudyReminder()
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("profile_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = NavyDeep)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(62.dp)
              .clip(CircleShape)
              .background(ElectricBlue),
            contentAlignment = Alignment.Center
          ) {
            val photoUrl = profile?.photoUrl
            if (!photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = photoUrl,
                contentDescription = "Profile avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Text(
                text = (profile?.fullName?.firstOrNull() ?: 'D').uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(profile?.fullName ?: "Datalabs Learner", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(profile?.email ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
            Text(
              text = "${student.enrollments.size} Enrolled • ${student.enrollments.count { it.status == "COMPLETED" }} Completed • ${student.certificates.size} Certificates • ${student.savedCourses.size} Saved",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD)
            )
          }
        }
      }
    }

    // Admin Console Button (strictly role-gated!)
    if (isAdmin) {
      item {
        Card(
          border = BorderStroke(1.5.dp, ElectricBlue),
          colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.08f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Administrator Console", style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
              Text("Manage AI Academy courses, lessons, students, and analytics.", style = MaterialTheme.typography.bodySmall)
            }
            Button(
              onClick = onOpenAdmin,
              modifier = Modifier.testTag("open_admin_console_button")
            ) {
              Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Manage")
            }
          }
        }
      }
    }

    // Subscription Status Card
    item {
      val sub = student.subscription
      val isPro = sub?.activeEntitlement == true
      Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = if (isPro) SuccessEmerald else ElectricBlue
              )
              Text(
                text = if (isPro) "Datalabs Pro Active" else "Free Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = sub?.let { "${it.planName} (${it.status})" } ?: "Upgrade for full access to all AI Academy courses & certificates.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          OutlinedButton(
            onClick = onOpenSubscription,
            modifier = Modifier.testTag("open_subscription_button")
          ) {
            Text(if (isPro) "Manage" else "Upgrade")
          }
        }
      }
    }

    // Editable Profile & Interests
    item {
      Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("Personal & Learning Goals", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = learningGoal,
            onValueChange = { learningGoal = it },
            label = { Text("Learning Goal") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = weeklyHours.toString(),
            onValueChange = { txt -> txt.toLongOrNull()?.let { weeklyHours = it.coerceIn(1L, 80L) } },
            label = { Text("Weekly Study Target (Hours)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Text("Career Interests", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            careerOptions.forEach { option ->
              val picked = selectedCareers.contains(option)
              FilterChip(
                selected = picked,
                onClick = { if (picked) selectedCareers.remove(option) else selectedCareers.add(option) },
                label = { Text(option) }
              )
            }
          }

          Text("Learning Interests", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            learningOptions.forEach { topic ->
              val picked = selectedTopics.contains(topic)
              FilterChip(
                selected = picked,
                onClick = { if (picked) selectedTopics.remove(topic) else selectedTopics.add(topic) },
                label = { Text(topic) }
              )
            }
          }
        }
      }
    }

    // Notification Preferences
    item {
      Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("Notification Settings", style = MaterialTheme.typography.titleMedium)
          PrefSwitchRow("Study reminders", reminders) { reminders = it }
          PrefSwitchRow("Course updates", courseUpdates) { courseUpdates = it }
          PrefSwitchRow("New academy announcements", announcements) { announcements = it }
          PrefSwitchRow("Certificates & achievements", achievements) { achievements = it }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  viewModel.sendLocalStudyReminder()
                }
              }
            ) {
              Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Test Reminder")
            }
            OutlinedButton(onClick = onOpenNotifications) {
              Text("View Inbox")
            }
          }
        }
      }
    }

    item {
      Button(
        onClick = {
          viewModel.updateProfile(
            fullName = fullName.ifBlank { profile?.fullName ?: "Datalabs Learner" },
            learningGoal = learningGoal,
            weeklyTargetHours = weeklyHours,
            careerInterests = selectedCareers.toList(),
            learningInterests = selectedTopics.toList(),
            remindersEnabled = reminders,
            courseUpdatesEnabled = courseUpdates,
            announcementsEnabled = announcements,
            achievementsEnabled = achievements
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("save_profile_button")
      ) {
        Text("Save Profile & Preferences")
      }
    }

    item {
      Card {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("Session & Sign-In", style = MaterialTheme.typography.titleMedium)
          PrefSwitchRow(
            label = "Remember me on this device",
            checked = authPrefs.rememberMe,
            onCheckedChange = { enabled ->
              authPreferences.setRememberMeAsync(
                enabled = enabled,
                markPromptHandled = true,
                email = profile?.email,
                displayName = profile?.fullName
              )
            }
          )
          Text(
            text = if (authPrefs.rememberMe) {
              "You will stay signed in automatically between app launches."
            } else {
              "You will be asked to sign in again the next time you reopen the app."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    item {
      OutlinedButton(
        onClick = {
          authPreferences.markExplicitSignOut()
          scope.launch {
            val cm = credentialManager ?: withContext(Dispatchers.IO) {
              runCatching { CredentialManager.create(context) }.getOrNull()
            }?.also { credentialManager = it }
            AuthManager.signOut(cm, scope)
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("sign_out_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out")
      }
    }
  }
}

@Composable
private fun PrefSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
