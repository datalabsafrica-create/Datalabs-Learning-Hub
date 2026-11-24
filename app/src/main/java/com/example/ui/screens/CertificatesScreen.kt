package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Certificate
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.StudentDataSnapshot

@Composable
fun CertificatesScreen(
  studentState: UiState<StudentDataSnapshot>,
  verifiedResult: Result<Certificate?>?,
  onVerifyCertificate: (String) -> Unit,
  onExportPdf: (Certificate) -> Unit,
  onOpenExplore: () -> Unit
) {
  if (studentState is UiState.Loading) {
    LoadingStateView("Loading certificates...")
    return
  }
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  var verifyQuery by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("certificates_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Certificates & Verification", style = MaterialTheme.typography.headlineLarge)
        Text(
          text = "Verified credentials earned by completing authorized Datalabs courses.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Verification Box
    item {
      Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = ElectricBlue)
            Text("Verify a Certificate", style = MaterialTheme.typography.titleMedium)
          }
          OutlinedTextField(
            value = verifyQuery,
            onValueChange = { verifyQuery = it },
            label = { Text("Enter Certificate ID or Code (e.g., DLH-2026-...)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("verify_certificate_input")
          )
          Button(
            onClick = { onVerifyCertificate(verifyQuery) },
            modifier = Modifier.testTag("verify_certificate_button")
          ) {
            Text("Verify Authenticity")
          }

          if (verifiedResult != null) {
            val cert = verifiedResult.getOrNull()
            if (cert != null) {
              Surface(
                color = SuccessEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text("Valid Certificate (${cert.status})", color = SuccessEmerald, fontWeight = FontWeight.Bold)
                  Text("Student: ${cert.studentName}", style = MaterialTheme.typography.bodySmall)
                  Text("Course: ${cert.courseTitle}", style = MaterialTheme.typography.bodySmall)
                  Text("Issued: ${cert.issuedDateText} • ${cert.certificateCode}", style = MaterialTheme.typography.labelSmall)
                }
              }
            } else {
              Surface(
                color = ErrorCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "No matching verified certificate was found for that ID/code.",
                  color = ErrorCrimson,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "My Earned Certificates (${student.certificates.size})",
        style = MaterialTheme.typography.titleLarge
      )
    }

    if (student.certificates.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.WorkspacePremium,
          title = "No certificates earned yet",
          description = "Complete 100% of lessons in any certificate-eligible course to earn and export your official PDF credential.",
          actionLabel = "Explore Courses",
          onAction = onOpenExplore
        )
      }
    } else {
      items(student.certificates, key = { it.id }) { cert ->
        CertificateCard(certificate = cert, onExportPdf = { onExportPdf(cert) })
      }
    }
  }
}

@Composable
private fun CertificateCard(
  certificate: Certificate,
  onExportPdf: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = NavyDeep),
    border = BorderStroke(1.5.dp, WarningGold.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AsyncImage(
            model = R.drawable.img_certificate_crest,
            contentDescription = "Datalabs Academic Seal",
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
          )
          Column {
            val isSpecialization = certificate.courseId == com.example.data.seed.AcademyCatalogDefinitions.AI_SPECIALIZATION_COURSE_ID
            Text(
              text = "DATALABS LEARNING HUB",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD)
            )
            Text(
              text = if (isSpecialization) "ACADEMY SPECIALIZATION CREDENTIAL" else "CERTIFICATE OF COMPLETION",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFFFCD34D),
              fontWeight = FontWeight.Bold
            )
          }
        }
        VerificationQrMatrix(code = certificate.certificateCode, modifier = Modifier.size(48.dp))
      }

      Text(
        text = certificate.studentName,
        style = MaterialTheme.typography.displayMedium,
        color = Color.White
      )
      Text(
        text = certificate.courseTitle,
        style = MaterialTheme.typography.titleLarge,
        color = Color(0xFF93C5FD)
      )
      Text(
        text = "${certificate.academyName} • Issued ${certificate.issuedDateText}",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFCBD5E1)
      )
      Text(
        text = "ID: ${certificate.certificateCode} • ${certificate.verificationUrl}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF94A3B8)
      )

      Button(
        onClick = onExportPdf,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("export_certificate_pdf_${certificate.id}")
      ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download & Share PDF Certificate")
      }
    }
  }
}

@Composable
private fun VerificationQrMatrix(code: String, modifier: Modifier = Modifier) {
  Surface(
    color = Color.White,
    shape = RoundedCornerShape(6.dp),
    modifier = modifier
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
      val grid = 9
      val cellW = size.width / grid
      val cellH = size.height / grid
      val hash = code.hashCode()
      for (r in 0 until grid) {
        for (c in 0 until grid) {
          val finder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
          val bit = ((hash ushr ((r * grid + c) % 31)) and 1) == 1
          if (finder || bit) {
            drawRect(
              color = NavyDeep,
              topLeft = Offset(c * cellW, r * cellH),
              size = Size(cellW, cellH)
            )
          }
        }
      }
    }
  }
}
