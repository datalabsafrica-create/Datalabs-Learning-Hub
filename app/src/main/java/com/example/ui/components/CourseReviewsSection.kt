package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Course
import com.example.data.model.CourseRatingHelper
import com.example.data.model.CourseRatingSummary
import com.example.data.model.CourseReview
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CourseReviewsSection(
  course: Course,
  reviews: List<CourseReview>,
  ratingSummary: CourseRatingSummary,
  currentUserId: String,
  onSubmitReview: (rating: Int, reviewText: String, onDone: (Boolean) -> Unit) -> Unit,
  onDeleteReview: (onDone: (Boolean) -> Unit) -> Unit,
  modifier: Modifier = Modifier
) {
  val existingReview = remember(reviews, currentUserId) {
    reviews.firstOrNull { it.userId == currentUserId }
  }
  var selectedRating by remember(existingReview?.id, existingReview?.rating) {
    mutableIntStateOf(existingReview?.rating?.toInt()?.coerceIn(1, 5) ?: 0)
  }
  var reviewText by remember(existingReview?.id, existingReview?.reviewText) {
    mutableStateOf(existingReview?.reviewText ?: "")
  }
  var isSubmitting by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .testTag("course_reviews_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.RateReview,
        contentDescription = null,
        tint = ElectricBlue,
        modifier = Modifier.size(22.dp)
      )
      Text(
        text = stringResource(R.string.course_reviews_heading),
        style = MaterialTheme.typography.titleLarge
      )
    }

    // Aggregate Rating Summary Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("course_rating_summary_card"),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Surface(
          color = WarningGold.copy(alpha = 0.14f),
          shape = RoundedCornerShape(14.dp)
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (ratingSummary.reviewCount > 0) {
                CourseRatingHelper.formatAverage(ratingSummary)
              } else {
                "--"
              },
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "out of 5",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          StarRatingDisplayRow(
            rating = kotlin.math.round(ratingSummary.averageRating).toInt(),
            starSize = 18.dp
          )
          Text(
            text = if (ratingSummary.reviewCount > 0) {
              val word = if (ratingSummary.reviewCount == 1) "student review" else "student reviews"
              "Based on ${ratingSummary.reviewCount} $word"
            } else {
              stringResource(R.string.course_rating_no_reviews)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
          if (existingReview != null) {
            Text(
              text = "You rated this ${existingReview.rating}/5",
              style = MaterialTheme.typography.labelMedium,
              color = SuccessEmerald,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // Submit / Edit Review Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("course_review_editor_card"),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = if (existingReview != null) {
            stringResource(R.string.course_reviews_update_title)
          } else {
            stringResource(R.string.course_reviews_rate_title)
          },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          (1..5).forEach { star ->
            val selected = star <= selectedRating
            IconButton(
              onClick = { if (!isSubmitting) selectedRating = star },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("review_star_$star")
            ) {
              Icon(
                imageVector = if (selected) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Rate $star out of 5 stars",
                tint = if (selected) WarningGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
              )
            }
          }
        }

        Text(
          text = when (selectedRating) {
            1 -> "1 / 5 • Needs major improvement"
            2 -> "2 / 5 • Fair, missing key depth"
            3 -> "3 / 5 • Good, helpful fundamentals"
            4 -> "4 / 5 • Very good, practical content"
            5 -> "5 / 5 • Excellent learning experience"
            else -> "Tap a star above to choose your rating"
          },
          style = MaterialTheme.typography.labelMedium,
          color = if (selectedRating > 0) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = reviewText,
          onValueChange = { updated ->
            if (updated.length <= CourseRatingHelper.MAX_REVIEW_LENGTH) {
              reviewText = updated
            }
          },
          label = { Text(stringResource(R.string.course_reviews_input_label)) },
          minLines = 3,
          maxLines = 6,
          supportingText = {
            Text("${reviewText.length}/${CourseRatingHelper.MAX_REVIEW_LENGTH}")
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("review_text_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (selectedRating in 1..5 && !isSubmitting) {
                isSubmitting = true
                onSubmitReview(selectedRating, reviewText) {
                  isSubmitting = false
                }
              }
            },
            enabled = selectedRating in 1..5 && !isSubmitting,
            colors = ButtonDefaults.buttonColors(
              containerColor = ElectricBlue,
              contentColor = Color.White
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("submit_review_button")
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = if (existingReview != null) {
                stringResource(R.string.course_reviews_update)
              } else {
                stringResource(R.string.course_reviews_submit)
              }
            )
          }

          if (existingReview != null) {
            TextButton(
              onClick = {
                if (!isSubmitting) {
                  isSubmitting = true
                  onDeleteReview { success ->
                    isSubmitting = false
                    if (success) {
                      selectedRating = 0
                      reviewText = ""
                    }
                  }
                }
              },
              enabled = !isSubmitting,
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("delete_review_button")
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.course_reviews_delete),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(stringResource(R.string.course_reviews_delete))
            }
          }
        }
      }
    }

    // Student Reviews List
    if (reviews.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Text(
          text = stringResource(R.string.course_reviews_empty_list),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      reviews.forEach { review ->
        CourseReviewItemCard(
          review = review,
          isCurrentUser = review.userId == currentUserId
        )
      }
    }
  }
}

@Composable
private fun CourseReviewItemCard(
  review: CourseReview,
  isCurrentUser: Boolean
) {
  val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
  val dateLabel = remember(review.updatedAt, review.createdAt) {
    (review.updatedAt ?: review.createdAt)?.toDate()?.let { dateFormatter.format(it) } ?: "Just now"
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("course_review_item_${review.id}"),
    border = BorderStroke(
      1.dp,
      if (isCurrentUser) ElectricBlue.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
    ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isCurrentUser) ElectricBlue else NavyDeep),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = review.userName.trim().take(1).ifEmpty { "L" }.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = review.userName.ifBlank { "Datalabs Learner" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            if (isCurrentUser) {
              Surface(
                color = ElectricBlue.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "You",
                  style = MaterialTheme.typography.labelSmall,
                  color = ElectricBlue,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        StarRatingDisplayRow(
          rating = review.rating.toInt().coerceIn(0, 5),
          starSize = 15.dp
        )
      }

      if (review.reviewText.isNotBlank()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Text(
          text = review.reviewText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun StarRatingDisplayRow(
  rating: Int,
  starSize: androidx.compose.ui.unit.Dp
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    (1..5).forEach { index ->
      Icon(
        imageVector = if (index <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
        contentDescription = null,
        tint = if (index <= rating) WarningGold else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(starSize)
      )
    }
  }
}
