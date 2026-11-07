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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BillingConnectionState {
    object Disconnected : BillingConnectionState()
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    data class Error(val message: String, val responseCode: Int) : BillingConnectionState()
}

sealed class BillingPurchaseResult {
    data class Success(val purchase: Purchase, val acknowledged: Boolean) : BillingPurchaseResult()
    data class Pending(val purchase: Purchase) : BillingPurchaseResult()
    data class UserCancelled(val message: String = "Purchase cancelled by user") : BillingPurchaseResult()
    data class Failure(val responseCode: Int, val debugMessage: String) : BillingPurchaseResult()
}

/**
 * Production-ready Google Play Billing Manager for AJIYA.
 * Handles Play Billing 7.x lifecycle, dynamic product querying, purchase flows,
 * purchase acknowledgment, and restoration.
 */
class PlayStoreBillingManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PlayStoreBillingManager"

        // Play Console Subscription Product IDs
        const val SUB_MONTHLY = "ajiya_pro_monthly"
        const val SUB_YEARLY = "ajiya_shield_yearly"
        const val SUB_FAMILY = "ajiya_family_monthly"

        // Play Console In-App One-Time Product IDs (Optional services & donations)
        const val INAPP_ESCORT = "ajiya_escort_service"
        const val INAPP_TOW = "ajiya_tow_service"
        const val INAPP_PARAMEDIC = "ajiya_paramedic_service"
        const val INAPP_DONATION_1K = "ajiya_donation_1000"
        const val INAPP_DONATION_5K = "ajiya_donation_5000"
        const val INAPP_DONATION_10K = "ajiya_donation_10000"
    }

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<BillingPurchaseResult>(replay = 1)
    val purchaseEvents: SharedFlow<BillingPurchaseResult> = _purchaseEvents.asSharedFlow()

    private val _activePurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val activePurchases: StateFlow<List<Purchase>> = _activePurchases.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection(onReady: (() -> Unit)? = null) {
        if (_connectionState.value == BillingConnectionState.Connected) {
            onReady?.invoke()
            return
        }

        _connectionState.value = BillingConnectionState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Google Play Billing client connected successfully")
                    _connectionState.value = BillingConnectionState.Connected
                    queryAllProducts()
                    queryActivePurchases()
                    onReady?.invoke()
                } else {
                    val errMsg = "Billing setup failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})"
                    Log.e(TAG, errMsg)
                    _connectionState.value = BillingConnectionState.Error(billingResult.debugMessage, billingResult.responseCode)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Google Play Billing service disconnected")
                _connectionState.value = BillingConnectionState.Disconnected
            }
        })
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        _connectionState.value = BillingConnectionState.Disconnected
    }

    private fun queryAllProducts() {
        if (!billingClient.isReady) return

        // 1. Query Subscriptions
        val subProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_FAMILY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProductList)
            .build()

        billingClient.queryProductDetailsAsync(subParams) { billingResult, queryProductDetailsList ->
            val resultMap = HashMap<String, ProductDetails>(_products.value)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryProductDetailsList.forEach { details ->
                    resultMap[details.productId] = details
                    Log.d(TAG, "Found sub product: ${details.productId} -> ${details.title}")
                }
                _products.value = resultMap
            } else {
                Log.w(TAG, "Failed to query subscriptions: ${billingResult.debugMessage}")
            }
        }

        // 2. Query In-App One-Time products
        val inAppProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_ESCORT)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_TOW)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_PARAMEDIC)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_DONATION_1K)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_DONATION_5K)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_DONATION_10K)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProductList)
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, queryProductDetailsList ->
            val resultMap = HashMap<String, ProductDetails>(_products.value)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryProductDetailsList.forEach { details ->
                    resultMap[details.productId] = details
                    Log.d(TAG, "Found in-app product: ${details.productId} -> ${details.title}")
                }
                _products.value = resultMap
            } else {
                Log.w(TAG, "Failed to query in-app products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        selectedOfferToken: String? = null
    ): Boolean {
        if (!billingClient.isReady) {
            Log.e(TAG, "Cannot launch billing flow: BillingClient is not ready")
            startConnection {
                launchBillingFlow(activity, productId, selectedOfferToken)
            }
            return false
        }

        val productDetails = _products.value[productId]
        if (productDetails == null) {
            Log.e(TAG, "ProductDetails for $productId not found in loaded products")
            return false
        }

        val productDetailsParamsList = when (productDetails.productType) {
            BillingClient.ProductType.SUBS -> {
                val offerToken = selectedOfferToken
                    ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken.isNullOrEmpty()) {
                    Log.e(TAG, "No valid subscription offer token found for $productId")
                    return false
                }
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            }
            else -> {
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            }
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase flow")
                scope.launch {
                    _purchaseEvents.emit(BillingPurchaseResult.UserCancelled())
                }
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})")
                scope.launch {
                    _purchaseEvents.emit(
                        BillingPurchaseResult.Failure(
                            billingResult.responseCode,
                            billingResult.debugMessage
                        )
                    )
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                scope.launch {
                    if (!purchase.isAcknowledged) {
                        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                            val success = result.responseCode == BillingClient.BillingResponseCode.OK
                            Log.i(TAG, "Purchase acknowledged: $success for order ${purchase.orderId}")
                            scope.launch {
                                _purchaseEvents.emit(BillingPurchaseResult.Success(purchase, acknowledged = success))
                                queryActivePurchases()
                            }
                        }
                    } else {
                        _purchaseEvents.emit(BillingPurchaseResult.Success(purchase, acknowledged = true))
                        queryActivePurchases()
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                Log.i(TAG, "Purchase pending: order ${purchase.orderId}")
                scope.launch {
                    _purchaseEvents.emit(BillingPurchaseResult.Pending(purchase))
                }
            }
            else -> {
                Log.w(TAG, "Unspecified purchase state for order: ${purchase.orderId}")
            }
        }
    }

    fun queryActivePurchases(onComplete: ((List<Purchase>) -> Unit)? = null) {
        if (!billingClient.isReady) return

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _activePurchases.value = purchasesList
                onComplete?.invoke(purchasesList)
            } else {
                Log.w(TAG, "Failed querying active subscriptions: ${billingResult.debugMessage}")
                onComplete?.invoke(emptyList())
            }
        }
    }

    fun restorePurchases(onRestored: (Boolean, Int) -> Unit) {
        if (!billingClient.isReady) {
            startConnection {
                restorePurchases(onRestored)
            }
            return
        }

        queryActivePurchases { purchases ->
            val validPurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            scope.launch {
                for (p in validPurchases) {
                    if (!p.isAcknowledged) {
                        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgeParams) { res ->
                            Log.i(TAG, "Restored purchase acknowledged: ${res.responseCode == BillingClient.BillingResponseCode.OK}")
                        }
                    }
                    _purchaseEvents.emit(BillingPurchaseResult.Success(p, acknowledged = true))
                }
                withContext(Dispatchers.Main) {
                    onRestored(validPurchases.isNotEmpty(), validPurchases.size)
                }
            }
        }
    }
}
