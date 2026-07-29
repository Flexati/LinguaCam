package com.linguacam.data.repository

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class BillingRepository(context: Context) : BillingRepositoryAPI {

    companion object {
        const val PRO_PLAN_SKU = "linguacam_pro"
        // Fix P1-8: PRO_PLAN_PRICE_EUR centralizzato in BillingConfig per evitare drift.
        // Qui riusiamo il valore esposto dal modulo contracts.
        val PRO_PLAN_PRICE_EUR: Double get() = BillingConfig.PRO_PLAN_PRICE_EUR
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RESPONSE_CODE_PRODUCT_NOT_READY = -1
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _flowResult = MutableSharedFlow<BillingFlowResult>(extraBufferCapacity = 8)
    override val flowResult: SharedFlow<BillingFlowResult> = _flowResult.asSharedFlow()

    private var reconnectAttempts = 0
    private var launchHandler: ((ProductDetails) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> handlePurchase(purchase) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                appScope.launch { _flowResult.emit(BillingFlowResult.UserCancelled) }
            }
            else -> {
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = result.responseCode,
                            message = result.debugMessage
                        )
                    )
                }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override fun setLaunchHandler(handler: ((ProductDetails) -> Unit)?) {
        this.launchHandler = handler
    }

    override fun startConnection() {
        if (billingClient.isReady) {
            queryProductDetails()
            appScope.launch { restoreInternal() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    queryProductDetails()
                    appScope.launch { restoreInternal() }
                } else {
                    Timber.w("Billing setup non OK: code=${result.responseCode} msg=${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++
                    Timber.w("Billing disconnected — retry $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")
                    startConnection()
                } else {
                    Timber.e("Billing service disconnected; rinuncia dopo $MAX_RECONNECT_ATTEMPTS tentativi")
                    _subscriptionState.update { it.copy(isReady = false) }
                }
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PLAN_SKU)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val pd = list.firstOrNull { it.productId == PRO_PLAN_SKU }
                _subscriptionState.update { it.copy(productDetails = pd, isReady = pd != null) }
            } else {
                Timber.w("queryProductDetails failed: ${result.debugMessage}")
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = result.responseCode,
                            message = result.debugMessage
                        )
                    )
                }
            }
        }
    }

    override fun requestPurchase() {
        _subscriptionState.value.productDetails?.let { pd ->
            launchHandler?.invoke(pd)
        } ?: run {
            Timber.w("requestPurchase chiamato ma productDetails=null")
            appScope.launch {
                _flowResult.emit(
                    BillingFlowResult.Error(
                        responseCode = RESPONSE_CODE_PRODUCT_NOT_READY,
                        message = "Prodotto non pronto. Riprova."
                    )
                )
            }
        }
    }

    override fun launchBillingFlow(activity: Activity, params: BillingFlowParams) {
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            appScope.launch { _flowResult.emit(BillingFlowResult.LaunchedFlow) }
        } else {
            appScope.launch {
                _flowResult.emit(
                    BillingFlowResult.Error(
                        responseCode = result.responseCode,
                        message = result.debugMessage
                    )
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return
        }
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    applyProState()
                    appScope.launch {
                        _flowResult.emit(
                            BillingFlowResult.Success(Purchase.PurchaseState.PURCHASED)
                        )
                    }
                } else {
                    Timber.e("Acknowledge fallito: code=${ackResult.responseCode}")
                }
            }
        } else {
            applyProState()
        }
    }

    private fun applyProState() {
        _subscriptionState.update {
            it.copy(
                plan = SubscriptionPlan.PRO,
                isPurchased = true,
                purchaseDate = System.currentTimeMillis(),
                unlimitedLanguages = true,
                hasTranslationHistory = true,
                hasConversationMode = true
            )
        }
    }

    override suspend fun restore(): Result<Unit> {
        return try {
            restoreInternal()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "restore error")
            Result.failure(e)
        }
    }

    private suspend fun restoreInternal() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        val paramResult = result.billingResult
        if (paramResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.purchasesList.forEach { handlePurchase(it) }
        } else {
            Timber.w("queryPurchases failed: ${paramResult.debugMessage}")
        }
    }

    // ============================================================
    // Legacy API mantenute per il codice pre-Step 1 (MaxInstalledLanguages etc.).
    // Non vengono rimosse in questo step: altri file potrebbero ancora chiamarle.
    // Verranno rimosse in un refactor successivo dopo che i call-site sono stati aggiornati.
    // ============================================================

    fun canInstallLanguageOnFreePlan(currentInstalledCount: Int): Boolean {
        return if (_subscriptionState.value.plan == SubscriptionPlan.PRO) {
            true
        } else {
            currentInstalledCount < 2
        }
    }

    fun getMaxInstalledLanguages(): Int {
        return if (_subscriptionState.value.plan == SubscriptionPlan.PRO) {
            Int.MAX_VALUE
        } else {
            2
        }
    }
}
