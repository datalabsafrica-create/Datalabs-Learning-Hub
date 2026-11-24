package com.example.data.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class LearningNotificationHelper(private val context: Context) {

  companion object {
    const val CHANNEL_REMINDERS = "datalabs_learning_reminders"
    const val CHANNEL_UPDATES = "datalabs_course_updates"
  }

  fun ensureChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val remindersChannel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Learning Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Daily study reminders and streak milestones"
      }
      val updatesChannel = NotificationChannel(
        CHANNEL_UPDATES,
        "Course Updates & Certificates",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "New academies, course releases, and certificate awards"
      }
      manager.createNotificationChannel(remindersChannel)
      manager.createNotificationChannel(updatesChannel)
    }
  }

  fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showLearningNotification(
    title: String,
    body: String,
    isCourseUpdate: Boolean = false
  ) {
    if (!hasNotificationPermission()) return
    ensureChannels()
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val channelId = if (isCourseUpdate) CHANNEL_UPDATES else CHANNEL_REMINDERS
    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_stat_learning)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()
    try {
      NotificationManagerCompat.from(context).notify(
        (System.currentTimeMillis() and 0xFFFFFF).toInt(),
        notification
      )
    } catch (_: SecurityException) {
      // User revoked POST_NOTIFICATIONS at runtime
    }
  }
}
