package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class AiHelpMode(val label: String, val instruction: String) {
  ASK_QUESTION(
    label = "Ask About Lesson",
    instruction = "Answer the student's question clearly using the current lesson concepts and practical workplace/business framing."
  ),
  SIMPLIFY(
    label = "Simpler Explanation",
    instruction = "Re-explain the core concept in plain, beginner-friendly language using an everyday analogy and concrete 3-step summary."
  ),
  EXTRA_EXAMPLE(
    label = "More Examples",
    instruction = "Provide 2 practical real-world worked examples relevant to work, entrepreneurship, or Africa/global remote teams."
  ),
  SUMMARIZE(
    label = "Key Summary",
    instruction = "Summarize the lesson into the 5 essential concepts to remember and 2 immediate actions to apply today."
  ),
  EXERCISE_HINT(
    label = "Guide Exercise",
    instruction = "Coach the student through the practical exercise step by step WITHOUT giving away full graded answers or doing the entire assignment for them. Ask 1 guiding question."
  ),
  PRACTICE_QUESTIONS(
    label = "Extra Practice",
    instruction = "Create 2 original self-check practice questions (1 multiple choice, 1 scenario prompt) with brief hints hidden below a 'Check Understanding' section."
  )
}

data class AiStudyRequest(
  val courseTitle: String,
  val academyName: String,
  val level: String,
  val lessonTitle: String,
  val lessonSummary: String,
  val lessonNotesExcerpt: String,
  val exercisePrompt: String?,
  val mode: AiHelpMode,
  val userPrompt: String
)

class AiLearningAssistantService {

  private companion object {
    const val TAG = "AiLearningAssistant"
    const val MODEL_NAME = "gemini-3.8-flash"
    const val TIMEOUT_MS = 60_000L
  }

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig = generationConfig {
        temperature = 0.35f
        maxOutputTokens = 1024
      },
      systemInstruction = content {
        text(
          """
          You are the Datalabs Learning Hub AI Study Assistant.
          Your purpose is to help adult learners master practical career, business, data analytics, AI, and digital skills.
          
          Strict Rules:
          1. Base explanations primarily on the provided Datalabs Course & Lesson Context. When adding extra general context, state clearly what goes beyond the lesson text.
          2. Never invent fake statistics, URLs, external citations, certificates, or guaranteed financial returns.
          3. When coaching on exercises or quizzes (Guide Exercise mode), give structured hints, reasoning frameworks, and guiding questions—NEVER hand over complete direct answers to graded tasks.
          4. Keep formatting concise, scannable, and practical (short paragraphs, bullet points, clear headings).
          5. If the topic relates to finance or investing, frame outputs strictly as objective educational explanation rather than financial advice.
          """.trimIndent()
        )
      }
    )
  }

  fun streamStudyAssistance(request: AiStudyRequest): Flow<String> = flow {
    val promptText = buildString {
      appendLine("COURSE CONTEXT:")
      appendLine("- Course: ${request.courseTitle} (${request.level})")
      appendLine("- Academy: ${request.academyName}")
      appendLine("- Current Lesson: ${request.lessonTitle}")
      appendLine("- Summary: ${request.lessonSummary}")
      if (!request.exercisePrompt.isNullOrBlank()) {
        appendLine("- Practical Exercise: ${request.exercisePrompt}")
      }
      appendLine()
      appendLine("VERIFIED LESSON NOTES EXCERPT:")
      appendLine(request.lessonNotesExcerpt.take(2800))
      appendLine()
      appendLine("TUTORING MODE: ${request.mode.label}")
      appendLine("MODE GUIDANCE: ${request.mode.instruction}")
      if (request.userPrompt.isNotBlank()) {
        appendLine()
        appendLine("STUDENT REQUEST:")
        appendLine(request.userPrompt.trim().take(800))
      }
    }

    val accumulated = StringBuilder()
    try {
      withTimeout(TIMEOUT_MS) {
        model.generateContentStream(promptText).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          if (chunkText.isNotEmpty()) {
            accumulated.append(chunkText)
            emit(accumulated.toString())
          }
        }
      }
      if (accumulated.isBlank()) {
        throw IllegalStateException("AI Study Assistant returned an empty response. Please try again.")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI study assistance timed out", e)
      throw IllegalStateException("AI response timed out after 60 seconds. Check connection and try again.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI generation failed", e)
      throw e
    }
  }
}
