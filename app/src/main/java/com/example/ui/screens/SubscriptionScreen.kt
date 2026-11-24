package com.example.ui.screens

import com.example.data.repository.findActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.HubCatalogSnapshot
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StudentDataSnapshot
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
  viewModel: MainViewModel,
  catalogState: UiState<HubCatalogSnapshot>,
  studentState: UiState<StudentDataSnapshot>,
  onBack: () -> Unit
) {
  val activity = LocalContext.current.findActivity()
  val billingState by viewModel.billingState.collectAsStateWithLifecycle()
  val catalog = (catalogState as? UiState.Success)?.data ?: HubCatalogSnapshot()
  val student = (studentState as? UiState.Success)?.data ?: StudentDataSnapshot()
  val activeSub = student.subscription
  val activeSingleCourses = remember(student.coursePurchases) {
    student.coursePurchases.filter { it.activeUnlock }
  }
  val hasSubscription = activeSub?.activeEntitlement == true
  val unlockedCourseIds = remember(activeSingleCourses) {
    activeSingleCourses.map { it.courseId }.toSet()
  }
  val stageMastery = remember(catalog.courses, student.enrollments, student.certificates) {
    com.example.data.seed.AcademyProgressionHelper.computeMastery(
      courses = catalog.courses,
      enrollments = student.enrollments,
      certificates = student.certificates
    )
  }

  LaunchedEffect(Unit) {
    viewModel.ensureBillingConnected()
  }

  var currency by remember { mutableStateOf("UGX") }
  var selectedProvider by remember(currency) {
    mutableStateOf(if (currency == "UGX") "MTN_MOMO" else "STRIPE")
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Datalabs Pro Membership") },
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
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Surface(
          color = NavyDeep,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "UNLOCK FULL AI ACADEMY ACCESS",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD)
            )
            Text(
              text = "Invest in Practical AI Skills",
              style = MaterialTheme.typography.displayMedium,
              color = Color.White
            )
            Text(
              text = "Full access to all 5 AI Academy courses, practice kits, AI Study Assistant, guided learning paths, and verified certificates.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFCBD5E1)
            )
            if (activeSub != null) {
              Surface(
                color = if (activeSub.activeEntitlement) SuccessEmerald.copy(alpha = 0.25f) else WarningGold.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
              ) {
                val expiryPart = activeSub.expiresAtText?.takeIf { it.isNotBlank() }?.let { " • Valid until $it" } ?: ""
                Text(
                  text = "Current Status: ${activeSub.status} (${activeSub.planName})$expiryPart",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }

      // Google Play Billing Status + Restore Purchases
      item {
        Card(
          border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = ElectricBlue)
              Text("Google Play Store Billing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
              text = billingState.statusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
              onClick = { viewModel.restorePlayPurchases() },
              modifier = Modifier.fillMaxWidth().testTag("restore_purchases_button")
            ) {
              Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Restore Google Play Purchases")
            }
          }
        }
      }

      // AI Academy Stage Access Breakdown
      if (stageMastery.nodes.isNotEmpty()) {
        item {
          Card(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("AI Academy Stage Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              stageMastery.nodes.forEach { node ->
                val unlocked = !node.course.premiumOnly || hasSubscription || unlockedCourseIds.contains(node.course.id)
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
                    Icon(
                      imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                      contentDescription = null,
                      tint = if (unlocked) SuccessEmerald else WarningGold,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = "Stage ${node.stageNumber}: ${node.course.title}",
                      style = MaterialTheme.typography.bodySmall
                    )
                  }
                  Text(
                    text = when {
                      !node.course.premiumOnly -> "Free"
                      unlocked -> "Unlocked"
                      else -> "Pro"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) SuccessEmerald else WarningGold,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      // Single-Course Unlocks Summary (if student owns individual premium courses)
      if (activeSingleCourses.isNotEmpty()) {
        item {
          Card(
            border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = SuccessEmerald.copy(alpha = 0.06f))
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Unlocked Individual Courses (${activeSingleCourses.size})", style = MaterialTheme.typography.titleSmall, color = SuccessEmerald)
              activeSingleCourses.forEach { purchase ->
                Text("• ${purchase.courseTitle} (${purchase.paymentProvider})", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      // Payment Gateway Configuration Transparency Banner
      item {
        Card(
          border = BorderStroke(1.dp, WarningGold.copy(alpha = 0.5f)),
          colors = CardDefaults.cardColors(containerColor = WarningGold.copy(alpha = 0.08f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = WarningGold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Regional & Card Checkout Requests", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "Live card/mobile-money webhooks (Stripe, MTN MoMo, Airtel Money) require backend webhook verification. Submitting a regional checkout request records a pending invoice in Firestore for verified activation, or use Google Play checkout directly on store builds.",
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }

      // Currency & Provider Selectors
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Currency & Payment Method", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = currency == "UGX",
              onClick = { currency = "UGX" },
              label = { Text("UGX (Uganda)") }
            )
            FilterChip(
              selected = currency == "USD",
              onClick = { currency = "USD" },
              label = { Text("USD (International)") }
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currency == "UGX") {
              FilterChip(
                selected = selectedProvider == "MTN_MOMO",
                onClick = { selectedProvider = "MTN_MOMO" },
                label = { Text("MTN MoMo") }
              )
              FilterChip(
                selected = selectedProvider == "AIRTEL_MONEY",
                onClick = { selectedProvider = "AIRTEL_MONEY" },
                label = { Text("Airtel Money") }
              )
            }
            FilterChip(
              selected = selectedProvider == "STRIPE",
              onClick = { selectedProvider = "STRIPE" },
              label = { Text("Card / Stripe") }
            )
          }
        }
      }

      items(catalog.plans.filter { it.active }, key = { it.id }) { plan ->
        PlanCard(
          plan = plan,
          currency = currency,
          provider = selectedProvider,
          onSubscribePlay = {
            if (activity != null) viewModel.launchPlaySubscriptionPurchase(activity, plan)
          },
          onRequestGateway = { viewModel.requestSubscriptionCheckout(plan, currency, selectedProvider) }
        )
      }
    }
  }
}

@Composable
private fun PlanCard(
  plan: SubscriptionPlan,
  currency: String,
  provider: String,
  onSubscribePlay: () -> Unit,
  onRequestGateway: () -> Unit
) {
  val priceText = if (currency == "UGX") {
    "UGX ${NumberFormat.getIntegerInstance(Locale.US).format(plan.priceUgx)}"
  } else {
    "$${plan.priceUsd.toInt()} USD"
  }
  Card(
    border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.45f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(plan.name, style = MaterialTheme.typography.titleLarge)
        Text(priceText, style = MaterialTheme.typography.headlineSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
      }
      Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      plan.features.forEach { feat ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessEmerald)
          Text(feat, style = MaterialTheme.typography.bodySmall)
        }
      }
      Button(
        onClick = onSubscribePlay,
        modifier = Modifier.fillMaxWidth().testTag("play_sub_${plan.id}")
      ) {
        Text("Subscribe via Google Play")
      }
      OutlinedButton(
        onClick = onRequestGateway,
        modifier = Modifier.fillMaxWidth().testTag("gateway_sub_${plan.id}")
      ) {
        Text("Request ${plan.billingCycle.lowercase()} plan via $provider")
      }
    }
  }
}
