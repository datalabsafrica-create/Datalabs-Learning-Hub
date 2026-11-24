package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: MainViewModel,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit
) {
  val catalog = (catalogState as? UiState.Success)?.data ?: HubCatalogSnapshot()
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) viewModel.sendLocalStudyReminder()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notifications & Updates") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Button(
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              viewModel.sendLocalStudyReminder()
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.NotificationsActive, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Study Reminder Notification")
        }
      }

      if (catalog.announcements.isNotEmpty()) {
        item {
          Text("Academy & Platform Announcements", style = MaterialTheme.typography.titleMedium)
        }
        items(catalog.announcements, key = { it.id }) { ann ->
          Card(
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
            colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.06f))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Filled.Campaign, contentDescription = null, tint = ElectricBlue)
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(ann.message, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      item {
        Text("Personal Notifications (${student.notifications.size})", style = MaterialTheme.typography.titleMedium)
      }

      if (student.notifications.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Filled.Notifications,
            title = "All caught up",
            description = "Course updates, certificates, and study reminders will appear here."
          )
        }
      } else {
        items(student.notifications, key = { it.id }) { item ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { if (!item.read) viewModel.markNotificationRead(item.id) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
              containerColor = if (item.read) MaterialTheme.colorScheme.surface else ElectricBlue.copy(alpha = 0.05f)
            )
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!item.read) {
                  Text("NEW", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                }
              }
              Text(item.body, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}
