package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Academy
import com.example.data.model.Course
import com.example.data.model.CourseRatingHelper
import com.example.data.model.CourseRatingSummary
import com.example.data.model.LearningPath
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.theme.parseAccentColor

fun academyIconFor(iconKey: String): ImageVector = when (iconKey.lowercase()) {
  "ai" -> Icons.Filled.AutoAwesome
  "analytics" -> Icons.Filled.Analytics
  "business" -> Icons.Filled.BusinessCenter
  "career" -> Icons.Filled.WorkOutline
  "freelancing" -> Icons.Filled.Public
  "tech" -> Icons.Filled.Devices
  "marketing" -> Icons.Filled.Campaign
  "life" -> Icons.Filled.Psychology
  "academic" -> Icons.AutoMirrored.Filled.MenuBook
  "industry" -> Icons.Filled.Engineering
  "finance" -> Icons.Filled.AccountBalance
  "investing" -> Icons.AutoMirrored.Filled.TrendingUp
  "ecommerce" -> Icons.Filled.ShoppingBag
  else -> Icons.Filled.School
}

fun formatDuration(minutes: Long): String {
  val hrs = minutes / 60
  val mins = minutes % 60
  return when {
    hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
    hrs > 0 -> "${hrs}h"
    else -> "${mins}m"
  }
}

@Composable
fun TierBadge(
  isPremium: Boolean,
  hasSubscription: Boolean = false
) {
  val bg = when {
    !isPremium -> SuccessEmerald.copy(alpha = 0.12f)
    hasSubscription -> ElectricBlue.copy(alpha = 0.12f)
    else -> WarningGold.copy(alpha = 0.14f)
  }
  val fg = when {
    !isPremium -> SuccessEmerald
    hasSubscription -> ElectricBlue
    else -> WarningGold
  }
  val label = when {
    !isPremium -> "FREE"
    hasSubscription -> "PRO UNLOCKED"
    else -> "PREMIUM"
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(6.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (isPremium && !hasSubscription) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(12.dp)
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun AcademyCard(
  academy: Academy,
  actualCourseCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val accent = parseAccentColor(academy.accentHex, ElectricBlue)
  Card(
    modifier = modifier
      .testTag("academy_card_${academy.id}")
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .background(accent)
      )
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = academyIconFor(academy.iconKey),
              contentDescription = academy.name,
              tint = accent,
              modifier = Modifier.size(22.dp)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
          ) {
            Text(
              text = "${if (actualCourseCount > 0) actualCourseCount else academy.courseCount} courses",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
        Text(
          text = academy.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = academy.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun CourseCard(
  course: Course,
  accentColor: Color = ElectricBlue,
  isSaved: Boolean = false,
  isCompared: Boolean = false,
  hasSubscription: Boolean = false,
  progressPercent: Long? = null,
  ratingSummary: CourseRatingSummary? = null,
  onClick: () -> Unit,
  onToggleSave: (() -> Unit)? = null,
  onToggleCompare: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("course_card_${course.id}")
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Text(
            text = course.academyName,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        TierBadge(isPremium = course.premiumOnly, hasSubscription = hasSubscription)
      }

      Text(
        text = course.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = course.subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      val activeSummary = ratingSummary?.takeIf { it.reviewCount > 0 }
      Row(
        modifier = Modifier.testTag("course_card_rating_${course.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = if (activeSummary != null) Icons.Filled.Star else Icons.Filled.StarBorder,
          contentDescription = if (activeSummary != null) {
            "Average rating ${CourseRatingHelper.formatAverage(activeSummary)} out of 5"
          } else {
            stringResource(R.string.course_rating_no_reviews)
          },
          tint = if (activeSummary != null) WarningGold else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
        if (activeSummary != null) {
          val reviewWord = if (activeSummary.reviewCount == 1) "review" else "reviews"
          Text(
            text = CourseRatingHelper.formatAverage(activeSummary),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "(${activeSummary.reviewCount} $reviewWord)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          Text(
            text = stringResource(R.string.course_rating_no_reviews),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${course.level.lowercase().replaceFirstChar { it.uppercase() }} • ${formatDuration(course.durationMinutes)} • ${course.instructorName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onToggleCompare != null) {
            IconButton(
              onClick = onToggleCompare,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("compare_course_${course.id}")
            ) {
              Icon(
                imageVector = Icons.Filled.CompareArrows,
                contentDescription = if (isCompared) "Remove from comparison" else "Compare course",
                tint = if (isCompared) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          if (onToggleSave != null) {
            IconButton(
              onClick = onToggleSave,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("save_course_${course.id}")
            ) {
              Icon(
                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isSaved) "Remove saved course" else "Save course",
                tint = if (isSaved) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      if (progressPercent != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          LinearProgressIndicator(
            progress = { (progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (progressPercent >= 100L) SuccessEmerald else ElectricBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = if (progressPercent >= 100L) "Completed" else "$progressPercent% complete",
              style = MaterialTheme.typography.labelSmall,
              color = if (progressPercent >= 100L) SuccessEmerald else ElectricBlue
            )
            if (progressPercent >= 100L) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Course completed",
                tint = SuccessEmerald,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LearningPathCard(
  path: LearningPath,
  progressPercent: Long,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .testTag("learning_path_${path.id}")
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = NavyDeep),
    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = ElectricBlue.copy(alpha = 0.2f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${path.courseIds.size} COURSES • ${path.estimatedHours}H",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF93C5FD),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        if (path.premiumOnly) {
          Surface(
            color = WarningGold.copy(alpha = 0.22f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "PRO PATH",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFFCD34D),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Text(
        text = path.title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = path.description,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFCBD5E1),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      LinearProgressIndicator(
        progress = { (progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = Color(0xFF60A5FA),
        trackColor = Color.White.copy(alpha = 0.15f)
      )
      Text(
        text = "$progressPercent% pathway progress",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF93C5FD)
      )
    }
  }
}

@Composable
fun LoadingStateView(message: String = "Loading Datalabs Learning Hub...") {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CircularProgressIndicator(color = ElectricBlue)
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = ElectricBlue,
        modifier = Modifier.size(36.dp)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onAction, modifier = Modifier.minimumInteractiveComponentSize()) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun ErrorStateCard(
  message: String,
  onRetry: (() -> Unit)? = null
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "Something went wrong",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onErrorContainer
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer
      )
      if (onRetry != null) {
        Button(onClick = onRetry) {
          Text("Retry")
        }
      }
    }
  }
}
