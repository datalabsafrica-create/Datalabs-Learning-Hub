package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.delay

/**
 * Pure helper managing tagged Practice Lab entries inside the learner's persisted
 * `lessonProgress.studentNotes` string so responses survive app restarts and stay
 * compatible with Firestore security rules.
 */
object LessonNotesHelper {
  const val LAB_SECTION_HEADER = "[PRACTICE LAB RESPONSE]"
  private const val MAX_NOTES_CHARS = 10000

  fun hasSavedLabResponse(studentNotes: String): Boolean {
    return extractLabDraft(studentNotes).isNotBlank()
  }

  fun extractLabDraft(studentNotes: String): String {
    val idx = studentNotes.indexOf(LAB_SECTION_HEADER)
    if (idx == -1) return ""
    return studentNotes.substring(idx + LAB_SECTION_HEADER.length).trim()
  }

  fun upsertLabResponse(existingNotes: String, labResponse: String): String {
    val cleanLab = labResponse.trim()
    val idx = existingNotes.indexOf(LAB_SECTION_HEADER)
    val baseNotes = if (idx == -1) {
      existingNotes.trimEnd()
    } else {
      existingNotes.substring(0, idx).trimEnd()
    }
    if (cleanLab.isBlank()) {
      return baseNotes.take(MAX_NOTES_CHARS)
    }
    val combined = buildString {
      if (baseNotes.isNotBlank()) {
        append(baseNotes)
        append("\n\n")
      }
      append(LAB_SECTION_HEADER)
      append('\n')
      append(cleanLab)
    }
    return combined.take(MAX_NOTES_CHARS)
  }
}

@Composable
fun CopyableCodeBlockCard(
  languageTag: String,
  code: String,
  modifier: Modifier = Modifier
) {
  val clipboard = LocalClipboardManager.current
  var copied by remember(code) { mutableStateOf(false) }

  LaunchedEffect(copied) {
    if (copied) {
      delay(1800L)
      copied = false
    }
  }

  Surface(
    color = NavyDeep,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, NavyElevated),
    modifier = modifier
      .fillMaxWidth()
      .testTag("copyable_code_block")
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Code,
            contentDescription = null,
            tint = ElectricBlueSoft,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = languageTag.ifBlank { "TEMPLATE / CODE" }.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlueSoft,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(
          onClick = {
            clipboard.setText(AnnotatedString(code))
            copied = true
          },
          modifier = Modifier.testTag("copy_code_button")
        ) {
          Icon(
            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = if (copied) "Copied snippet" else "Copy snippet",
            tint = if (copied) SuccessEmerald else ElectricBlueSoft,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      HorizontalDivider(color = NavyElevated)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(12.dp)
      ) {
        Text(
          text = code,
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          color = Color(0xFFE2E8F0)
        )
      }
    }
  }
}

@Composable
fun InteractiveKeyTakeawaysCard(
  takeaways: List<String>,
  lessonId: String,
  modifier: Modifier = Modifier
) {
  if (takeaways.isEmpty()) return
  val checkedState = remember(lessonId) { mutableStateMapOf<Int, Boolean>() }
  val checkedCount = takeaways.indices.count { checkedState[it] == true }
  val allChecked = checkedCount == takeaways.size

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("interactive_takeaways_card"),
    border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = if (allChecked) 0.75f else 0.4f)),
    colors = CardDefaults.cardColors(containerColor = SuccessEmerald.copy(alpha = 0.06f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
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
            imageVector = Icons.Filled.Lightbulb,
            contentDescription = null,
            tint = SuccessEmerald,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Key Takeaways Checklist",
            style = MaterialTheme.typography.titleSmall,
            color = SuccessEmerald,
            fontWeight = FontWeight.Bold
          )
        }
        Surface(
          color = SuccessEmerald.copy(alpha = if (allChecked) 0.22f else 0.12f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = if (allChecked) "All Verified" else "$checkedCount/${takeaways.size} Verified",
            style = MaterialTheme.typography.labelSmall,
            color = SuccessEmerald,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      takeaways.forEachIndexed { idx, takeaway ->
        val isChecked = checkedState[idx] == true
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { checkedState[idx] = !isChecked }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("takeaway_check_$idx"),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = if (isChecked) "Verified takeaway" else "Verify takeaway",
            tint = if (isChecked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = takeaway,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun LessonExerciseLabCard(
  lessonId: String,
  exercisePrompt: String,
  savedNotes: String,
  onSaveLabResponse: (String) -> Unit,
  onAskTutorForFeedback: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val hasLogged = remember(savedNotes) { LessonNotesHelper.hasSavedLabResponse(savedNotes) }
  var labDraft by remember(lessonId) {
    mutableStateOf(LessonNotesHelper.extractLabDraft(savedNotes))
  }
  // Keep draft synchronized on first load from Firestore if still empty locally
  LaunchedEffect(lessonId, savedNotes) {
    val persisted = LessonNotesHelper.extractLabDraft(savedNotes)
    if (labDraft.isBlank() && persisted.isNotBlank()) {
      labDraft = persisted
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("lesson_exercise_lab_card"),
    border = BorderStroke(
      1.dp,
      if (hasLogged) SuccessEmerald.copy(alpha = 0.55f) else ElectricBlue.copy(alpha = 0.4f)
    ),
    colors = CardDefaults.cardColors(
      containerColor = if (hasLogged) SuccessEmerald.copy(alpha = 0.05f) else ElectricBlue.copy(alpha = 0.05f)
    )
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
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Science,
            contentDescription = null,
            tint = if (hasLogged) SuccessEmerald else ElectricBlue,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "Hands-On Practice Lab",
            style = MaterialTheme.typography.titleSmall,
            color = if (hasLogged) SuccessEmerald else ElectricBlue,
            fontWeight = FontWeight.Bold
          )
        }
        Surface(
          color = (if (hasLogged) SuccessEmerald else ElectricBlue).copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = if (hasLogged) "Lab Logged" else "Action Lab",
            style = MaterialTheme.typography.labelSmall,
            color = if (hasLogged) SuccessEmerald else ElectricBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Text(
        text = exercisePrompt,
        style = MaterialTheme.typography.bodyMedium
      )

      OutlinedTextField(
        value = labDraft,
        onValueChange = { labDraft = it },
        label = { Text("Draft your lab response here") },
        placeholder = { Text("Write your prompt, calculation, audit log, or JSON schema...") },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("lesson_lab_input")
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onSaveLabResponse(labDraft) },
          enabled = labDraft.isNotBlank(),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (hasLogged) SuccessEmerald else ElectricBlue,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("save_lab_response_button")
        ) {
          Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (hasLogged) "Update Lab" else "Save Lab")
        }
        OutlinedButton(
          onClick = { onAskTutorForFeedback(labDraft.ifBlank { exercisePrompt }) },
          modifier = Modifier
            .weight(1f)
            .testTag("review_lab_ai_button")
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("AI Coach Hint")
        }
      }
    }
  }
}

@Composable
fun PracticeKitPreviewCard(
  title: String,
  content: String,
  onDownloadShare: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember(title) { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current
  var copied by remember(title) { mutableStateOf(false) }

  LaunchedEffect(copied) {
    if (copied) {
      delay(1800L)
      copied = false
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("practice_kit_preview_card"),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { expanded = !expanded }
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PRACTICE KIT TEMPLATE",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "Collapse kit preview" else "Expand kit preview"
        )
      }

      if (expanded) {
        Surface(
          color = NavySurface,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(12.dp)
          ) {
            Text(
              text = content,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = Color(0xFFE2E8F0)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            clipboard.setText(AnnotatedString(content))
            copied = true
          },
          modifier = Modifier
            .weight(1f)
            .testTag("copy_practice_kit_button")
        ) {
          Icon(
            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (copied) "Copied Kit" else "Copy Template")
        }
        Button(
          onClick = onDownloadShare,
          modifier = Modifier
            .weight(1f)
            .testTag("download_resource_button")
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export File")
        }
      }
    }
  }
}
