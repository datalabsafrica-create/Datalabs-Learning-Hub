package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiHelpMode
import com.example.data.model.AiAssistantUiState
import com.example.data.model.Course
import com.example.data.model.Lesson
import com.example.data.model.LessonContent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.NavyDeep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
  course: Course,
  lesson: Lesson,
  lessonContent: LessonContent?,
  aiState: AiAssistantUiState,
  onAskAi: (AiHelpMode, String) -> Unit,
  onClearAi: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedMode by remember { mutableStateOf(AiHelpMode.ASK_QUESTION) }
  var questionInput by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .testTag("ai_assistant_sheet"),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "AI Study Tutor",
            tint = ElectricBlue
          )
          Column {
            Text("Datalabs AI Study Tutor", style = MaterialTheme.typography.titleLarge)
            Text(
              text = lesson.title,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("ai_assistant_close_button")
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Close AI Tutor")
        }
      }

      // Transparency badge distinguishing official course notes vs AI guidance
      Surface(
        color = NavyDeep.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "AI Study Explanation — Assists your understanding alongside Verified Datalabs Course Material. Coaches you on exercises without giving away graded answers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text("Quick Study Modes", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          AiHelpMode.SIMPLIFY,
          AiHelpMode.EXTRA_EXAMPLE,
          AiHelpMode.SUMMARIZE,
          AiHelpMode.EXERCISE_HINT,
          AiHelpMode.PRACTICE_QUESTIONS
        ).forEach { mode ->
          FilterChip(
            selected = selectedMode == mode,
            onClick = {
              selectedMode = mode
              onAskAi(mode, questionInput)
            },
            label = { Text(mode.label) },
            modifier = Modifier.testTag("ai_mode_${mode.name.lowercase()}")
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = questionInput,
          onValueChange = { questionInput = it },
          label = { Text("Ask a question about this lesson...") },
          placeholder = { Text("e.g. How do I apply this in Excel or business?") },
          minLines = 1,
          maxLines = 3,
          modifier = Modifier
            .weight(1f)
            .testTag("ai_question_input")
        )
        Button(
          onClick = {
            if (questionInput.isNotBlank()) {
              selectedMode = AiHelpMode.ASK_QUESTION
              onAskAi(AiHelpMode.ASK_QUESTION, questionInput)
            }
          },
          enabled = questionInput.isNotBlank() && aiState !is AiAssistantUiState.Loading,
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
          modifier = Modifier.testTag("ai_send_question_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask AI Tutor", modifier = Modifier.size(18.dp))
        }
      }

      when (aiState) {
        is AiAssistantUiState.Idle -> {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text("How I can help with ${course.title}:", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "• Tap a study mode above for simpler explanations, worked examples, key summaries, or practice questions.\n• Or type any specific question about '${lesson.title}' above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        is AiAssistantUiState.Loading -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ElectricBlue)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Preparing study explanation for \"${aiState.promptLabel}\"...",
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        is AiAssistantUiState.Streaming -> {
          AiResponseCard(
            heading = "${aiState.promptLabel} (Generating...)",
            body = aiState.text,
            isStreaming = true,
            onClear = null
          )
        }

        is AiAssistantUiState.Success -> {
          AiResponseCard(
            heading = aiState.promptLabel,
            body = aiState.text,
            isStreaming = false,
            onClear = onClearAi
          )
        }

        is AiAssistantUiState.Error -> {
          Card(
            border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.08f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "AI Study Tutor Unavailable",
                style = MaterialTheme.typography.titleSmall,
                color = ErrorCrimson,
                fontWeight = FontWeight.Bold
              )
              Text(aiState.message, style = MaterialTheme.typography.bodySmall)
              OutlinedButton(
                onClick = { onAskAi(selectedMode, questionInput) },
                modifier = Modifier.testTag("ai_retry_button")
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry")
              }
            }
          }
        }
      }

      if (lessonContent == null) {
        Text(
          text = "Tip: Wait for lesson notes to load for deeper contextual answers.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
    }
  }
}

@Composable
private fun AiResponseCard(
  heading: String,
  body: String,
  isStreaming: Boolean,
  onClear: (() -> Unit)?
) {
  Card(
    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = ElectricBlue.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = if (isStreaming) "AI Tutor • Streaming" else "AI Study Explanation",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        if (onClear != null) {
          OutlinedButton(onClick = onClear) {
            Text("Reset")
          }
        }
      }
      Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Text(body, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
