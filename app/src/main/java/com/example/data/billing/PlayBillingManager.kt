package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayBillingEvent {
  data class SubscriptionPurchased(
    val productId: String,
    val purchaseToken: String,
    val autoRenewing: Boolean
  ) : PlayBillingEvent

  data class CoursePurchased(
    val productId: String,
    val purchaseToken: String,
    val courseIdHint: String?
  ) : PlayBillingEvent

  data class Error(val message: String) : PlayBillingEvent
  data class Info(val message: String) : PlayBillingEvent
}

data class PlayBillingUiState(
  val isConnected: Boolean = false,
  val isConnecting: Boolean = false,
  val statusMessage: String = "Connecting to Google Play Billing...",
  val subscriptionProducts: List<ProductDetails> = emptyList(),
  val courseProducts: List<ProductDetails> = emptyList()
)

class PlayBillingManager(context: Context) : PurchasesUpdatedListener {

  companion object {
    private const val TAG = "PlayBillingManager"
    const val PRODUCT_PRO_MONTHLY = "datalabs_pro_monthly"
    const val PRODUCT_PRO_ANNUAL = "datalabs_pro_annual"
    const val PRODUCT_SINGLE_COURSE = "datalabs_course_unlock"
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(PlayBillingUiState())
  val state: StateFlow<PlayBillingUiState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<PlayBillingEvent>(extraBufferCapacity = 16)
  val events: SharedFlow<PlayBillingEvent> = _events.asSharedFlow()

  @Volatile
  private var pendingCourseIdHint: String? = null

  private val appContext = context.applicationContext

  private val billingClient: BillingClient? by lazy {
    runCatching {
      BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
          PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()
        )
        .build()
    }.onFailure { t ->
      Log.w(TAG, "Play Billing client creation failed", t)
    }.getOrNull()
  }

  fun ensureConnected() {
    val client = billingClient ?: run {
      _state.value = _state.value.copy(
        isConnected = false,
        isConnecting = false,
        statusMessage = "Google Play Billing unavailable on this device."
      )
      return
    }
    if (client.isReady || _state.value.isConnecting) return
    _state.value = _state.value.copy(
      isConnecting = true,
      statusMessage = "Connecting to Google Play Store..."
    )
    runCatching {
      client.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
          if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
              isConnected = true,
              isConnecting = false,
              statusMessage = "Google Play Billing ready"
            )
            scope.launch {
              queryCatalogProducts()
            }
          } else {
            val msg = "Google Play Billing unavailable (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})"
            Log.w(TAG, msg)
            _state.value = _state.value.copy(
              isConnected = false,
              isConnecting = false,
              statusMessage = msg
            )
          }
        }

        override fun onBillingServiceDisconnected() {
          _state.value = _state.value.copy(
            isConnected = false,
            isConnecting = false,
            statusMessage = "Google Play Billing disconnected"
          )
        }
      })
    }.onFailure { t ->
      Log.w(TAG, "startConnection failed", t)
      _state.value = _state.value.copy(
        isConnected = false,
        isConnecting = false,
        statusMessage = "Google Play Billing unavailable"
      )
    }
  }

  private suspend fun queryCatalogProducts() {
    val client = billingClient ?: return
    if (!client.isReady) return

    val subsParams = QueryProductDetailsParams.newBuilder()
      .setProductList(
        listOf(PRODUCT_PRO_MONTHLY, PRODUCT_PRO_ANNUAL).map { id ->
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(id)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        }
      )
      .build()

    val inAppParams = QueryProductDetailsParams.newBuilder()
      .setProductList(
        listOf(PRODUCT_SINGLE_COURSE).map { id ->
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(id)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        }
      )
      .build()

    val subsResult = client.queryProductDetails(subsParams)
    val inAppResult = client.queryProductDetails(inAppParams)

    val subsList = subsResult.productDetailsList.orEmpty()
    val inAppList = inAppResult.productDetailsList.orEmpty()
    val summary = if (subsList.isEmpty() && inAppList.isEmpty()) {
      "Google Play connected. Configure Play Console products ($PRODUCT_PRO_MONTHLY, $PRODUCT_PRO_ANNUAL, $PRODUCT_SINGLE_COURSE) for live store checkout, or use regional payment requests."
    } else {
      "Google Play Billing connected (${subsList.size} subscription plans, ${inAppList.size} course passes)."
    }

    _state.value = _state.value.copy(
      subscriptionProducts = subsList,
      courseProducts = inAppList,
      statusMessage = summary
    )
  }

  fun launchSubscriptionPurchase(activity: Activity, productId: String) {
    val client = billingClient
    if (client == null || !client.isReady) {
      ensureConnected()
      _events.tryEmit(PlayBillingEvent.Error("Google Play Billing is not ready yet. Please try again."))
      return
    }
    val details = _state.value.subscriptionProducts.firstOrNull { it.productId == productId }
    if (details == null) {
      _events.tryEmit(
        PlayBillingEvent.Error(
          "Play Store product '$productId' is not configured on this build yet. You can submit an instant gateway checkout request below."
        )
      )
      return
    }
    val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
    if (offerToken.isNullOrBlank()) {
      _events.tryEmit(PlayBillingEvent.Error("No active subscription offer found for $productId."))
      return
    }

    val params = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(
        listOf(
          BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        )
      )
      .build()
    val result = client.launchBillingFlow(activity, params)
    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
      _events.tryEmit(PlayBillingEvent.Error("Could not launch Play checkout: ${result.debugMessage}"))
    }
  }

  fun launchCoursePurchase(activity: Activity, courseId: String) {
    val client = billingClient
    if (client == null || !client.isReady) {
      ensureConnected()
      _events.tryEmit(PlayBillingEvent.Error("Google Play Billing is not ready yet."))
      return
    }
    val details = _state.value.courseProducts.firstOrNull { it.productId == PRODUCT_SINGLE_COURSE }
    if (details == null) {
      _events.tryEmit(
        PlayBillingEvent.Error(
          "Single-course Play product '$PRODUCT_SINGLE_COURSE' is not configured in Play Console yet. Submit a single-course payment request below or choose Pro."
        )
      )
      return
    }
    pendingCourseIdHint = courseId
    val params = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(
        listOf(
          BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        )
      )
      .build()
    val result = client.launchBillingFlow(activity, params)
    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
      pendingCourseIdHint = null
      _events.tryEmit(PlayBillingEvent.Error("Could not launch course checkout: ${result.debugMessage}"))
    }
  }

  fun restorePurchases() {
    val client = billingClient
    if (client == null || !client.isReady) {
      ensureConnected()
      _events.tryEmit(PlayBillingEvent.Error("Google Play Store connection is not active."))
      return
    }
    scope.launch {
      val subsPurchases = client.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.SUBS)
          .build()
      )
      val inAppPurchases = client.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.INAPP)
          .build()
      )

      val allPurchased = subsPurchases.purchasesList + inAppPurchases.purchasesList
      val activePurchased = allPurchased.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
      if (activePurchased.isEmpty()) {
        _events.tryEmit(PlayBillingEvent.Info("No prior Google Play purchases were found on this Google account."))
      } else {
        activePurchased.forEach { handleVerifiedPurchase(it) }
      }
    }
  }

  override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
    when (result.responseCode) {
      BillingClient.BillingResponseCode.OK -> {
        purchases.orEmpty().forEach { purchase ->
          scope.launch { handleVerifiedPurchase(purchase) }
        }
      }
      BillingClient.BillingResponseCode.USER_CANCELED -> {
        pendingCourseIdHint = null
        _events.tryEmit(PlayBillingEvent.Info("Purchase cancelled."))
      }
      else -> {
        pendingCourseIdHint = null
        _events.tryEmit(
          PlayBillingEvent.Error(
            "Google Play checkout did not complete (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})"
          )
        )
      }
    }
  }

  private suspend fun handleVerifiedPurchase(purchase: Purchase) {
    if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
      _events.tryEmit(PlayBillingEvent.Info("Purchase is pending payment confirmation by Google Play."))
      return
    }
    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

    val client = billingClient ?: return
    if (!purchase.isAcknowledged) {
      val ackResult = client.acknowledgePurchase(
        AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken)
          .build()
      )
      if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
        _events.tryEmit(PlayBillingEvent.Error("Purchase acknowledgement failed: ${ackResult.debugMessage}"))
        return
      }
    }

    val productId = purchase.products.firstOrNull() ?: return
    if (productId == PRODUCT_SINGLE_COURSE) {
      val hint = pendingCourseIdHint
      pendingCourseIdHint = null
      _events.tryEmit(
        PlayBillingEvent.CoursePurchased(
          productId = productId,
          purchaseToken = purchase.purchaseToken,
          courseIdHint = hint
        )
      )
    } else {
      _events.tryEmit(
        PlayBillingEvent.SubscriptionPurchased(
          productId = productId,
          purchaseToken = purchase.purchaseToken,
          autoRenewing = purchase.isAutoRenewing
        )
      )
    }
  }
}
