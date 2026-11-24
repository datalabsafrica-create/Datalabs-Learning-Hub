package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuizQuestion
import com.example.data.model.UiState
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
  quizId: String,
  courseId: String,
  viewModel: MainViewModel,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit
) {
  val quizzes by viewModel.observeQuizzes(courseId).collectAsStateWithLifecycle(initialValue = emptyList())
  val quiz = quizzes.firstOrNull { it.id == quizId }
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val attempts = student.quizAttempts.filter { it.quizId == quizId }

  if (quiz == null) {
    LoadingStateView("Loading assessment...")
    return
  }

  val questions = remember(quiz.questionsJson) {
    try {
      Json { ignoreUnknownKeys = true }.decodeFromString<List<QuizQuestion>>(quiz.questionsJson)
    } catch (_: Exception) {
      emptyList()
    }
  }

  val selectedAnswers = remember(quizId) { mutableStateMapOf<String, String>() }
  var submittedNow by remember(quizId) { mutableStateOf(false) }
  var lastScorePercent by remember(quizId) { mutableStateOf(0L) }
  var lastPassed by remember(quizId) { mutableStateOf(false) }

  val attemptsRemaining = (quiz.maxAttempts - attempts.size).coerceAtLeast(0)
  val canSubmit = (attemptsRemaining > 0) && (quiz.allowRetakes || attempts.isEmpty())

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Assessment") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
        .testTag("quiz_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Surface(
        color = ElectricBlue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          text = if (quiz.assessmentType == "MODULE_QUIZ") "MODULE CHECKPOINT QUIZ" else "FINAL COURSE EXAMINATION",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricBlue,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
      Text(quiz.title, style = MaterialTheme.typography.headlineMedium)
      Text(quiz.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = "Passing score: ${quiz.passingScorePercent}% • Attempts used: ${attempts.size}/${quiz.maxAttempts} • Retakes: ${if (quiz.allowRetakes) "Allowed" else "Single attempt"}",
        style = MaterialTheme.typography.labelMedium,
        color = ElectricBlue
      )

      val bestPassedScore = attempts.filter { it.passed }.maxOfOrNull { it.scorePercent }
      if (!submittedNow && bestPassedScore != null) {
        Surface(
          color = SuccessEmerald.copy(alpha = 0.1f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessEmerald)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Already Passed • Best Score: $bestPassedScore%",
                style = MaterialTheme.typography.titleSmall,
                color = SuccessEmerald,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "You have cleared this requirement. You may retake below to sharpen your mastery.",
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }

      if (submittedNow) {
        Surface(
          color = if (lastPassed) SuccessEmerald.copy(alpha = 0.12f) else ErrorCrimson.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = if (lastPassed) "Passed! Score: $lastScorePercent%" else "Score: $lastScorePercent% (Required: ${quiz.passingScorePercent}%)",
              style = MaterialTheme.typography.titleMedium,
              color = if (lastPassed) SuccessEmerald else ErrorCrimson,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Review why each answer is right or wrong below before continuing.",
              style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (lastPassed) {
                Button(
                  onClick = onBack,
                  modifier = Modifier.testTag("quiz_back_to_course_button")
                ) {
                  Text("Back to Course")
                }
              }
              if (canSubmit && !lastPassed) {
                OutlinedButton(
                  onClick = {
                    selectedAnswers.clear()
                    submittedNow = false
                  },
                  modifier = Modifier.testTag("retake_quiz_button")
                ) {
                  Icon(Icons.Filled.Refresh, contentDescription = null)
                  Text("Retake Assessment")
                }
              }
            }
          }
        }
      }

      questions.forEachIndexed { idx, q ->
        val chosen = selectedAnswers[q.id] ?: ""
        val isCorrect = chosen.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
        val typeLabel = when (q.type.uppercase()) {
          "TRUE_FALSE" -> "True / False"
          "SCENARIO" -> "Scenario"
          "SHORT_ANSWER" -> "Short Answer"
          else -> "Multiple Choice"
        }

        Card(
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
              Text("Question ${idx + 1} (${q.points} pts)", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = typeLabel,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
            Text(q.prompt, style = MaterialTheme.typography.titleSmall)

            if (q.type == "SHORT_ANSWER" || q.options.isEmpty()) {
              OutlinedTextField(
                value = chosen,
                onValueChange = { selectedAnswers[q.id] = it },
                label = { Text("Your answer") },
                singleLine = true,
                enabled = !submittedNow,
                modifier = Modifier.fillMaxWidth()
              )
            } else {
              q.options.forEach { option ->
                val picked = chosen == option
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !submittedNow) { selectedAnswers[q.id] = option },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, if (picked) ElectricBlue else MaterialTheme.colorScheme.outline),
                  color = if (picked) ElectricBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = if (picked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                      contentDescription = null,
                      tint = if (picked) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }

            if (submittedNow) {
              Surface(
                color = if (isCorrect) SuccessEmerald.copy(alpha = 0.08f) else ErrorCrimson.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = if (isCorrect) "Correct! You selected: $chosen" else "Your answer: ${chosen.ifBlank { "No answer" }} • Correct answer: ${q.correctAnswer}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCorrect) SuccessEmerald else ErrorCrimson,
                    fontWeight = FontWeight.SemiBold
                  )
                  if (q.explanation.isNotBlank()) {
                    Text(
                      text = "Why: ${q.explanation}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }
      }

      val answeredCount = questions.count { !selectedAnswers[it.id].isNullOrBlank() }
      val allAnswered = questions.isNotEmpty() && answeredCount == questions.size

      if (questions.isNotEmpty() && !submittedNow) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          LinearProgressIndicator(
            progress = { answeredCount.toFloat() / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = ElectricBlue
          )
          Text(
            text = "Answered $answeredCount of ${questions.size} questions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Button(
        onClick = {
          val totalPoints = questions.sumOf { it.points }.coerceAtLeast(1)
          var earned = 0
          questions.forEach { q ->
            val ans = selectedAnswers[q.id]?.trim() ?: ""
            if (ans.equals(q.correctAnswer.trim(), ignoreCase = true)) {
              earned += q.points
            }
          }
          val scorePct = ((earned * 100.0) / totalPoints).toLong().coerceIn(0L, 100L)
          val passed = scorePct >= quiz.passingScorePercent
          lastScorePercent = scorePct
          lastPassed = passed
          submittedNow = true
          viewModel.submitQuiz(
            quiz = quiz,
            scorePercent = scorePct,
            earnedPoints = earned.toLong(),
            totalPoints = totalPoints.toLong(),
            passed = passed,
            attemptNumber = (attempts.size + 1).toLong(),
            answersSummary = selectedAnswers.entries.joinToString("; ") { "${it.key}=${it.value}" }
          )
        },
        enabled = canSubmit && !submittedNow && allAnswered,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("submit_quiz_button")
      ) {
        Text(
          when {
            !canSubmit -> "Maximum Attempts Reached"
            !allAnswered && !submittedNow -> "Answer All Questions ($answeredCount/${questions.size})"
            else -> "Submit Assessment"
          }
        )
      }

      if (attempts.isNotEmpty()) {
        Text("Assessment History", style = MaterialTheme.typography.titleMedium)
        attempts.forEach { att ->
          Card(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Attempt #${att.attemptNumber}", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "${att.scorePercent}% • ${if (att.passed) "PASSED" else "NOT PASSED"}",
                style = MaterialTheme.typography.labelMedium,
                color = if (att.passed) SuccessEmerald else ErrorCrimson
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
