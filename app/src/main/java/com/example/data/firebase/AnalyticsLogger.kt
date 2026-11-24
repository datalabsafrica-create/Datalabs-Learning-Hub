package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import com.example.data.model.Certificate
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AnalyticsLogger(private val context: Context) {
  private val analytics: FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(context.applicationContext)
  }

  fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
    try {
      val bundle = Bundle().apply {
        params.forEach { (k, v) -> putString(k, v.take(100)) }
      }
      analytics.logEvent(name, bundle)
    } catch (_: Exception) {
      // Non-fatal analytics safeguard
    }
  }

  fun logCourseViewed(courseId: String, title: String, academyName: String) {
    logEvent(
      "course_viewed",
      mapOf("course_id" to courseId, "course_title" to title, "academy" to academyName)
    )
  }

  fun logCourseEnrolled(courseId: String, accessTier: String) {
    logEvent("course_enrolled", mapOf("course_id" to courseId, "access_tier" to accessTier))
  }

  fun logLessonCompleted(courseId: String, lessonId: String) {
    logEvent("lesson_completed", mapOf("course_id" to courseId, "lesson_id" to lessonId))
  }

  fun logCertificateIssued(courseId: String, certificateCode: String) {
    logEvent(
      "certificate_issued",
      mapOf("course_id" to courseId, "certificate_code" to certificateCode)
    )
  }
}

class MaterialAndCertificateStorage(private val context: Context) {
  private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

  suspend fun uploadCourseAsset(
    folder: String,
    fileName: String,
    sourceUri: Uri
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
      val ref = storage.reference.child("datalabs/$folder/${System.currentTimeMillis()}_$safeName")
      ref.putFile(sourceUri).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      Result.success(downloadUrl)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveDownloadableMaterialLocally(
    fileName: String,
    content: String
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      val dir = File(context.filesDir, "materials").apply { mkdirs() }
      val safeFile = File(dir, fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_"))
      safeFile.writeText(content)
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        safeFile
      )
      Result.success(uri)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun generateCertificatePdfUri(certificate: Certificate): Result<Uri> =
    withContext(Dispatchers.IO) {
      try {
        val dir = File(context.cacheDir, "certificates").apply { mkdirs() }
        val file = File(dir, "${certificate.certificateCode}.pdf")
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 landscape
        val page = pdf.startPage(pageInfo)
        drawCertificatePage(page.canvas, certificate)
        pdf.finishPage(page)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        val uri = FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          file
        )
        Result.success(uri)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun drawCertificatePage(canvas: Canvas, cert: Certificate) {
    val navy = 0xFF0B1B36.toInt()
    val blue = 0xFF2563EB.toInt()
    val gold = 0xFFD97706.toInt()
    val slate = 0xFF334155.toInt()
    val muted = 0xFF64748B.toInt()

    canvas.drawColor(0xFFFAFBFD.toInt())

    val borderPaint = Paint().apply {
      color = navy
      style = Paint.Style.STROKE
      strokeWidth = 6f
    }
    canvas.drawRect(24f, 24f, 818f, 571f, borderPaint)

    val innerBorderPaint = Paint().apply {
      color = blue
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
    }
    canvas.drawRect(36f, 36f, 806f, 559f, innerBorderPaint)

    val topRibbonPaint = Paint().apply { color = navy }
    canvas.drawRect(36f, 36f, 806f, 112f, topRibbonPaint)

    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFFFFFFFF.toInt()
      textSize = 20f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText("DATALABS LEARNING HUB", 64f, 74f, brandPaint)

    val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF93C5FD.toInt()
      textSize = 11f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    canvas.drawText("POWERED BY DATALABS ANALYTICS  •  SKILLS FOR WORK, BUSINESS & LIFE", 64f, 94f, subBrandPaint)

    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = gold
      textSize = 14f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText("CERTIFICATE OF ACHIEVEMENT", 421f, 165f, headingPaint)

    val bodyLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = muted
      textSize = 14f
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("This is proudly presented to", 421f, 205f, bodyLabelPaint)

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = navy
      textSize = 34f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    canvas.drawText(cert.studentName, 421f, 255f, namePaint)

    canvas.drawText("for successfully completing the practical professional course", 421f, 295f, bodyLabelPaint)

    val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = blue
      textSize = 24f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(cert.courseTitle, 421f, 340f, coursePaint)

    val academyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slate
      textSize = 15f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
    }
    canvas.drawText(cert.academyName, 421f, 372f, academyPaint)

    // Verification footer & deterministic matrix QR block
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slate
      textSize = 12f
      textAlign = Paint.Align.LEFT
    }
    canvas.drawText("Issued Date: ${cert.issuedDateText}", 64f, 470f, metaPaint)
    canvas.drawText("Certificate ID: ${cert.certificateCode}", 64f, 492f, metaPaint)
    canvas.drawText("Verify at: ${cert.verificationUrl}", 64f, 514f, metaPaint)
    canvas.drawText("Status: ${cert.status}", 64f, 536f, metaPaint)

    // Deterministic verification matrix
    val qrOriginX = 684f
    val qrOriginY = 440f
    val cell = 6f
    val darkPaint = Paint().apply { color = navy }
    val hash = cert.certificateCode.hashCode()
    for (r in 0 until 14) {
      for (c in 0 until 14) {
        val isFinder = (r < 4 && c < 4) || (r < 4 && c >= 10) || (r >= 10 && c < 4)
        val bit = ((hash ushr ((r * 14 + c) % 31)) and 1) == 1 || ((r + c) % 3 == 0 && (r * c + hash) % 2 == 0)
        if (isFinder || bit) {
          canvas.drawRect(
            qrOriginX + c * cell,
            qrOriginY + r * cell,
            qrOriginX + (c + 1) * cell,
            qrOriginY + (r + 1) * cell,
            darkPaint
          )
        }
      }
    }
  }

  fun shareUri(uri: Uri, mimeType: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
      Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }
}
